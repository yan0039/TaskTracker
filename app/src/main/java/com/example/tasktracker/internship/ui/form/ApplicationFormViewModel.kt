package com.example.tasktracker.internship.ui.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tasktracker.internship.data.JobApplicationRepository
import com.example.tasktracker.internship.model.ApplicationStatus
import com.example.tasktracker.internship.model.JobApplication
import com.example.tasktracker.internship.model.JobApplicationInput
import com.example.tasktracker.internship.util.ApplicationField
import com.example.tasktracker.internship.util.InvalidApplicationException
import com.example.tasktracker.internship.util.validateJobApplicationInput
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ApplicationFormValues(
    val companyName: String = "",
    val jobTitle: String = "",
    val city: String = "",
    val jobUrl: String = "",
    val appliedDate: LocalDate? = null,
    val status: ApplicationStatus = ApplicationStatus.PENDING,
    val nextFollowUpDate: LocalDate? = null,
    val note: String = "",
) {
    fun toInput(): JobApplicationInput = JobApplicationInput(
        companyName = companyName,
        jobTitle = jobTitle,
        city = city,
        jobUrl = jobUrl.ifBlank { null },
        appliedDate = appliedDate,
        status = status,
        nextFollowUpDate = nextFollowUpDate,
        note = note,
    )

    companion object {
        fun from(record: JobApplication) = ApplicationFormValues(
            companyName = record.companyName,
            jobTitle = record.jobTitle,
            city = record.city,
            jobUrl = record.jobUrl.orEmpty(),
            appliedDate = record.appliedDate,
            status = record.status,
            nextFollowUpDate = record.nextFollowUpDate,
            note = record.note,
        )
    }
}

data class ApplicationFormUiState(
    val recordId: Long? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val loadFailed: Boolean = false,
    val saveSucceeded: Boolean = false,
    val values: ApplicationFormValues = ApplicationFormValues(),
    val initialValues: ApplicationFormValues = ApplicationFormValues(),
    val fieldErrors: Map<ApplicationField, String> = emptyMap(),
    val message: String? = null,
) {
    val hasUnsavedChanges: Boolean get() = !saveSucceeded && values != initialValues
}

@HiltViewModel
class ApplicationFormViewModel @Inject constructor(
    private val repository: JobApplicationRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val recordId = savedStateHandle.get<Long>("recordId")?.takeIf { it > 0 }
    private val _uiState = MutableStateFlow(
        ApplicationFormUiState(recordId = recordId, isLoading = recordId != null),
    )
    val uiState: StateFlow<ApplicationFormUiState> = _uiState.asStateFlow()

    init {
        if (recordId != null) {
            viewModelScope.launch {
                try {
                    val record = repository.getById(recordId)
                    if (record == null) {
                        _uiState.update {
                            it.copy(isLoading = false, loadFailed = true, message = "记录已不存在")
                        }
                    } else {
                        val values = ApplicationFormValues.from(record)
                        _uiState.update {
                            it.copy(isLoading = false, values = values, initialValues = values)
                        }
                    }
                } catch (_: Exception) {
                    _uiState.update {
                        it.copy(isLoading = false, loadFailed = true, message = "读取记录失败，请返回重试")
                    }
                }
            }
        }
    }

    private inline fun edit(field: ApplicationField? = null, change: (ApplicationFormValues) -> ApplicationFormValues) {
        _uiState.update { current ->
            if (current.isSaving || current.loadFailed || current.saveSucceeded) current
            else current.copy(
                values = change(current.values),
                fieldErrors = if (field == null) current.fieldErrors else current.fieldErrors - field,
                message = null,
            )
        }
    }

    fun setCompanyName(value: String) = edit(ApplicationField.COMPANY_NAME) { it.copy(companyName = value) }
    fun setJobTitle(value: String) = edit(ApplicationField.JOB_TITLE) { it.copy(jobTitle = value) }
    fun setCity(value: String) = edit(ApplicationField.CITY) { it.copy(city = value) }
    fun setJobUrl(value: String) = edit(ApplicationField.JOB_URL) { it.copy(jobUrl = value) }
    fun setAppliedDate(value: LocalDate?) = edit { it.copy(appliedDate = value) }
    fun setStatus(value: ApplicationStatus) = edit { it.copy(status = value) }
    fun setNextFollowUpDate(value: LocalDate?) = edit { it.copy(nextFollowUpDate = value) }
    fun setNote(value: String) = edit { it.copy(note = value) }

    fun save() {
        val current = _uiState.value
        if (current.isLoading || current.isSaving || current.loadFailed || current.saveSucceeded) return
        val input = current.values.toInput()
        val errors = validateJobApplicationInput(input)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = errors, message = "请检查标出的字段") }
            return
        }

        _uiState.update { it.copy(isSaving = true, fieldErrors = emptyMap(), message = null) }
        viewModelScope.launch {
            try {
                if (recordId == null) {
                    repository.create(input)
                } else if (!repository.update(recordId, input)) {
                    throw IllegalStateException("该记录已被删除，无法保存")
                }
                _uiState.update {
                    it.copy(isSaving = false, saveSucceeded = true, initialValues = it.values)
                }
            } catch (invalid: InvalidApplicationException) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        fieldErrors = invalid.fieldErrors,
                        message = "请检查标出的字段",
                    )
                }
            } catch (failure: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = if (failure is IllegalStateException &&
                            failure.message == "该记录已被删除，无法保存") {
                            failure.message
                        } else {
                            "保存失败，请重试；输入内容已保留"
                        },
                    )
                }
            }
        }
    }
}
