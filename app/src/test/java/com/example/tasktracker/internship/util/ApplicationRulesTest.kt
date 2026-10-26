package com.example.tasktracker.internship.util

import com.example.tasktracker.internship.model.ApplicationStatus
import com.example.tasktracker.internship.model.JobApplication
import com.example.tasktracker.internship.model.JobApplicationInput
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApplicationRulesTest {
    private val today = LocalDate.of(2026, 10, 25)

    @Test
    fun dueIncludesYesterdayAndTodayInDateOrderAndRecalculatesOnNextDay() {
        val yesterday = record(1, followUp = today.minusDays(1))
        val dueToday = record(2, followUp = today)
        val tomorrow = record(3, followUp = today.plusDays(1))
        val noDate = record(4)
        val offer = record(5, status = ApplicationStatus.OFFER, followUp = yesterday.nextFollowUpDate)
        val rejected = record(6, status = ApplicationStatus.REJECTED, followUp = dueToday.nextFollowUpDate)
        val records = listOf(tomorrow, noDate, dueToday, offer, yesterday, rejected)

        assertEquals(listOf(1L, 2L), dueFollowUps(records, today).map { it.id })
        assertEquals(listOf(1L, 2L, 3L), dueFollowUps(records, today.plusDays(1)).map { it.id })
    }

    @Test
    fun movingOutOfTerminalStatusReenablesSavedFollowUpDate() {
        val terminal = record(1, status = ApplicationStatus.OFFER, followUp = today.minusDays(2))
        assertTrue(dueFollowUps(listOf(terminal), today).isEmpty())
        assertEquals(listOf(terminal.id), dueFollowUps(listOf(terminal.copy(status = ApplicationStatus.INTERVIEW)), today).map { it.id })
    }

    @Test
    fun cityAndStatusFiltersComposeAndCanBeCleared() {
        val shanghaiApplied = record(1, city = " 上海 ", status = ApplicationStatus.APPLIED)
        val shanghaiInterview = record(2, city = "上海", status = ApplicationStatus.INTERVIEW)
        val beijingApplied = record(3, city = "北京", status = ApplicationStatus.APPLIED)
        val records = listOf(shanghaiApplied, shanghaiInterview, beijingApplied)

        assertEquals(listOf(1L, 2L), filterApplications(records, ApplicationFilter(city = "上海")).map { it.id })
        assertEquals(listOf(1L, 3L), filterApplications(records, ApplicationFilter(status = ApplicationStatus.APPLIED)).map { it.id })
        assertEquals(listOf(1L), filterApplications(records, ApplicationFilter(" 上海 ", ApplicationStatus.APPLIED)).map { it.id })
        assertEquals(records, filterApplications(records, ApplicationFilter()))
        assertTrue(filterApplications(records, ApplicationFilter("广州", ApplicationStatus.APPLIED)).isEmpty())
    }

    @Test
    fun validationRejectsBlankRequiredFieldsAndNonHttpLinks() {
        val input = JobApplicationInput("  ", "", " \t ", "javascript:alert(1)")
        val errors = validateJobApplicationInput(input)
        assertEquals(ApplicationField.entries.toSet(), errors.keys)

        val incompleteHttp = input.copy(companyName = "公司", jobTitle = "岗位", city = "城市", jobUrl = "https://")
        assertTrue(ApplicationField.JOB_URL in validateJobApplicationInput(incompleteHttp))
        assertTrue(ApplicationField.JOB_URL in validateJobApplicationInput(incompleteHttp.copy(jobUrl = "https://exa mple.com")))
        assertFalse(ApplicationField.JOB_URL in validateJobApplicationInput(incompleteHttp.copy(jobUrl = " https://example.com/jobs/1 ")))
        assertTrue(validateJobApplicationInput(incompleteHttp.copy(jobUrl = " ")).isEmpty())
    }

    private fun record(
        id: Long,
        city: String = "上海",
        status: ApplicationStatus = ApplicationStatus.PENDING,
        followUp: LocalDate? = null,
    ) = JobApplication(
        id = id,
        companyName = "示例公司$id",
        jobTitle = "Android 实习生",
        city = city,
        jobUrl = null,
        appliedDate = null,
        status = status,
        nextFollowUpDate = followUp,
        note = "",
        createdAtMillis = 10,
        updatedAtMillis = 10,
    )
}
