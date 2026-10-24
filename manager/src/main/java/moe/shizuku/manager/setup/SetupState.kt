package moe.shizuku.manager.setup

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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

    /**
     * Whether Settings will actually show its Developer options page. Turning them off makes Settings
     * disable its own page component, so the intent then lands on DevelopmentSettingsDisabledActivity,
     * which closes at once. Checking where the intent resolves works on every OEM; writing
     * development_settings_enabled does NOT re-enable that page (and makes Settings think the user is
     * already a developer, so the Build-number taps stop working), so the helper never writes it.
     * adb_enabled is no evidence either: the helper itself turns it on when it can.
     */
    fun developerOptionsOn(context: Context): Boolean {
        val flag = Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
        val target = context.packageManager.resolveActivity(
            Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS), PackageManager.MATCH_DEFAULT_ONLY
        )?.activityInfo?.name
        return flag && target?.endsWith("DevelopmentSettingsDisabledActivity") != true
    }

    /** Wireless debugging only works on a connected Wi-Fi network. */
    fun wifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        return cm.getNetworkCapabilities(cm.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }

    fun usbDebuggingOn(): Boolean = EnvironmentUtils.isAdbEnabled()

    fun wirelessDebuggingOn(context: Context): Boolean =
        Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0) == 1

    /** Launch intent for the first installed allowed client (WhtsAgent Phone), if any. */
    fun clientLaunchIntent(context: Context): Intent? =
        BuildConfig.ALLOWED_CLIENT_PACKAGES.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            .firstNotNullOfOrNull { context.packageManager.getLaunchIntentForPackage(it) }
}
