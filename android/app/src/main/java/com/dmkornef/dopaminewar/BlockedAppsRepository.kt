package com.dmkornef.dopaminewar

import android.content.Context

class BlockedAppsRepository(context: Context) {

    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    init {
        ensureDefaultBlockedApps()
    }

    fun getBlockedPackages(): Set<String> {
        return preferences.getStringSet(KEY_BLOCKED_PACKAGES, emptySet())
            ?.toSet()
            ?: emptySet()
    }

    fun isBlocked(packageName: String): Boolean {
        return packageName in getBlockedPackages()
    }

    fun blockApp(packageName: String) {
        val currentPackages = getBlockedPackages().toMutableSet()
        currentPackages.add(packageName)
        saveBlockedPackages(currentPackages)
    }

    fun requestUnblockApp(packageName: String) {
        // Telegram approval will be connected here later.
        // For now we intentionally do NOT remove the app from the blocked list.
    }

    private fun ensureDefaultBlockedApps() {
        if (preferences.contains(KEY_BLOCKED_PACKAGES)) {
            return
        }

        saveBlockedPackages(DEFAULT_BLOCKED_PACKAGES)
    }

    private fun saveBlockedPackages(packages: Set<String>) {
        preferences.edit()
            .putStringSet(KEY_BLOCKED_PACKAGES, packages)
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "blocked_apps_preferences"
        private const val KEY_BLOCKED_PACKAGES = "blocked_packages"

        private val DEFAULT_BLOCKED_PACKAGES = setOf(
            "com.google.android.youtube",
            "com.instagram.android"
        )
    }
}