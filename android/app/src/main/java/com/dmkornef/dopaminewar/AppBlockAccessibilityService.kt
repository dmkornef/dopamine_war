package com.dmkornef.dopaminewar

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class AppBlockAccessibilityService : AccessibilityService() {

    private val blockedPackages = setOf(
        "com.google.android.youtube",
        "com.instagram.android"
    )

    private var lastBlockedPackage: String? = null
    private var lastBlockTimeMs: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return

        Log.d(TAG, "Foreground package: $packageName")

        if (packageName !in blockedPackages) {
            lastBlockedPackage = null
            return
        }

        val now = System.currentTimeMillis()

        if (packageName == lastBlockedPackage && now - lastBlockTimeMs < BLOCK_COOLDOWN_MS) {
            return
        }

        lastBlockedPackage = packageName
        lastBlockTimeMs = now

        Log.d(TAG, "Blocking package: $packageName")
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility service connected")
    }

    companion object {
        private const val TAG = "DopamineWarBlocker"
        private const val BLOCK_COOLDOWN_MS = 1500L
    }
}