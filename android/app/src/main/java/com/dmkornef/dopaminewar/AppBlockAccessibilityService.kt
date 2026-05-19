package com.dmkornef.dopaminewar

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class AppBlockAccessibilityService : AccessibilityService() {

    private val blockedAppsRepository by lazy {
        BlockedAppsRepository(applicationContext)
    }

    private val ignoredRootPackages = setOf(
        "android",
        "com.android.systemui",
        "com.google.android.inputmethod.latin",
        "com.samsung.android.honeyboard"
    )

    private val launcherPackages = setOf(
        "com.sec.android.app.launcher",
        "com.google.android.apps.nexuslauncher",
        "com.android.launcher3"
    )

    private var activeBlockPackageName: String? = null
    private var lastBlockLaunchTimeMs: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val eventPackage = event?.packageName?.toString()
        val rootPackage = rootInActiveWindow?.packageName?.toString()
        val eventType = event?.eventType ?: -1

        Log.d(
            TAG,
            "EVENT type=${eventTypeName(eventType)} eventPackage=$eventPackage rootPackage=$rootPackage activeBlock=$activeBlockPackageName"
        )

        if (rootPackage == null) {
            return
        }

        if (rootPackage == this.packageName) {
            return
        }

        if (rootPackage in ignoredRootPackages) {
            return
        }

        if (rootPackage in launcherPackages) {
            activeBlockPackageName = null
            return
        }

        if (blockedAppsRepository.isBlocked(rootPackage)) {
            openBlockActivity(rootPackage)
            return
        }

        if (isLaunchableApp(rootPackage)) {
            activeBlockPackageName = null
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "SERVICE interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "SERVICE connected")
    }

    private fun openBlockActivity(blockedPackageName: String) {
        val now = System.currentTimeMillis()

        if (
            activeBlockPackageName == blockedPackageName &&
            now - lastBlockLaunchTimeMs < BLOCK_ACTIVITY_LAUNCH_COOLDOWN_MS
        ) {
            Log.d(TAG, "ACTION block activity launch skipped by cooldown package=$blockedPackageName")
            return
        }

        activeBlockPackageName = blockedPackageName
        lastBlockLaunchTimeMs = now

        Log.d(TAG, "ACTION open BlockActivity package=$blockedPackageName")

        startActivity(
            BlockActivity.createIntent(
                context = applicationContext,
                blockedPackageName = blockedPackageName
            )
        )
    }

    private fun isLaunchableApp(packageName: String): Boolean {
        return try {
            packageManager.getLaunchIntentForPackage(packageName) != null
        } catch (_: Exception) {
            false
        }
    }

    private fun eventTypeName(eventType: Int): String {
        return when (eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "WINDOW_STATE_CHANGED"
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> "WINDOWS_CHANGED"
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "WINDOW_CONTENT_CHANGED"
            AccessibilityEvent.TYPE_VIEW_CLICKED -> "VIEW_CLICKED"
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> "VIEW_FOCUSED"
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> "VIEW_SCROLLED"
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> "NOTIFICATION_STATE_CHANGED"
            -1 -> "NULL_EVENT"
            else -> "OTHER_$eventType"
        }
    }

    companion object {
        private const val TAG = "DopamineWarBlocker"
        private const val BLOCK_ACTIVITY_LAUNCH_COOLDOWN_MS = 1200L
    }
}