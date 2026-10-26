package com.example.tasktracker.internship.util

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Reads local calendar days from an injectable clock for deterministic deadline tests. */
class LocalDateProvider @Inject constructor(private val clock: Clock) {
    fun today(): LocalDate = LocalDate.now(clock)

    fun nowMillis(): Long = clock.millis()
}

/** Resolve the device zone on each read so a foreground refresh also handles zone changes. */
class DeviceLocalClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = Clock.system(zone)

    override fun instant(): Instant = Instant.now()
}
