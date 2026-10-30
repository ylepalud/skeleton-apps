package dev.ylp.notes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ylp.client.SharedClient
import dev.ylp.client.auth.application.Session
import dev.ylp.client.auth.application.SignInResult
import kotlinx.coroutines.launch

class SignInViewModel(
    private val client: SharedClient,
) : ViewModel() {
    var signedIn by mutableStateOf(false)
        private set
    var busy by mutableStateOf(true)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            signedIn = client.currentSession.execute() == Session.SignedIn
            busy = false
        }
    }

    fun signIn() {
        busy = true
        message = null
        viewModelScope.launch {
            when (val result = client.signIn.execute()) {
                SignInResult.SignedIn -> signedIn = true
                SignInResult.Cancelled -> message = "Sign-in cancelled."
                is SignInResult.Failed -> message = "Sign-in failed: ${result.message}"
            }
            busy = false
        }
    }
}

@Composable
fun SignInScreen(
    model: SignInViewModel,
    onSignedIn: () -> Unit,
) {
    LaunchedEffect(model.signedIn) { if (model.signedIn) onSignedIn() }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Notes", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = model::signIn, enabled = !model.busy) { Text("Sign in") }
        model.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
