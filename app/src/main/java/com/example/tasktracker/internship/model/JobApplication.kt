package com.example.tasktracker.internship.model

import java.time.LocalDate

/** The UI-facing model; Room entities stay inside the data layer. */
data class JobApplication(
    val id: Long,
    val companyName: String,
    val jobTitle: String,
    val city: String,
    val jobUrl: String?,
    val appliedDate: LocalDate?,
    val status: ApplicationStatus,
    val nextFollowUpDate: LocalDate?,
    val note: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

/** Values edited by the form. Existing identity and timestamps are managed by the repository. */
data class JobApplicationInput(
    val companyName: String,
    val jobTitle: String,
    val city: String,
    val jobUrl: String? = null,
    val appliedDate: LocalDate? = null,
    val status: ApplicationStatus = ApplicationStatus.PENDING,
    val nextFollowUpDate: LocalDate? = null,
    val note: String = "",
)

fun JobApplication.toInput(): JobApplicationInput = JobApplicationInput(
    companyName = companyName,
    jobTitle = jobTitle,
    city = city,
    jobUrl = jobUrl,
    appliedDate = appliedDate,
    status = status,
    nextFollowUpDate = nextFollowUpDate,
    note = note,
)
