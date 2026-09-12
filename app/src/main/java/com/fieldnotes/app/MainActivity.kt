package com.fieldnotes.app

import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fieldnotes.app.data.repo.AppSettings
import com.fieldnotes.app.di.AppContainer
import com.fieldnotes.app.di.LocalAppContainer
import com.fieldnotes.app.navigation.AppRoot
import com.fieldnotes.app.ui.theme.DarkFN
import com.fieldnotes.app.ui.theme.FN
import com.fieldnotes.app.ui.theme.LightFN
import com.fieldnotes.app.ui.theme.FT
import com.fieldnotes.app.ui.theme.FieldNotesTheme
import com.fieldnotes.app.ui.theme.LocalHapticsEnabled
import com.fieldnotes.app.ui.theme.LocalReducedMotion
import com.fieldnotes.app.ui.theme.rememberDarkTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as FieldNotesApp).container
        setContent {
            val settings by container.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            CompositionLocalProvider(
                LocalAppContainer provides container,
                LocalHapticsEnabled provides settings.haptics,
                LocalReducedMotion provides settings.reduceMotion
            ) {
                val dark = rememberDarkTheme(settings.appearance)
                val view = LocalView.current
                if (!view.isInEditMode) {
                    val activity = view.context.findFragmentActivity()
                    if (activity != null) {
                        val window = activity.window
                        // Keep the window backdrop and system bar icons in sync with the
                        // app-selected theme (which can differ from the system setting).
                        SideEffect {
                            window.setBackgroundDrawable(
                                ColorDrawable(if (dark) DarkFN.bg.toArgb() else LightFN.bg.toArgb())
                            )
                            val controller = WindowInsetsControllerCompat(window, view)
                            controller.isAppearanceLightStatusBars = !dark
                            controller.isAppearanceLightNavigationBars = !dark
                        }
                    }
                }
                FieldNotesTheme(dark = dark) {
                    LockGate(
                        enabled = settings.biometricLock && container.biometricAvailable,
                        timeoutMinutes = settings.autoLockMinutes
                    ) {
                        AppRoot(container)
                    }
                }
            }
        }
    }
}

@Composable
private fun LockGate(enabled: Boolean, timeoutMinutes: Int, content: @Composable () -> Unit) {
    if (!enabled) {
        content()
        return
    }
    val activity = LocalContext.current.findFragmentActivity()
    // Plain remember (not saveable): if the system kills the process in the
    // background, the next launch must be locked again.
    var unlocked by remember { mutableStateOf(false) }
    var backgroundedAtMs by remember { mutableStateOf(0L) }
    val lifecycleOwner = LocalLifecycleOwner.current

    if (activity != null) {
        val promptInfo = remember {
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Field Notes")
                .setSubtitle("Your notes are locked")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
        }
        val showPrompt = {
            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        unlocked = true
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                        ) {
                            activity.moveTaskToBack(true)
                        }
                    }
                }
            )
            prompt.authenticate(promptInfo)
        }
        // Re-lock after the app has been backgrounded for longer than the timeout.
        DisposableEffect(lifecycleOwner, timeoutMinutes) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> {
                        backgroundedAtMs = System.currentTimeMillis()
                    }
                    Lifecycle.Event.ON_START -> {
                        if (unlocked && backgroundedAtMs > 0) {
                            val goneMs = System.currentTimeMillis() - backgroundedAtMs
                            if (goneMs >= timeoutMinutes * 60_000L) {
                                unlocked = false
                            }
                        }
                        backgroundedAtMs = 0L
                    }
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        LaunchedEffect(enabled, unlocked) {
            if (!unlocked) showPrompt()
        }
        if (unlocked) {
            content()
        } else {
            LockScreen(onUnlock = showPrompt)
        }
    } else {
        content()
    }
}

@Composable
private fun LockScreen(onUnlock: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(FN.bg)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(shape = CircleShape, color = FN.strong, modifier = Modifier.size(84.dp)) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Fingerprint,
                    contentDescription = null,
                    tint = FN.onStrong,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Field Notes is locked", style = FT.sectionTitle, color = FN.text)
        Spacer(Modifier.height(8.dp))
        Text(
            "Authenticate to open your notes",
            style = FT.bodySmall,
            color = FN.muted
        )
        Spacer(Modifier.height(24.dp))
        Surface(
            shape = RoundedCornerShape(50),
            color = FN.strong,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Text(
                "Unlock",
                style = FT.button,
                color = FN.onStrong,
                modifier = Modifier
                    .clickable(onClick = onUnlock)
                    .padding(horizontal = 32.dp, vertical = 12.dp)
            )
        }
    }
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
