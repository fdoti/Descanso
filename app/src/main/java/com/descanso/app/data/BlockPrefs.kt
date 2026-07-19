package com.descanso.app.data

import android.content.Context

/** Estado compartido entre la UI, el servicio de accesibilidad y el temporizador. */
object BlockPrefs {
    private const val PREFS = "descanso_prefs"
    private const val KEY_PKGS = "blocked_packages"
    private const val KEY_END = "end_time"

    private fun prefs(c: Context) =
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun setBlock(c: Context, packages: Set<String>, endTimeMillis: Long) {
        prefs(c).edit()
            .putStringSet(KEY_PKGS, packages)
            .putLong(KEY_END, endTimeMillis)
            .apply()
    }

    fun clear(c: Context) {
        prefs(c).edit().putLong(KEY_END, 0L).apply()
    }

    fun endTime(c: Context): Long = prefs(c).getLong(KEY_END, 0L)

    fun blockedPackages(c: Context): Set<String> =
        prefs(c).getStringSet(KEY_PKGS, emptySet()) ?: emptySet()

    fun isActive(c: Context): Boolean = endTime(c) > System.currentTimeMillis()
}
