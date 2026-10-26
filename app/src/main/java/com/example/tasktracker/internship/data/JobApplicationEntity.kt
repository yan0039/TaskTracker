package com.example.tasktracker.internship.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.tasktracker.internship.model.ApplicationStatus
import com.example.tasktracker.internship.model.JobApplication
import java.time.LocalDate

@Entity(tableName = "job_application")
data class JobApplicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyName: String,
    val jobTitle: String,
    val city: String,
    val jobUrl: String?,
    val appliedEpochDay: Long?,
    val status: String,
    val nextFollowUpEpochDay: Long?,
    val note: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

internal fun JobApplicationEntity.toModel(): JobApplication = JobApplication(
    id = id,
    companyName = companyName,
    jobTitle = jobTitle,
    city = city,
    jobUrl = jobUrl,
    appliedDate = appliedEpochDay?.let(LocalDate::ofEpochDay),
    status = ApplicationStatus.fromCode(status),
    nextFollowUpDate = nextFollowUpEpochDay?.let(LocalDate::ofEpochDay),
    note = note,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

internal fun JobApplication.toEntity(): JobApplicationEntity = JobApplicationEntity(
    id = id,
    companyName = companyName,
    jobTitle = jobTitle,
    city = city,
    jobUrl = jobUrl,
    appliedEpochDay = appliedDate?.toEpochDay(),
    status = status.code,
    nextFollowUpEpochDay = nextFollowUpDate?.toEpochDay(),
    note = note,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)
