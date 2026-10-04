package com.topdawg.focusmaxxing.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.topdawg.focusmaxxing.BuildConfig
import com.topdawg.focusmaxxing.ui.components.ArcadeSprites
import com.topdawg.focusmaxxing.ui.components.PixelButton
import com.topdawg.focusmaxxing.ui.components.PixelButtonKind
import com.topdawg.focusmaxxing.ui.components.PixelCard
import com.topdawg.focusmaxxing.ui.components.PixelDialog
import com.topdawg.focusmaxxing.ui.components.PixelLoadingIndicator
import com.topdawg.focusmaxxing.ui.components.PixelSprite
import com.topdawg.focusmaxxing.ui.components.PixelTextField
import com.topdawg.focusmaxxing.ui.components.safeWindowInsets
import com.topdawg.focusmaxxing.ui.theme.ArcadeColors
import com.topdawg.focusmaxxing.ui.theme.ArcadeDimens
import com.topdawg.focusmaxxing.ui.theme.FocusmaxxingTheme
import com.topdawg.focusmaxxing.ui.theme.PixelHeadingFont
import com.topdawg.focusmaxxing.viewmodels.AuthUiState
import com.topdawg.focusmaxxing.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(viewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var create by remember { mutableStateOf(false) }
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    SignInPanel(
        email = email,
        password = password,
        creating = create,
        state = state,
        onEmailChange = { email = it },
        onPasswordChange = { password = it },
        onSubmit = { if (create) viewModel.register(email, password) else viewModel.signIn(email, password) },
        onToggleMode = { create = !create; viewModel.clearError() },
        onGuest = viewModel::signInGuest,
        onGoogle = {
            scope.launch {
                try {
                    if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) error("Set FOCUSMAXXING_GOOGLE_WEB_CLIENT_ID in local.properties first.")
                    val option = GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(false).setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID).setAutoSelectEnabled(false).build()
                    val credential = CredentialManager.create(context).getCredential(context, GetCredentialRequest.Builder().addCredentialOption(option).build()).credential
                    val idToken = if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) GoogleIdTokenCredential.createFrom(credential.data).idToken else error("Google sign-in was cancelled or returned an unsupported credential.")
                    viewModel.signInGoogle(idToken)
                } catch (e: Exception) { viewModel.reportError(e.message ?: "Google sign-in failed.") }
            }
        }
    )
}

@Composable
private fun SignInPanel(
    email: String,
    password: String,
    creating: Boolean,
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onToggleMode: () -> Unit,
    onGuest: () -> Unit,
    onGoogle: () -> Unit
) {
    Column(
        Modifier.fillMaxSize()
            .background(ArcadeColors.Background)
            .windowInsetsPadding(safeWindowInsets())
            .verticalScroll(rememberScrollState())
            .padding(ArcadeDimens.Space6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PixelSprite(ArcadeSprites.Paw.rows, ArcadeSprites.Paw.palette, ArcadeSprites.Paw.description, Modifier.size(56.dp))
        Spacer(Modifier.height(ArcadeDimens.Space4))
        Text("FOCUSMAXXING", color = ArcadeColors.Accent, fontFamily = PixelHeadingFont, fontSize = 18.sp, lineHeight = 28.sp)
        Text("lock in. level up.", color = ArcadeColors.TextMuted, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(ArcadeDimens.Space6))
        PixelCard(Modifier.fillMaxWidth(), accent = ArcadeColors.Accent) {
            Text(if (creating) "CREATE YOUR ACCOUNT" else "PLAYER LOGIN", color = ArcadeColors.Text, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(ArcadeDimens.Space3))
            PixelTextField(email, onEmailChange, "EMAIL", placeholder = "you@example.com", singleLine = true, enabled = !state.loading)
            Spacer(Modifier.height(ArcadeDimens.Space3))
            PasswordField(password, onPasswordChange, !state.loading)
            state.error?.let { AuthError(it) }
            if (state.loading) {
                Spacer(Modifier.height(ArcadeDimens.Space3))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { PixelLoadingIndicator(contentDescription = "Signing in") }
            }
            Spacer(Modifier.height(ArcadeDimens.Space4))
            PixelButton(if (creating) "CREATE ACCOUNT" else "SIGN IN", onSubmit, Modifier.fillMaxWidth(), enabled = !state.loading)
            Spacer(Modifier.height(ArcadeDimens.Space2))
            PixelButton(if (creating) "BACK TO SIGN IN" else "NEW PLAYER? SIGN UP", onToggleMode, Modifier.fillMaxWidth(), kind = PixelButtonKind.Secondary, enabled = !state.loading)
            Spacer(Modifier.height(ArcadeDimens.Space2))
            PixelButton("CONTINUE WITH GOOGLE", onGoogle, Modifier.fillMaxWidth(), kind = PixelButtonKind.Secondary, enabled = !state.loading)
            Spacer(Modifier.height(ArcadeDimens.Space2))
            PixelButton("PLAY AS GUEST", onGuest, Modifier.fillMaxWidth(), kind = PixelButtonKind.Secondary, enabled = !state.loading, leading = {
                PixelSprite(ArcadeSprites.Paw.rows, ArcadeSprites.Paw.palette, "Guest play", Modifier.size(18.dp))
            })
        }
    }
}

@Composable
private fun PasswordField(value: String, onValueChange: (String) -> Unit, enabled: Boolean) {
    PixelTextField(value, onValueChange, "PASSWORD", singleLine = true, enabled = enabled, visualTransformation = PasswordVisualTransformation())
}

@Composable
private fun AuthError(message: String) {
    Spacer(Modifier.height(ArcadeDimens.Space2))
    PixelCard(Modifier.fillMaxWidth(), accent = ArcadeColors.Danger, containerColor = ArcadeColors.SurfaceHigh) {
        Text(message, color = ArcadeColors.Danger, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun UsernameScreen(viewModel: AuthViewModel) {
    var username by remember { mutableStateOf("") }
    val state by viewModel.uiState.collectAsState()
    UsernamePanel(
        username, state,
        onUsernameChange = { username = it.lowercase().filter { ch -> ch in 'a'..'z' || ch in '0'..'9' || ch == '_' }.take(20) },
        onContinue = { viewModel.saveUsername(username) }
    )
}

@Composable
private fun UsernamePanel(username: String, state: AuthUiState, onUsernameChange: (String) -> Unit, onContinue: () -> Unit) {
    Column(
        Modifier.fillMaxSize()
            .background(ArcadeColors.Background)
            .windowInsetsPadding(safeWindowInsets())
            .verticalScroll(rememberScrollState())
            .padding(ArcadeDimens.Space6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PixelSprite(ArcadeSprites.Paw.rows, ArcadeSprites.Paw.palette, ArcadeSprites.Paw.description, Modifier.size(52.dp))
        Spacer(Modifier.height(ArcadeDimens.Space4))
        Text("CHOOSE YOUR HANDLE", color = ArcadeColors.Accent, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(ArcadeDimens.Space5))
        PixelCard(Modifier.fillMaxWidth()) {
            Text("Make it yours", style = MaterialTheme.typography.titleMedium, color = ArcadeColors.Text)
            Text("3–20 lowercase letters, numbers, or underscores.", style = MaterialTheme.typography.bodyMedium, color = ArcadeColors.TextMuted)
            Spacer(Modifier.height(ArcadeDimens.Space3))
            PixelTextField(username, onUsernameChange, "USERNAME", placeholder = "top_dawg", singleLine = true, errorMessage = state.error, enabled = !state.loading)
            if (state.loading) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { PixelLoadingIndicator(contentDescription = "Saving username") }
            PixelButton("LOCK IT IN", onContinue, Modifier.fillMaxWidth(), enabled = !state.loading)
        }
    }
}

@Composable
fun UpgradeAccountDialog(viewModel: AuthViewModel, onDismiss: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    PixelDialog(
        onDismissRequest = onDismiss,
        title = if (state.upgradeDone) "ACCOUNT LINKED" else "KEEP YOUR PROGRESS",
        confirmButton = {
            PixelButton("LINK ACCOUNT", { viewModel.upgradeEmail(email, password) }, kind = PixelButtonKind.Primary, enabled = !state.loading && !state.upgradeDone)
        },
        dismissButton = { PixelButton("CLOSE", onDismiss, kind = PixelButtonKind.Secondary) }
    ) {
        Text("Link an email and password to keep your guest progress.", color = ArcadeColors.Text, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(ArcadeDimens.Space3))
        PixelTextField(email, { email = it }, "EMAIL", singleLine = true, enabled = !state.loading)
        Spacer(Modifier.height(ArcadeDimens.Space2))
        PixelTextField(password, { password = it }, "PASSWORD", singleLine = true, enabled = !state.loading, visualTransformation = PasswordVisualTransformation())
        Spacer(Modifier.height(ArcadeDimens.Space2))
        PixelButton("LINK GOOGLE", {
            scope.launch {
                try {
                    if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) error("Set FOCUSMAXXING_GOOGLE_WEB_CLIENT_ID in local.properties first.")
                    val option = GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(false).setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID).setAutoSelectEnabled(false).build()
                    val credential = CredentialManager.create(context).getCredential(context, GetCredentialRequest.Builder().addCredentialOption(option).build()).credential
                    val token = if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) GoogleIdTokenCredential.createFrom(credential.data).idToken else error("Google sign-in was cancelled.")
                    viewModel.upgradeGoogle(token)
                } catch (e: Exception) { viewModel.reportError(e.message ?: "Google account linking failed.") }
            }
        }, Modifier.fillMaxWidth(), kind = PixelButtonKind.Secondary, enabled = !state.loading && !state.upgradeDone)
        state.error?.let { AuthError(it) }
        if (state.upgradeDone) Text("Progress secured. You're all set.", color = ArcadeColors.Accent, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10, heightDp = 850)
@Composable
private fun SignInPreview() = FocusmaxxingTheme {
    SignInPanel("player@example.com", "pixelpass", false, AuthUiState(), {}, {}, {}, {}, {}, {})
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10, heightDp = 760)
@Composable
private fun SignInErrorPreview() = FocusmaxxingTheme {
    SignInPanel("bad", "", true, AuthUiState(error = "Enter a valid email address."), {}, {}, {}, {}, {}, {})
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10)
@Composable
private fun UsernamePreview() = FocusmaxxingTheme {
    UsernamePanel("top_dawg", AuthUiState(), {}, {})
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10)
@Composable
private fun UsernameErrorPreview() = FocusmaxxingTheme {
    UsernamePanel("top_dawg", AuthUiState(error = "That username is already taken."), {}, {})
}
