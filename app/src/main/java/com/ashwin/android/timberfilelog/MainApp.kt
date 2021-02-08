package com.ashwin.android.timberfilelog

import android.app.Application

class MainApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AnalyticsManager.getInstance(this)
    }
}