package com.descanso.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.descanso.app.BlockingActivity
import com.descanso.app.data.BlockPrefs

/**
 * Detecta cuándo pasa a primer plano una app bloqueada mientras el temporizador
 * está activo y muestra la pantalla de descanso encima.
 */
class BlockAccessibilityService : AccessibilityService() {

    private var lastBlockedAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (!BlockPrefs.isActive(this)) return
        if (pkg !in BlockPrefs.blockedPackages(this)) return

        val now = System.currentTimeMillis()
        if (now - lastBlockedAt < 800) return
        lastBlockedAt = now

        val intent = Intent(this, BlockingActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
        }
        startActivity(intent)
    }

    override fun onInterrupt() { /* sin acción */ }
}
