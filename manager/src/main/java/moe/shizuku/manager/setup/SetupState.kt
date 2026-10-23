package moe.shizuku.manager.setup

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import moe.shizuku.manager.ShizukuSettings
import rikka.shizuku.Shizuku

/** WhtsAgent's guided setup: what is already done on this phone, and what to switch on automatically. */
object SetupState {

    private const val KEY_AUTO_CONFIGURED = "whtsagent_auto_configured"

    private val prefs get() = ShizukuSettings.getPreferences()

    /** The helper has run at least once on this install. */
    val autoConfigured: Boolean get() = prefs.getBoolean(KEY_AUTO_CONFIGURED, false)

    /**
     * The first time the helper is connected to a running server, turn on what keeps it running:
     * Watchdog, and Start on boot where that can work (Android 11+ wireless debugging, or root).
     * Once per install, so a later choice in Settings sticks.
     */
    fun onServerRunning(context: Context) {
        if (autoConfigured) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || Shizuku.getUid() == 0) {
            ShizukuSettings.setStartOnBoot(context, true)
        }
        ShizukuSettings.setWatchdog(context, true)
        prefs.edit { putBoolean(KEY_AUTO_CONFIGURED, true) }
    }
}
