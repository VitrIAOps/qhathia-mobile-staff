package com.qhathia.mobile.staff.core.testing

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** A [Clock] tests can move forward, for staleness and relative-time rules. */
class MutableClock(
    var now: Instant = TestFixtures.NOW,
    private val zone: ZoneId = ZoneOffset.UTC,
) : Clock() {

    fun advanceBy(duration: Duration) {
        now = now.plus(duration)
    }

    override fun instant(): Instant = now

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = MutableClock(now, zone)
}
