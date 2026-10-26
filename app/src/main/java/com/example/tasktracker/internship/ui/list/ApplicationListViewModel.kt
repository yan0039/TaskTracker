package com.example.tasktracker.internship.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tasktracker.internship.data.JobApplicationRepository
import com.example.tasktracker.internship.model.ApplicationStatus
import com.example.tasktracker.internship.model.JobApplication
import com.example.tasktracker.internship.util.ApplicationFilter
import com.example.tasktracker.internship.util.LocalDateProvider
import com.example.tasktracker.internship.util.dueFollowUps
import com.example.tasktracker.internship.util.filterApplications
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PendingDeletion(val record: JobApplication, val token: Long)

data class ApplicationListUiState(
    val isLoading: Boolean = true,
    val allRecords: List<JobApplication> = emptyList(),
    val visibleRecords: List<JobApplication> = emptyList(),
    val dueRecords: List<JobApplication> = emptyList(),
    val availableCities: List<String> = emptyList(),
    val filter: ApplicationFilter = ApplicationFilter(),
    val today: LocalDate = LocalDate.MIN,
    val pendingDeletion: PendingDeletion? = null,
) {
    val hasActiveFilter: Boolean get() = !filter.city.isNullOrBlank() || filter.status != null
}

@HiltViewModel
class ApplicationListViewModel @Inject constructor(
    private val repository: JobApplicationRepository,
    private val dateProvider: LocalDateProvider,
) : ViewModel() {
    private val filter = MutableStateFlow(ApplicationFilter())
    private val today = MutableStateFlow(dateProvider.today())
    private val pendingDeletion = MutableStateFlow<PendingDeletion?>(null)
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val messages = _messages.asSharedFlow()

    private var deletionJob: Job? = null
    private var deletionToken = 0L
    private var deletionInProgress = false
    private var restoring = false

    val uiState: StateFlow<ApplicationListUiState> = combine(
        repository.observeAll(), filter, today, pendingDeletion,
    ) { records, selectedFilter, currentDay, pending ->
        val visible = filterApplications(records, selectedFilter)
        ApplicationListUiState(
            isLoading = false,
            allRecords = records,
            visibleRecords = visible,
            dueRecords = dueFollowUps(visible, currentDay),
            availableCities = records.map { it.city.trim() }
                .filter { it.isNotEmpty() }
                .distinctBy { it.lowercase() }
                .sortedWith(String.CASE_INSENSITIVE_ORDER),
            filter = selectedFilter,
            today = currentDay,
            pendingDeletion = pending,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ApplicationListUiState())

    fun selectCity(city: String?) {
        filter.value = filter.value.copy(city = city)
    }

    fun selectStatus(status: ApplicationStatus?) {
        filter.value = filter.value.copy(status = status)
    }

    fun clearFilters() {
        filter.value = ApplicationFilter()
    }

    /** Called when the Activity enters the foreground, including after a midnight rollover. */
    fun refreshToday() {
        today.value = dateProvider.today()
    }

    fun delete(id: Long) {
        if (deletionInProgress || pendingDeletion.value != null) return
        deletionInProgress = true
        viewModelScope.launch {
            try {
                val deleted = repository.delete(id)
                if (deleted == null) {
                    _messages.emit("记录已不存在，无法删除")
                } else {
                    val token = ++deletionToken
                    pendingDeletion.value = PendingDeletion(deleted, token)
                    deletionJob?.cancel()
                    deletionJob = launch {
                        delay(5_000)
                        if (pendingDeletion.value?.token == token) pendingDeletion.value = null
                    }
                }
            } catch (_: Exception) {
                _messages.emit("删除失败，请重试")
            } finally {
                deletionInProgress = false
            }
        }
    }

    /** Reinserts the complete snapshot, including its original ID and timestamps. */
    fun undoDelete() {
        val pending = pendingDeletion.value ?: return
        if (restoring) return
        restoring = true
        deletionJob?.cancel()
        viewModelScope.launch {
            try {
                repository.restore(pending.record)
                if (pendingDeletion.value?.token == pending.token) pendingDeletion.value = null
            } catch (_: Exception) {
                _messages.emit("撤销失败，请重试")
                if (pendingDeletion.value?.token == pending.token) {
                    val retry = PendingDeletion(pending.record, ++deletionToken)
                    pendingDeletion.value = retry
                    deletionJob = launch {
                        delay(5_000)
                        if (pendingDeletion.value?.token == retry.token) pendingDeletion.value = null
                    }
                }
            } finally {
                restoring = false
            }
        }
    }
}
