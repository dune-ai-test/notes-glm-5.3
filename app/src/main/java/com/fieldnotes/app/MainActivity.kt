package com.fieldnotes.app

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fieldnotes.app.data.repo.AppSettings
import com.fieldnotes.app.di.AppContainer
import com.fieldnotes.app.di.LocalAppContainer
import com.fieldnotes.app.navigation.AppRoot
import com.fieldnotes.app.ui.theme.FN
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
                FieldNotesTheme(dark = rememberDarkTheme(settings.appearance)) {
                    LockGate(enabled = settings.biometricLock && container.biometricAvailable) {
                        AppRoot(container)
                    }
                }
            }
        }
    }
}

@Composable
private fun LockGate(enabled: Boolean, content: @Composable () -> Unit) {
    if (!enabled) {
        content()
        return
    }
    val activity = LocalContext.current.findFragmentActivity()
    var unlocked by rememberSaveable { mutableStateOf(false) }

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
        LaunchedEffect(enabled) {
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
