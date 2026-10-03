package com.topdawg.focusmaxxing.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.topdawg.focusmaxxing.BuildConfig
import com.topdawg.focusmaxxing.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(viewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var create by remember { mutableStateOf(false) }
    val state by viewModel.uiState.collectAsState(); val context = LocalContext.current; val scope = rememberCoroutineScope()
    Scaffold { padding -> Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Focusmaxxing", style = MaterialTheme.typography.headlineMedium)
        Text("Sign in to lock in with your friends.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !state.loading)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), enabled = !state.loading)
        Spacer(Modifier.height(12.dp))
        Button(onClick = { if (create) viewModel.register(email, password) else viewModel.signIn(email, password) }, modifier = Modifier.fillMaxWidth(), enabled = !state.loading) { Text(if (create) "Create account" else "Sign in") }
        TextButton(onClick = { create = !create; viewModel.clearError() }, modifier = Modifier.fillMaxWidth()) { Text(if (create) "Already have an account? Sign in" else "New here? Create an account") }
        OutlinedButton(onClick = {
            scope.launch {
                try {
                    if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) error("Set FOCUSMAXXING_GOOGLE_WEB_CLIENT_ID in local.properties first.")
                    val option = GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(false).setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID).setAutoSelectEnabled(false).build()
                    val credential = CredentialManager.create(context).getCredential(context, GetCredentialRequest.Builder().addCredentialOption(option).build()).credential
                    val idToken = if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) GoogleIdTokenCredential.createFrom(credential.data).idToken else error("Google sign-in was cancelled or returned an unsupported credential.")
                    viewModel.signInGoogle(idToken)
                } catch (e: Exception) { viewModel.reportError(e.message ?: "Google sign-in failed.") }
            }
        }, modifier = Modifier.fillMaxWidth(), enabled = !state.loading) { Text("Continue with Google") }
        TextButton(onClick = { viewModel.signInGuest() }, modifier = Modifier.fillMaxWidth(), enabled = !state.loading) { Text("Continue as guest") }
        if (state.loading) { Spacer(Modifier.height(12.dp)); LinearProgressIndicator(Modifier.fillMaxWidth()) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
    } }
}

@Composable
fun UsernameScreen(viewModel: AuthViewModel) {
    var username by remember { mutableStateOf("") }; val state by viewModel.uiState.collectAsState()
    Scaffold { padding -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Choose your username", style = MaterialTheme.typography.headlineMedium)
        Text("3–20 lowercase letters, numbers, or underscores.", modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
        OutlinedTextField(username, { username = it.lowercase().filter { ch -> ch in 'a'..'z' || ch in '0'..'9' || ch == '_' }.take(20) }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth(), isError = state.error != null)
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(16.dp)); Button(onClick = { viewModel.saveUsername(username) }, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
        if (state.loading) { Spacer(Modifier.height(12.dp)); LinearProgressIndicator(Modifier.fillMaxWidth()) }
    } }
}

@Composable
fun UpgradeAccountDialog(viewModel: AuthViewModel, onDismiss: () -> Unit) {
    var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Upgrade your account") }, text = {
        Column {
            Text("Link an email and password to keep your guest progress.")
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true)
            OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
            OutlinedButton(onClick = {
                scope.launch {
                    try {
                        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) error("Set FOCUSMAXXING_GOOGLE_WEB_CLIENT_ID in local.properties first.")
                        val option = GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(false).setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID).setAutoSelectEnabled(false).build()
                        val credential = CredentialManager.create(context).getCredential(context, GetCredentialRequest.Builder().addCredentialOption(option).build()).credential
                        val token = if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) GoogleIdTokenCredential.createFrom(credential.data).idToken else error("Google sign-in was cancelled.")
                        viewModel.upgradeGoogle(token)
                    } catch (e: Exception) { viewModel.reportError(e.message ?: "Google account linking failed.") }
                }
            }, enabled = !state.loading && !state.upgradeDone, modifier = Modifier.fillMaxWidth()) { Text("Link Google account") }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.upgradeDone) Text("Account linked.", color = MaterialTheme.colorScheme.primary)
        }
    }, confirmButton = { TextButton(onClick = { viewModel.upgradeEmail(email, password) }, enabled = !state.loading && !state.upgradeDone) { Text("Link account") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}
