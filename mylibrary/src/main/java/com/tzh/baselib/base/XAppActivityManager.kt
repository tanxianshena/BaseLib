package com.tzh.baselib.base

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Looper
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import com.tzh.baselib.R
import java.lang.ref.WeakReference

/** Tracks explicitly registered activities; the top is the most recently added/resumed one.
 * All stack operations belong on the main thread. A top activity is not necessarily visible.
 */
@MainThread
class XAppActivityManager private constructor() {
    private val activities = mutableListOf<WeakReference<AppCompatActivity>>()

    companion object {
        private val singleton by lazy { XAppActivityManager() }

        @JvmStatic
        fun getInstance(): XAppActivityManager = singleton

        @JvmStatic
        fun startActivityRtl(context: Context, intent: Intent) {
            requireMainThread()
            var host = context
            while (host is ContextWrapper && host !is Activity) {
                val next = host.baseContext
                if (next === host) break
                host = next
            }
            // Do not mutate the caller's Intent when starting from an application context.
            val launchIntent = Intent(intent)
            if (host !is Activity) launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val options = ActivityOptionsCompat.makeCustomAnimation(context,
                R.anim.activity_slide_right_in, R.anim.activity_slide_left_out)
            context.startActivity(launchIntent, options.toBundle())
        }

        private fun requireMainThread() {
            check(Looper.myLooper() == Looper.getMainLooper()) {
                "Activity management must run on the main thread"
            }
        }
    }

    private fun snapshot(): List<AppCompatActivity> {
        requireMainThread()
        val live = ArrayList<AppCompatActivity>(activities.size)
        val iterator = activities.iterator()
        while (iterator.hasNext()) {
            val activity = iterator.next().get()
            if (activity == null || activity.isFinishing || activity.isDestroyed) {
                iterator.remove()
            } else {
                live.add(activity)
            }
        }
        return live
    }

    fun addActivity(activity: AppCompatActivity?) {
        val live = snapshot()
        if (activity == null || activity.isFinishing || activity.isDestroyed) return
        if (live.none { it === activity }) activities.add(WeakReference(activity))
    }

    /** Call from onResume when manually integrating an activity outside XBaseBindingActivity. */
    fun markActivityResumed(activity: AppCompatActivity?) {
        removeActivity(activity)
        addActivity(activity)
    }

    fun currentActivity(): AppCompatActivity? = snapshot().lastOrNull()

    fun removeActivity(activity: AppCompatActivity?) {
        snapshot()
        activities.removeAll { it.get() === activity }
    }

    fun finishActivity(activity: AppCompatActivity?) {
        removeActivity(activity)
        if (activity != null && !activity.isFinishing && !activity.isDestroyed) activity.finish()
    }

    /** Preserves the old behavior: finish the first matching registered instance. */
    fun finishActivity(cls: Class<*>) {
        finishActivity(getActivityByClass(cls))
    }

    /** Retain only the most recent instance of this exact class; finish all if absent. */
    fun finishAllActivityExceptOne(cls: Class<*>) {
        val live = snapshot()
        finishSnapshot(live, live.lastOrNull { it.javaClass == cls })
    }

    fun finishAllActivityExceptOne(activity: AppCompatActivity?) {
        val live = snapshot()
        finishSnapshot(live, live.firstOrNull { it === activity })
    }

    fun haveActivity(cls: Class<*>): Boolean = getActivityByClass(cls) != null

    fun finishAllActivity() = finishSnapshot(snapshot(), null)

    fun getActivityByClass(cls: Class<*>): AppCompatActivity? =
        snapshot().firstOrNull { it.javaClass == cls }

    fun isCurrentActivity(cls: Class<*>): Boolean = currentActivity()?.javaClass == cls

    private fun finishSnapshot(live: List<AppCompatActivity>, retained: AppCompatActivity?) {
        // Update first: finish callbacks may synchronously remove activities or add new ones.
        activities.clear()
        retained?.let { activities.add(WeakReference(it)) }
        live.forEach {
            if (it !== retained && !it.isFinishing && !it.isDestroyed) it.finish()
        }
    }
}
