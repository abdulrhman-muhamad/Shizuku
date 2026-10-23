package moe.shizuku.manager.setup

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.content.edit
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.utils.EnvironmentUtils
import rikka.shizuku.Shizuku

/** WhtsAgent's guided setup: what is already done on this phone, and what to switch on automatically. */
object SetupState {

    private const val KEY_AUTO_CONFIGURED = "whtsagent_auto_configured"
    private const val KEY_ADB_PAIRED = "whtsagent_adb_paired"

    private val prefs get() = ShizukuSettings.getPreferences()

    /** Set by AdbPairingService when wireless-debugging pairing succeeds. */
    var adbPaired: Boolean
        get() = prefs.getBoolean(KEY_ADB_PAIRED, false)
        set(value) = prefs.edit { putBoolean(KEY_ADB_PAIRED, value) }

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

    /** Same rule the home screen uses to offer "Start via Wireless debugging". */
    val supportsWirelessDebugging: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ||
                EnvironmentUtils.isTelevision() ||
                EnvironmentUtils.getAdbTcpPort() > 0

    // MIUI can report development_settings_enabled=0 while USB debugging is on, so either counts.
    fun developerOptionsOn(context: Context): Boolean =
        Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1 ||
                usbDebuggingOn()

    fun usbDebuggingOn(): Boolean = EnvironmentUtils.isAdbEnabled()

    fun wirelessDebuggingOn(context: Context): Boolean =
        Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0) == 1

    /** Launch intent for the first installed allowed client (WhtsAgent Phone), if any. */
    fun clientLaunchIntent(context: Context): Intent? =
        BuildConfig.ALLOWED_CLIENT_PACKAGES.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            .firstNotNullOfOrNull { context.packageManager.getLaunchIntentForPackage(it) }
}
