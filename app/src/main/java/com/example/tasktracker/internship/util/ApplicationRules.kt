package com.example.tasktracker.internship.util

import com.example.tasktracker.internship.model.ApplicationStatus
import com.example.tasktracker.internship.model.JobApplication
import com.example.tasktracker.internship.model.JobApplicationInput
import java.net.URI
import java.net.URISyntaxException
import java.time.LocalDate

enum class ApplicationField { COMPANY_NAME, JOB_TITLE, CITY, JOB_URL }

class InvalidApplicationException(val fieldErrors: Map<ApplicationField, String>) :
    IllegalArgumentException(fieldErrors.values.firstOrNull() ?: "投递记录输入无效")

data class ApplicationFilter(
    val city: String? = null,
    val status: ApplicationStatus? = null,
)

/** Validation is pure so the form can show errors before attempting a database write. */
fun validateJobApplicationInput(input: JobApplicationInput): Map<ApplicationField, String> = buildMap {
    if (input.companyName.isBlank()) put(ApplicationField.COMPANY_NAME, "请填写公司名称")
    if (input.jobTitle.isBlank()) put(ApplicationField.JOB_TITLE, "请填写岗位名称")
    if (input.city.isBlank()) put(ApplicationField.CITY, "请填写城市")
    val url = input.jobUrl?.trim().orEmpty()
    if (url.isNotEmpty() && !isHttpUrl(url)) {
        put(ApplicationField.JOB_URL, "岗位链接须以 http:// 或 https:// 开头并包含有效主机")
    }
}

private fun isHttpUrl(value: String): Boolean = try {
    val uri = URI(value)
    (uri.scheme.equals("http", ignoreCase = true) ||
        uri.scheme.equals("https", ignoreCase = true)) &&
        !uri.host.isNullOrBlank() &&
        uri.port >= -1 &&
        uri.userInfo == null
} catch (_: URISyntaxException) {
    false
}

/** Filter both the full list and follow-up list with this same function. */
fun filterApplications(
    records: List<JobApplication>,
    filter: ApplicationFilter,
): List<JobApplication> {
    val city = filter.city?.trim().orEmpty()
    return records.filter { record ->
        (city.isEmpty() || record.city.trim().equals(city, ignoreCase = true)) &&
            (filter.status == null || record.status == filter.status)
    }
}

/** Today and overdue are included; terminal stages retain their dates but are excluded. */
fun dueFollowUps(records: List<JobApplication>, today: LocalDate): List<JobApplication> =
    records.asSequence()
        .filter { record ->
            val date = record.nextFollowUpDate
            date != null && !date.isAfter(today) && !record.status.isTerminal
        }
        .sortedWith(compareBy<JobApplication> { it.nextFollowUpDate }.thenBy { it.id })
        .toList()
