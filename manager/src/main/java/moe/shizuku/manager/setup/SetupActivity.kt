package moe.shizuku.manager.setup

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.adb.AdbPairingTutorialActivity
import moe.shizuku.manager.app.AppBarActivity
import moe.shizuku.manager.databinding.HomeItemContainerBinding
import moe.shizuku.manager.databinding.SetupActivityBinding
import moe.shizuku.manager.databinding.SetupStepBinding
import moe.shizuku.manager.home.AdbPairDialogFragment
import moe.shizuku.manager.home.StartWirelessAdbViewHolder
import moe.shizuku.manager.home.showAccessibilityDialog
import moe.shizuku.manager.starter.Starter
import moe.shizuku.manager.starter.StarterActivity
import moe.shizuku.manager.utils.EnvironmentUtils
import moe.shizuku.manager.utils.SettingsPage
import moe.shizuku.manager.utils.ShizukuStateMachine

/**
 * Step-by-step setup for non-technical users, picked by what the phone can do: root (one tap),
 * Android 11+ wireless debugging (three one-time steps, then automatic), or a computer (Android 8-10).
 * Every step is re-checked when the user comes back from Settings, so finished steps tick themselves.
 */
class SetupActivity : AppBarActivity() {

    private lateinit var binding: SetupActivityBinding
    private var rooted = false

    private val stateListener: (ShizukuStateMachine.State) -> Unit = { runOnUiThread { render() } }

    private class Step(
        val title: Int,
        val body: CharSequence,
        val done: Boolean,
        val button: Int? = null,
        val buttonIcon: Int = R.drawable.ic_outline_open_in_new_24,
        val action: (() -> Unit)? = null,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SetupActivityBinding.inflate(layoutInflater, rootView, true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        rooted = EnvironmentUtils.isRooted()
        ShizukuStateMachine.addListener(stateListener)
    }

    override fun onResume() {
        super.onResume()
        ShizukuStateMachine.update()
        render()
    }

    override fun onDestroy() {
        ShizukuStateMachine.removeListener(stateListener)
        super.onDestroy()
    }

    private fun render() {
        val running = ShizukuStateMachine.isRunning()
        val steps = when {
            rooted -> rootSteps(running)
            SetupState.supportsWirelessDebugging -> wirelessSteps(running)
            else -> computerSteps(running)
        }
        binding.intro.text = when {
            rooted -> getString(R.string.setup_intro_root)
            SetupState.supportsWirelessDebugging -> getString(R.string.setup_intro_wireless)
            else -> getString(R.string.setup_intro_computer, Build.VERSION.RELEASE)
        }

        binding.steps.removeAllViews()
        steps.forEach { addStep(it) }
        if (running) addStep(doneStep())
    }

    private fun wirelessSteps(running: Boolean) = listOf(
        developerOptionsStep(running),
        Step(
            title = R.string.setup_step_wadb_title,
            body = getString(R.string.setup_step_wadb_body),
            done = running || (SetupState.usbDebuggingOn() && SetupState.wirelessDebuggingOn(this)),
            button = R.string.setup_open_developer_options,
            action = {
                val hint = SetupState.developerOptionsOn(this) // otherwise the page explains itself
                SettingsPage.Developer.HighlightWirelessDebugging.launch(this)
                if (hint) showHint(R.string.setup_hint_wireless_debugging)
            },
        ),
        Step(
            title = R.string.setup_step_pair_title,
            body = getString(R.string.setup_step_pair_body),
            done = running || SetupState.adbPaired,
            button = R.string.setup_step_pair_button,
            buttonIcon = R.drawable.ic_baseline_link_24,
            action = ::startPairing,
        ),
        Step(
            title = R.string.setup_step_start_title,
            body = getString(R.string.setup_step_start_body),
            done = running,
            button = R.string.setup_step_start_button,
            buttonIcon = R.drawable.ic_server_start_24dp,
            action = { StartWirelessAdbViewHolder.start(this, lifecycleScope) },
        ),
    )

    private fun computerSteps(running: Boolean) = listOf(
        developerOptionsStep(running),
        Step(
            title = R.string.setup_step_usb_title,
            body = getString(R.string.setup_step_usb_body),
            done = running || SetupState.usbDebuggingOn(),
            button = R.string.setup_open_developer_options,
            action = { SettingsPage.Developer.HighlightUsbDebugging.launch(this) },
        ),
        Step(
            title = R.string.setup_step_computer_title,
            body = getString(R.string.setup_step_computer_body, Starter.adbCommand),
            done = running,
            button = R.string.setup_step_computer_button,
            buttonIcon = R.drawable.ic_baseline_link_24,
            action = ::copyCommand,
        ),
    )

    private fun rootSteps(running: Boolean) = listOf(
        Step(
            title = R.string.setup_step_root_title,
            body = getString(R.string.setup_step_root_body),
            done = running,
            button = R.string.setup_step_start_button,
            buttonIcon = R.drawable.ic_server_start_24dp,
            action = {
                startActivity(Intent(this, StarterActivity::class.java).putExtra(StarterActivity.EXTRA_IS_ROOT, true))
            },
        ),
    )

    private fun developerOptionsStep(running: Boolean) = Step(
        title = R.string.setup_step_dev_title,
        body = getString(R.string.setup_step_dev_body),
        done = running || SetupState.developerOptionsOn(this),
        button = R.string.setup_step_dev_button,
        action = {
            // Settings highlights the row on stock Android; OEM "About phone" pages ignore the key.
            launchSettings(Settings.ACTION_DEVICE_INFO_SETTINGS, highlight = "build_number")
            showHint(R.string.setup_hint_build_number)
        },
    )

    private fun doneStep(): Step {
        val autoStart = ShizukuSettings.getStartOnBoot(this)
        val client = SetupState.clientLaunchIntent(this)
        return Step(
            title = R.string.setup_done_title,
            body = getString(if (autoStart) R.string.setup_done_body_auto else R.string.setup_done_body_manual),
            done = true,
            button = if (client != null) R.string.setup_done_button else null,
            action = client?.let { intent -> { startActivity(intent); finish() } },
        )
    }

    private fun addStep(step: Step) {
        val outer = HomeItemContainerBinding.inflate(layoutInflater, binding.steps, false)
        val inner = SetupStepBinding.inflate(layoutInflater, outer.root, true)
        outer.root.isClickable = false
        outer.root.isCheckable = false
        inner.icon.setImageResource(if (step.done) R.drawable.ic_check_circle_24 else R.drawable.ic_radio_button_unchecked_24)
        inner.icon.contentDescription = if (step.done) getString(R.string.setup_step_done) else null
        inner.title.setText(step.title)
        inner.summary.text = step.body
        // A finished step shrinks to its title and tick, so the next step (or the final "all set"
        // card, which keeps its text and its "Open WhtsAgent Phone" button) is in view.
        val isFinal = step.title == R.string.setup_done_title
        inner.summary.isVisible = !step.done || isFinal
        val showButton = step.button != null && step.action != null && (!step.done || isFinal)
        inner.button1.isVisible = showButton
        if (showButton) {
            inner.button1.setText(step.button)
            inner.button1.setIconResource(step.buttonIcon)
            inner.button1.setOnClickListener { step.action.invoke() }
        }
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.bottomMargin = resources.getDimensionPixelSize(R.dimen.home_icon_padding) / 2
        binding.steps.addView(outer.root, params)
    }

    private fun startPairing() {
        val displayId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display?.displayId ?: 0 else 0
        when {
            EnvironmentUtils.isTelevision() -> showAccessibilityDialog()
            // Same rule as the home screen's Pairing button: a dialog where a notification is awkward.
            displayId > 0 || ShizukuSettings.getLegacyPairing() -> AdbPairDialogFragment().show(supportFragmentManager)
            // The tutorial starts pairing (asking for notifications/permissions first if needed),
            // then goes straight on to Wireless debugging with a hint.
            else -> startActivity(
                Intent(this, AdbPairingTutorialActivity::class.java)
                    .putExtra(AdbPairingTutorialActivity.EXTRA_OPEN_SETTINGS, true)
            )
        }
    }

    private fun copyCommand() {
        getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText(getString(R.string.setup_step_computer_title), Starter.adbCommand))
        Toast.makeText(this, R.string.setup_toast_copied, Toast.LENGTH_SHORT).show()
    }

    /** Tells the user what to tap on the Settings page that is opening. */
    private fun showHint(text: Int) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }

    private fun launchSettings(action: String, highlight: String? = null) {
        try {
            val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (highlight != null) intent.putExtra(":settings:fragment_args_key", highlight)
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
