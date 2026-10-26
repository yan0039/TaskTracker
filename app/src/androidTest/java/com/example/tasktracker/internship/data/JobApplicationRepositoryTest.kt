package com.example.tasktracker.internship.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.tasktracker.internship.model.ApplicationStatus
import com.example.tasktracker.internship.model.JobApplicationInput
import com.example.tasktracker.internship.model.toInput
import com.example.tasktracker.internship.util.LocalDateProvider
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JobApplicationRepositoryTest {
    private lateinit var database: InternshipDatabase
    private lateinit var repository: JobApplicationRepository
    private val initialInstant = Instant.parse("2026-10-25T08:00:00Z")
    private val zone = ZoneId.of("Asia/Shanghai")

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, InternshipDatabase::class.java).build()
        repository = repositoryAt(initialInstant)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createUpdateDeleteAndRestoreRoundTripThroughRoom() = runBlocking {
        val appliedDate = LocalDate.of(2026, 10, 23)
        val followUpDate = LocalDate.of(2026, 10, 26)
        val id = repository.create(
            JobApplicationInput(
                companyName = "  示例科技  ",
                jobTitle = " Android 实习生 ",
                city = " 上海 ",
                jobUrl = " https://example.com/jobs/1 ",
                appliedDate = appliedDate,
                nextFollowUpDate = followUpDate,
                note = "初次投递",
            ),
        )

        val created = repository.getById(id)
        assertNotNull(created)
        assertEquals(id, created!!.id)
        assertEquals("示例科技", created.companyName)
        assertEquals("Android 实习生", created.jobTitle)
        assertEquals(" 上海 ", created.city)
        assertEquals("https://example.com/jobs/1", created.jobUrl)
        assertEquals(appliedDate, created.appliedDate)
        assertEquals(followUpDate, created.nextFollowUpDate)
        assertEquals(ApplicationStatus.PENDING, created.status)
        assertEquals(listOf(id), repository.observeAll().first().map { it.id })

        val laterRepository = repositoryAt(initialInstant.plusSeconds(60))
        assertTrue(laterRepository.update(id, created.toInput().copy(status = ApplicationStatus.INTERVIEW, note = "安排面试")))
        val edited = laterRepository.getById(id)!!
        assertEquals(ApplicationStatus.INTERVIEW, edited.status)
        assertEquals("安排面试", edited.note)
        assertEquals(created.createdAtMillis, edited.createdAtMillis)
        assertTrue(edited.updatedAtMillis > created.updatedAtMillis)
        assertEquals(ApplicationStatus.INTERVIEW, repository.observeAll().first().single().status)

        val snapshot = laterRepository.delete(id)
        assertEquals(edited, snapshot)
        assertNull(repository.getById(id))
        assertTrue(repository.observeAll().first().isEmpty())

        // Inserting another row cannot change the restored row's original identifier or fields.
        repository.create(JobApplicationInput("其他公司", "测试", "北京"))
        repository.restore(snapshot!!)
        assertEquals(edited, repository.getById(id))
        assertEquals(2, repository.observeAll().first().size)
    }

    @Test
    fun updateMissingRowReturnsFalseAndInvalidInputNeverWrites() = runBlocking {
        assertFalse(repository.update(99, JobApplicationInput("公司", "岗位", "城市")))
        assertNull(repository.delete(99))
        try {
            repository.create(JobApplicationInput(" ", "岗位", "城市"))
            throw AssertionError("Expected input validation failure")
        } catch (_: IllegalArgumentException) {
            assertTrue(repository.observeAll().first().isEmpty())
        }
    }

    private fun repositoryAt(instant: Instant): JobApplicationRepository =
        JobApplicationRepository(
            database.jobApplicationDao(),
            LocalDateProvider(Clock.fixed(instant, zone)),
        )
}
