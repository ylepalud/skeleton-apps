package dev.ylp.notes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.lifecycle.viewModelScope
import dev.ylp.client.SharedClient
import dev.ylp.client.notes.domain.DeleteNoteResult
import dev.ylp.client.notes.domain.Note
import dev.ylp.client.notes.domain.NoteConflict
import dev.ylp.client.notes.domain.NoteConflictResult
import dev.ylp.client.notes.domain.NoteDraft
import dev.ylp.client.notes.domain.NoteFailure
import dev.ylp.client.notes.domain.NoteResult
import dev.ylp.client.notes.domain.Resolution
import dev.ylp.client.notes.domain.ResolutionResult
import dev.ylp.client.notes.domain.SyncState
import kotlinx.coroutines.launch

/**
 * Writes a new note when [id] is null, otherwise edits or deletes that note. A change the API
 * refuses because of a change made elsewhere shows as a conflict, resolved with the notes strategy.
 */
class NoteEditorViewModel(
    private val client: SharedClient,
    private val id: String?,
) : ViewModel() {
    val editing = id != null
    var title by mutableStateOf("")
    var content by mutableStateOf("")
    var problem by mutableStateOf<String?>(null)
        private set
    var done by mutableStateOf(false)
        private set
    var signedOut by mutableStateOf(false)
        private set
    var confirmingDelete by mutableStateOf(false)

    /** The conflict of the note, while the user resolves it. */
    var conflict by mutableStateOf<NoteConflict?>(null)
        private set

    init {
        if (id != null) viewModelScope.launch { load(id) }
    }

    private suspend fun load(id: String) {
        when (val result = client.getNote.execute(id)) {
            is NoteResult.Success -> {
                title = result.note.title
                content = result.note.content
                if (result.note.sync == SyncState.CONFLICTING) showConflict(id)
            }

            is NoteResult.Failed -> fail(result.failure)
        }
    }

    fun save() {
        viewModelScope.launch {
            val result = if (id == null) client.createNote.execute(title, content) else client.updateNote.execute(id, title, content)
            when (result) {
                is NoteResult.Success -> if (result.note.sync == SyncState.CONFLICTING) showConflict(result.note.id) else done = true
                is NoteResult.Failed -> fail(result.failure)
            }
        }
    }

    fun delete() {
        val id = id ?: return
        confirmingDelete = false
        viewModelScope.launch {
            when (val result = client.deleteNote.execute(id)) {
                // A deletion of a note changed elsewhere waits for the user.
                DeleteNoteResult.Deleted -> if (!showConflict(id)) done = true

                is DeleteNoteResult.Failed -> fail(result.failure)
            }
        }
    }

    fun resolve(resolution: Resolution) {
        val id = conflict?.mine?.id ?: return
        viewModelScope.launch {
            when (val result = client.resolveConflict.execute(id, resolution)) {
                ResolutionResult.Resolved -> done = true
                is ResolutionResult.Failed -> fail(result.failure)
            }
        }
    }

    /** Shows the conflict of the note, if it has one. */
    private suspend fun showConflict(id: String): Boolean {
        val result = client.getConflict.execute(id)
        conflict = (result as? NoteConflictResult.Found)?.conflict
        return conflict != null
    }

    private fun fail(failure: NoteFailure) {
        if (failure == NoteFailure.SignedOut) signedOut = true else problem = failure.message()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    model: NoteEditorViewModel,
    onDone: () -> Unit,
    onSignedOut: () -> Unit,
) {
    LaunchedEffect(model.done) { if (model.done) onDone() }
    LaunchedEffect(model.signedOut) { if (model.signedOut) onSignedOut() }
    Scaffold(topBar = { TopAppBar(title = { Text(if (model.editing) "Edit note" else "New note") }) }) { padding ->
        val conflict = model.conflict
        if (conflict != null) {
            ConflictPanel(conflict, model.problem, model::resolve, Modifier.padding(padding))
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = model.title,
                onValueChange = { model.title = it.take(NoteDraft.TITLE_MAX_LENGTH) },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = model.content,
                onValueChange = { model.content = it.take(NoteDraft.CONTENT_MAX_LENGTH) },
                label = { Text("Content") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            model.problem?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = model::save) { Text(if (model.editing) "Save" else "Add note") }
                if (model.editing) OutlinedButton(onClick = { model.confirmingDelete = true }) { Text("Delete") }
            }
        }
    }
    if (model.confirmingDelete) {
        AlertDialog(
            onDismissRequest = { model.confirmingDelete = false },
            text = { Text("Delete this note?") },
            confirmButton = { TextButton(onClick = model::delete) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { model.confirmingDelete = false }) { Text("Keep") } },
        )
    }
}

/** Both sides of a conflict, and one button per resolution. */
@Composable
private fun ConflictPanel(
    conflict: NoteConflict,
    problem: String?,
    onResolve: (Resolution) -> Unit,
    modifier: Modifier,
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(conflict.headline(), style = MaterialTheme.typography.titleMedium)
        if (!conflict.mineDeleted) Side("Yours", conflict.mine)
        conflict.theirs?.let { Side("Theirs", it) }
        problem?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Resolution.entries.filter(conflict::allows).forEach { resolution ->
            OutlinedButton(onClick = { onResolve(resolution) }, modifier = Modifier.fillMaxWidth()) { Text(conflict.label(resolution)) }
        }
    }
}

@Composable
private fun Side(
    name: String,
    note: Note,
) {
    Column {
        Text(name, style = MaterialTheme.typography.labelMedium)
        Text(note.title, style = MaterialTheme.typography.bodyLarge)
        if (note.content.isNotEmpty()) Text(note.content)
    }
}
