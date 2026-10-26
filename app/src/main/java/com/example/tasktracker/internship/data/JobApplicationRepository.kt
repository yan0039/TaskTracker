package com.example.tasktracker.internship.data

import com.example.tasktracker.internship.model.JobApplication
import com.example.tasktracker.internship.model.JobApplicationInput
import com.example.tasktracker.internship.util.InvalidApplicationException
import com.example.tasktracker.internship.util.LocalDateProvider
import com.example.tasktracker.internship.util.validateJobApplicationInput
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The only write path for application records. Persistence errors propagate to the caller. */
class JobApplicationRepository @Inject constructor(
    private val dao: JobApplicationDao,
    private val dateProvider: LocalDateProvider,
) {
    fun observeAll(): Flow<List<JobApplication>> =
        dao.observeAll().map { rows -> rows.map(JobApplicationEntity::toModel) }

    suspend fun getById(id: Long): JobApplication? = dao.getById(id)?.toModel()

    suspend fun create(input: JobApplicationInput): Long {
        val validated = input.validated()
        val now = dateProvider.nowMillis()
        return dao.insert(
            JobApplicationEntity(
                companyName = validated.companyName,
                jobTitle = validated.jobTitle,
                city = validated.city,
                jobUrl = validated.jobUrl,
                appliedEpochDay = validated.appliedDate?.toEpochDay(),
                status = validated.status.code,
                nextFollowUpEpochDay = validated.nextFollowUpDate?.toEpochDay(),
                note = validated.note,
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
        )
    }

    /** Returns false if the record was already deleted; it never inserts a replacement row. */
    suspend fun update(id: Long, input: JobApplicationInput): Boolean {
        val validated = input.validated()
        val existing = dao.getById(id) ?: return false
        return dao.update(
            existing.copy(
                companyName = validated.companyName,
                jobTitle = validated.jobTitle,
                city = validated.city,
                jobUrl = validated.jobUrl,
                appliedEpochDay = validated.appliedDate?.toEpochDay(),
                status = validated.status.code,
                nextFollowUpEpochDay = validated.nextFollowUpDate?.toEpochDay(),
                note = validated.note,
                updatedAtMillis = dateProvider.nowMillis(),
            ),
        ) == 1
    }

    /** Returns a complete persisted snapshot for a 5-second undo action. */
    suspend fun delete(id: Long): JobApplication? = dao.deleteByIdReturningSnapshot(id)?.toModel()

    /** Reinserts the original ID and all fields. A conflicting row fails rather than overwriting data. */
    suspend fun restore(record: JobApplication) {
        require(record.id > 0) { "只能恢复已有的投递记录" }
        dao.insert(record.toEntity())
    }
}

private fun JobApplicationInput.validated(): JobApplicationInput {
    val errors = validateJobApplicationInput(this)
    if (errors.isNotEmpty()) throw InvalidApplicationException(errors)
    return copy(
        companyName = companyName.trim(),
        jobTitle = jobTitle.trim(),
        // Keep the original city for editing; filters and display trim it.
        city = city,
        jobUrl = jobUrl?.trim()?.ifEmpty { null },
    )
}
