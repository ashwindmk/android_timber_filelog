package com.ashwin.android.timberfilelog

class MainPresenter(val view: MainContract.View) {
    fun trackEvent(event: String) {
        AnalyticsManager.getInstance(view.getContext()).logEvent(event)
    }
}