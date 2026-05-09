package com.akhil.weekendout.ui

import com.akhil.weekendout.data.model.UserPrefs
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tiny in-memory hand-off between PlannerViewModel (writes) and ResultsViewModel
 * (reads). Avoids serializing UserPrefs into the navigation route. Cleared after
 * a successful read.
 */
@Singleton
class PrefsHolder @Inject constructor() {
    @Volatile private var current: UserPrefs? = null
    fun set(p: UserPrefs) { current = p }
    fun take(): UserPrefs? = current.also { current = null }
    fun peek(): UserPrefs? = current
}
