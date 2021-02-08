package com.ashwin.android.timberfilelog

import android.content.Context
import timber.log.Timber

class AnalyticsManager {
    companion object {
        @Volatile
        private var INSTANCE: AnalyticsManager? = null

        @JvmStatic
        fun getInstance(context: Context): AnalyticsManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildInstance(context).also { INSTANCE = it }
            }

        private fun buildInstance(context: Context): AnalyticsManager {
            Timber.plant(Timber.DebugTree())
            Timber.plant(FileLoggingTree(context))
            return AnalyticsManager()
        }
    }

    fun logEvent(event: String) {
        Timber.d("event: $event")
    }
}