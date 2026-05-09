package com.akhil.weekendout

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WeekendOutApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        // Provider factory differs per build type — see src/debug & src/release.
        installAppCheckProvider(FirebaseAppCheck.getInstance())
    }
}
