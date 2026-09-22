package com.tzh.baselib

import com.tzh.baselib.util.general.ObservableUtil
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.TestScheduler
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class ObservableUtilTest {
    private val scheduler = TestScheduler()
    @Before fun setup() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { scheduler }
        RxAndroidPlugins.setMainThreadSchedulerHandler { scheduler }
        RxJavaPlugins.setComputationSchedulerHandler { scheduler }
        RxJavaPlugins.setIoSchedulerHandler { scheduler }
    }
    @After fun cleanup() {
        ObservableUtil.stopTimer("test")
        RxAndroidPlugins.reset()
        RxJavaPlugins.reset()
    }

    @Test fun stoppingReleasesBothEntriesAndPreventsFurtherCallbacks() {
        var ticks = 0
        ObservableUtil.startTimer(10, "test") { ticks++ }
        scheduler.advanceTimeBy(20, TimeUnit.MILLISECONDS)
        assertEquals(2, ticks)
        ObservableUtil.stopTimer("test")
        scheduler.advanceTimeBy(20, TimeUnit.MILLISECONDS)
        assertEquals(2, ticks)
        for (name in listOf("disposableMap", "listerMap")) {
            val field = ObservableUtil::class.java.getDeclaredField(name)
            field.isAccessible = true
            assertFalse((field.get(null) as Map<*, *>).containsKey("test"))
        }
        ObservableUtil.stopTimer("test")
    }

    @Test fun restartingSameKeyDisposesOldSubscription() {
        var oldTicks = 0
        var newTicks = 0
        ObservableUtil.startTimer(10, "test") { oldTicks++ }
        scheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS)
        ObservableUtil.startTimer(10, "test") { newTicks++ }
        scheduler.advanceTimeBy(20, TimeUnit.MILLISECONDS)
        assertEquals(1, oldTicks)
        assertEquals(2, newTicks)
    }
}