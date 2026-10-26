package com.example.tasktracker.internship.ui.form

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tasktracker.internship.model.ApplicationStatus
import com.example.tasktracker.internship.util.ApplicationField
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationFormScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ApplicationFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDiscardConfirmation by rememberSaveable { mutableStateOf(false) }
    val requestBack: () -> Unit = {
        if (!uiState.isSaving) {
            if (uiState.hasUnsavedChanges) showDiscardConfirmation = true else onBack()
        }
    }

    BackHandler(enabled = !uiState.saveSucceeded, onBack = requestBack)
    LaunchedEffect(uiState.saveSucceeded) {
        if (uiState.saveSucceeded) onSaved()
    }

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text("放弃修改？") },
            text = { Text("尚未保存的内容会丢失。") },
            confirmButton = {
                TextButton(
                    onClick = { showDiscardConfirmation = false; onBack() },
                    modifier = Modifier.testTag("form_discard_confirm"),
                ) { Text("放弃修改") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmation = false }) { Text("继续编辑") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.recordId == null) "新增投递" else "编辑投递") },
                navigationIcon = {
                    TextButton(onClick = requestBack, modifier = Modifier.testTag("form_back")) {
                        Text("返回")
                    }
                },
            )
        },
    ) { contentPadding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(contentPadding).padding(24.dp)) {
                Text("正在读取记录…")
            }
            uiState.loadFailed -> Column(
                Modifier.fillMaxSize().padding(contentPadding).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(uiState.message ?: "读取记录失败")
                Button(onClick = onBack) { Text("返回列表") }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(contentPadding).imePadding().testTag("form_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "company") {
                    OutlinedTextField(
                        value = uiState.values.companyName,
                        onValueChange = viewModel::setCompanyName,
                        label = { Text("公司名称 *") },
                        singleLine = true,
                        isError = uiState.fieldErrors.containsKey(ApplicationField.COMPANY_NAME),
                        supportingText = uiState.fieldErrors[ApplicationField.COMPANY_NAME]?.let { error ->
                            { Text(error) }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("form_company"),
                    )
                }
                item(key = "job_title") {
                    OutlinedTextField(
                        value = uiState.values.jobTitle,
                        onValueChange = viewModel::setJobTitle,
                        label = { Text("岗位名称 *") },
                        singleLine = true,
                        isError = uiState.fieldErrors.containsKey(ApplicationField.JOB_TITLE),
                        supportingText = uiState.fieldErrors[ApplicationField.JOB_TITLE]?.let { error ->
                            { Text(error) }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("form_job_title"),
                    )
                }
                item(key = "city") {
                    OutlinedTextField(
                        value = uiState.values.city,
                        onValueChange = viewModel::setCity,
                        label = { Text("城市 *") },
                        singleLine = true,
                        isError = uiState.fieldErrors.containsKey(ApplicationField.CITY),
                        supportingText = uiState.fieldErrors[ApplicationField.CITY]?.let { error ->
                            { Text(error) }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("form_city"),
                    )
                }
                item(key = "url") {
                    OutlinedTextField(
                        value = uiState.values.jobUrl,
                        onValueChange = viewModel::setJobUrl,
                        label = { Text("岗位链接（可选）") },
                        singleLine = true,
                        isError = uiState.fieldErrors.containsKey(ApplicationField.JOB_URL),
                        supportingText = uiState.fieldErrors[ApplicationField.JOB_URL]?.let { error ->
                            { Text(error) }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("form_url"),
                    )
                }
                item(key = "status") {
                    StatusSelector(uiState.values.status, viewModel::setStatus)
                }
                item(key = "applied_date") {
                    DateSelector(
                        label = "投递日期",
                        date = uiState.values.appliedDate,
                        onDateChanged = viewModel::setAppliedDate,
                        tag = "form_applied_date",
                    )
                }
                item(key = "follow_up") {
                    DateSelector(
                        label = "下次跟进日期",
                        date = uiState.values.nextFollowUpDate,
                        onDateChanged = viewModel::setNextFollowUpDate,
                        tag = "form_follow_up_date",
                    )
                }
                item(key = "note") {
                    OutlinedTextField(
                        value = uiState.values.note,
                        onValueChange = viewModel::setNote,
                        label = { Text("备注（可选）") },
                        minLines = 3,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth().testTag("form_note"),
                    )
                }
                uiState.message?.let { message ->
                    item(key = "form_message") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text(
                                message,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
                item(key = "save") {
                    Button(
                        onClick = viewModel::save,
                        enabled = !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth().testTag("form_save"),
                    ) { Text(if (uiState.isSaving) "保存中…" else "保存记录") }
                }
            }
        }
    }
}

@Composable
private fun StatusSelector(status: ApplicationStatus, onSelected: (ApplicationStatus) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("投递阶段", style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.testTag("form_status_menu"),
            ) { Text(status.displayName) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                ApplicationStatus.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.displayName) },
                        onClick = { onSelected(option); expanded = false },
                        modifier = Modifier.testTag("form_status_${option.code}"),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelector(
    label: String,
    date: LocalDate?,
    onDateChanged: (LocalDate?) -> Unit,
    tag: String,
) {
    var showPicker by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.testTag(tag)) {
                Text(date?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: "选择日期")
            }
            if (date != null) {
                TextButton(onClick = { onDateChanged(null) }) { Text("清除") }
            }
        }
    }
    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date?.toEpochDay()?.times(MILLIS_PER_DAY),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onDateChanged(LocalDate.ofEpochDay(Math.floorDiv(millis, MILLIS_PER_DAY)))
                        }
                        showPicker = false
                    },
                    modifier = Modifier.testTag("${tag}_confirm"),
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private const val MILLIS_PER_DAY = 86_400_000L
