package dev.ylp.notes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import dev.ylp.client.SharedClient
import dev.ylp.client.notes.domain.Note
import dev.ylp.client.notes.domain.NoteFailure
import dev.ylp.client.notes.domain.NotePageResult
import kotlinx.coroutines.launch

class NotesViewModel(
    private val client: SharedClient,
) : ViewModel() {
    var notes by mutableStateOf(emptyList<Note>())
        private set
    var totalNotes by mutableStateOf(0L)
        private set
    var hasNext by mutableStateOf(false)
        private set
    var loading by mutableStateOf(false)
        private set
    var problem by mutableStateOf<String?>(null)
        private set
    var signedOut by mutableStateOf(false)
        private set
    var offline by mutableStateOf(false)
        private set

    /** The changes a sign-out would lose, while the user confirms it. */
    var confirmingSignOut by mutableStateOf<Long?>(null)
    private var page = 0

    /** Reloads from the first page, as when coming back from the editor. */
    fun refresh() = load(0)

    fun loadMore() = load(page + 1)

    /** Signs out at once, or first asks when changes not sent yet would be lost. */
    fun signOut() {
        viewModelScope.launch {
            val pending = client.countPendingChanges.execute()
            if (pending > 0) confirmingSignOut = pending else signOutAnyway()
        }
    }

    fun signOutAnyway() {
        confirmingSignOut = null
        viewModelScope.launch {
            client.signOut.execute()
            signedOut = true
        }
    }

    private fun load(page: Int) {
        loading = true
        viewModelScope.launch {
            when (val result = client.listNotes.execute(page = page)) {
                is NotePageResult.Success -> {
                    this@NotesViewModel.page = page
                    notes = if (page == 0) result.page.notes else notes + result.page.notes
                    totalNotes = result.page.totalNotes
                    hasNext = result.page.hasNext
                    offline = result.page.offline
                    problem = null
                }

                is NotePageResult.Failed ->
                    if (result.failure == NoteFailure.SignedOut) signedOut = true else problem = result.failure.message()
            }
            loading = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    model: NotesViewModel,
    onOpen: (String) -> Unit,
    onNew: () -> Unit,
    onSignedOut: () -> Unit,
) {
    LifecycleResumeEffect(Unit) {
        model.refresh()
        onPauseOrDispose {}
    }
    LaunchedEffect(model.signedOut) { if (model.signedOut) onSignedOut() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your notes (${model.totalNotes})") },
                actions = { TextButton(onClick = model::signOut) { Text("Sign out") } },
            )
        },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = onNew) { Text("New note") } },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            model.problem?.let { problem ->
                Text(problem, color = MaterialTheme.colorScheme.error)
                Button(onClick = model::refresh) { Text("Retry") }
            }
            if (model.offline) {
                Text(OFFLINE)
                Button(onClick = model::refresh) { Text("Retry") }
            }
            if (!model.loading && model.problem == null && model.notes.isEmpty()) Text("No notes yet.")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(model.notes, key = { it.id }) { note ->
                    Card(Modifier.fillMaxWidth().clickable { onOpen(note.id) }) {
                        Column(Modifier.padding(16.dp)) {
                            Text(note.title, style = MaterialTheme.typography.titleMedium)
                            if (note.content.isNotEmpty()) Text(note.content, maxLines = 3)
                            note.sync.label()?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary) }
                        }
                    }
                }
                if (model.hasNext) {
                    item { TextButton(onClick = model::loadMore, enabled = !model.loading) { Text("Load more") } }
                }
            }
        }
    }
    model.confirmingSignOut?.let { pending ->
        AlertDialog(
            onDismissRequest = { model.confirmingSignOut = null },
            text = { Text("Changes not synced yet: $pending. Signing out loses them.") },
            confirmButton = { TextButton(onClick = model::signOutAnyway) { Text("Sign out anyway") } },
            dismissButton = { TextButton(onClick = { model.confirmingSignOut = null }) { Text("Stay") } },
        )
    }
}
