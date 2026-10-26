package com.example.tasktracker.internship.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.tasktracker.internship.model.ApplicationStatus
import com.example.tasktracker.internship.model.JobApplication
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationListScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: ApplicationListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshToday()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message -> snackbarHostState.showSnackbar(message) }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("投递簿") }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                modifier = Modifier.testTag("application_add"),
            ) { Text("新增") }
        },
        snackbarHost = {
            if (uiState.pendingDeletion != null) {
                Snackbar(
                    modifier = Modifier.testTag("undo_snackbar"),
                    action = {
                        TextButton(
                            onClick = viewModel::undoDelete,
                            modifier = Modifier.testTag("undo_action"),
                        ) { Text("撤销") }
                    },
                ) { Text("已删除投递记录") }
            } else {
                SnackbarHost(snackbarHostState)
            }
        },
    ) { contentPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(contentPadding).padding(24.dp)) {
                Text("正在加载投递记录…")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(contentPadding).testTag("application_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "filters") {
                    FilterBar(
                        cities = uiState.availableCities,
                        city = uiState.filter.city,
                        status = uiState.filter.status,
                        onCitySelected = viewModel::selectCity,
                        onStatusSelected = viewModel::selectStatus,
                        onClear = viewModel::clearFilters,
                        hasActiveFilter = uiState.hasActiveFilter,
                    )
                }
                item(key = "due_header") {
                    SectionHeading("待跟进", uiState.dueRecords.size)
                }
                if (uiState.dueRecords.isEmpty()) {
                    item(key = "due_empty") {
                        EmptyCard(
                            if (uiState.hasActiveFilter) "当前筛选下没有待跟进记录" else "暂无到期跟进",
                            "已逾期和今天应跟进的非终态记录会出现在这里。",
                        )
                    }
                } else {
                    items(uiState.dueRecords, key = { "due_${it.id}" }) { record ->
                        ApplicationCard(
                            record = record,
                            today = uiState.today,
                            isDueSection = true,
                            canDelete = uiState.pendingDeletion == null,
                            onEdit = { onEdit(record.id) },
                            onDelete = { viewModel.delete(record.id) },
                            onOpenUrl = { url ->
                                try {
                                    uriHandler.openUri(url)
                                } catch (_: Exception) {
                                    scope.launch { snackbarHostState.showSnackbar("无法打开岗位链接") }
                                }
                            },
                        )
                    }
                }
                item(key = "all_header") {
                    SectionHeading("全部记录", uiState.visibleRecords.size)
                }
                if (uiState.visibleRecords.isEmpty()) {
                    item(key = "all_empty") {
                        EmptyCard(
                            if (uiState.allRecords.isEmpty()) "还没有投递记录" else "没有符合筛选条件的记录",
                            if (uiState.allRecords.isEmpty()) "点击右下角“新增”，记录第一条投递。"
                            else "可以调整城市或阶段，或清空筛选条件。",
                        )
                    }
                } else {
                    items(uiState.visibleRecords, key = { "all_${it.id}" }) { record ->
                        ApplicationCard(
                            record = record,
                            today = uiState.today,
                            isDueSection = false,
                            canDelete = uiState.pendingDeletion == null,
                            onEdit = { onEdit(record.id) },
                            onDelete = { viewModel.delete(record.id) },
                            onOpenUrl = { url ->
                                try {
                                    uriHandler.openUri(url)
                                } catch (_: Exception) {
                                    scope.launch { snackbarHostState.showSnackbar("无法打开岗位链接") }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterBar(
    cities: List<String>,
    city: String?,
    status: ApplicationStatus?,
    onCitySelected: (String?) -> Unit,
    onStatusSelected: (ApplicationStatus?) -> Unit,
    onClear: () -> Unit,
    hasActiveFilter: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("筛选", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            var cityExpanded by remember { mutableStateOf(false) }
            Box(Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { cityExpanded = true },
                    modifier = Modifier.fillMaxWidth().testTag("filter_city"),
                ) { Text(city ?: "全部城市", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                DropdownMenu(expanded = cityExpanded, onDismissRequest = { cityExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("全部城市") },
                        onClick = { onCitySelected(null); cityExpanded = false },
                        modifier = Modifier.testTag("filter_city_all"),
                    )
                    cities.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = { onCitySelected(option); cityExpanded = false },
                            modifier = Modifier.testTag("filter_city_${option}"),
                        )
                    }
                }
            }
            var statusExpanded by remember { mutableStateOf(false) }
            Box(Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { statusExpanded = true },
                    modifier = Modifier.fillMaxWidth().testTag("filter_status"),
                ) { Text(status?.displayName ?: "全部阶段", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                DropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("全部阶段") },
                        onClick = { onStatusSelected(null); statusExpanded = false },
                        modifier = Modifier.testTag("filter_status_all"),
                    )
                    ApplicationStatus.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.displayName) },
                            onClick = { onStatusSelected(option); statusExpanded = false },
                            modifier = Modifier.testTag("filter_status_${option.code}"),
                        )
                    }
                }
            }
        }
        if (hasActiveFilter) {
            TextButton(onClick = onClear, modifier = Modifier.testTag("filter_clear")) {
                Text("清空筛选")
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, count: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text("$count 条", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmptyCard(title: String, detail: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ApplicationCard(
    record: JobApplication,
    today: LocalDate,
    isDueSection: Boolean,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val tagSuffix = if (isDueSection) "due_${record.id}" else record.id.toString()
    Card(modifier = Modifier.fillMaxWidth().testTag("application_card_$tagSuffix")) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(record.companyName.trim(), style = MaterialTheme.typography.titleMedium)
                Text(record.status.displayName, color = MaterialTheme.colorScheme.primary)
            }
            Text(record.jobTitle.trim(), style = MaterialTheme.typography.bodyLarge)
            Text(record.city.trim(), style = MaterialTheme.typography.bodyMedium)
            val followUp = record.nextFollowUpDate
            if (followUp != null) {
                val dueLabel = when {
                    !isDueSection -> {
                        val prefix = if (record.status.isTerminal) "跟进日期" else "下次跟进"
                        "$prefix：${followUp.format(DateTimeFormatter.ISO_LOCAL_DATE)}"
                    }
                    followUp.isBefore(today) -> "已逾期 ${ChronoUnit.DAYS.between(followUp, today)} 天 · ${followUp.format(DateTimeFormatter.ISO_LOCAL_DATE)}"
                    else -> "今天跟进 · ${followUp.format(DateTimeFormatter.ISO_LOCAL_DATE)}"
                }
                Text(
                    dueLabel,
                    color = if (isDueSection) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                record.jobUrl?.let { url ->
                    TextButton(onClick = { onOpenUrl(url) }) { Text("岗位链接") }
                }
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("application_edit_$tagSuffix"),
                ) { Text("编辑") }
                Spacer(Modifier.width(4.dp))
                TextButton(
                    onClick = onDelete,
                    enabled = canDelete,
                    modifier = Modifier.testTag("application_delete_$tagSuffix"),
                ) { Text("删除") }
            }
        }
    }
}
