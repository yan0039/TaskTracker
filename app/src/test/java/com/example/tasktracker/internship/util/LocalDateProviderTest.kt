package com.example.tasktracker.internship.util

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalDateProviderTest {
    @Test
    fun todayUsesInjectedLocalZoneAtMidnightBoundary() {
        val instant = Instant.parse("2026-10-25T16:30:00Z")
        val shanghai = LocalDateProvider(Clock.fixed(instant, ZoneId.of("Asia/Shanghai")))
        val utc = LocalDateProvider(Clock.fixed(instant, ZoneId.of("UTC")))

        assertEquals(LocalDate.of(2026, 10, 26), shanghai.today())
        assertEquals(LocalDate.of(2026, 10, 25), utc.today())
        assertEquals(instant.toEpochMilli(), shanghai.nowMillis())
    }

    @Test
    fun deviceClockReadsChangedZoneOnEachCall() {
        val original = TimeZone.getDefault()
        try {
            val clock = DeviceLocalClock()
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            assertEquals(ZoneId.of("UTC"), clock.zone)
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
            assertEquals(ZoneId.of("Asia/Shanghai"), clock.zone)
        } finally {
            TimeZone.setDefault(original)
        }
    }
}
