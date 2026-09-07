package dev.dheirav.thirsttrap.data

import android.util.Log
import dev.dheirav.thirsttrap.data.BuildConfig

/**
 * One place for the app's logging, with a fixed set of tags so a log can be
 * filtered to the thing you care about:
 *
 *     adb logcat -s TTPhoto        just the photo pipeline
 *     adb logcat -s TTReminder     just scheduling and notifications
 *     adb logcat -s TTData         every write to the database
 *     adb logcat -s TTUi           what the user actually tapped
 *
 * Two bugs in the photo pipeline reached a real device precisely because
 * nothing logged: the camera reported success, the app looked fine, and the
 * photo vanished. Writes and user actions log at INFO for that reason.
 *
 * Debug-only by default. Warnings and errors always go out, because a silent
 * failure in release is exactly what happened before.
 */
object TTLog {

    const val PHOTO = "TTPhoto"
    const val REMINDER = "TTReminder"
    const val DATA = "TTData"
    const val UI = "TTUi"

    fun i(tag: String, message: () -> String) {
        if (BuildConfig.DEBUG) Log.i(tag, message())
    }

    fun w(tag: String, message: () -> String, error: Throwable? = null) {
        Log.w(tag, message(), error)
    }

    fun e(tag: String, message: () -> String, error: Throwable? = null) {
        Log.e(tag, message(), error)
    }
}
