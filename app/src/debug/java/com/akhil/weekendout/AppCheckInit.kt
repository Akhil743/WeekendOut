package com.akhil.weekendout

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug-only App Check provider. Mints a debug token whose UUID is logged once
 * to logcat ("Enter this debug secret into the allow list in the Firebase
 * Console for your project: …"). That UUID must be registered in Firebase
 * Console > App Check > debug tokens.
 */
fun installAppCheckProvider(appCheck: FirebaseAppCheck) {
    appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
}
