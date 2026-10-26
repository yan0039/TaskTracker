package com.example.tasktracker.internship.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JobApplicationDao {
    @Query("SELECT * FROM job_application ORDER BY updatedAtMillis DESC, id DESC")
    fun observeAll(): Flow<List<JobApplicationEntity>>

    @Query("SELECT * FROM job_application WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): JobApplicationEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: JobApplicationEntity): Long

    @Update
    suspend fun update(record: JobApplicationEntity): Int

    @Delete
    suspend fun delete(record: JobApplicationEntity): Int

    /** Snapshot and deletion share a transaction so undo restores exactly what was removed. */
    @Transaction
    suspend fun deleteByIdReturningSnapshot(id: Long): JobApplicationEntity? {
        val record = getById(id) ?: return null
        return if (delete(record) == 1) record else null
    }
}
