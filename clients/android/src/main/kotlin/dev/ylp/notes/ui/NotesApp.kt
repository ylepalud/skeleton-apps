package dev.ylp.notes.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.ylp.client.SharedClient

/** The screens and how they lead to each other. A lost session always leads back to sign-in. */
@Composable
fun NotesApp(client: SharedClient) {
    val navigation = rememberNavController()
    val signedOut = {
        navigation.navigate("signIn") { popUpTo(0) }
    }
    MaterialTheme {
        NavHost(navigation, startDestination = "signIn") {
            composable("signIn") {
                SignInScreen(
                    viewModel { SignInViewModel(client) },
                    onSignedIn = { navigation.navigate("notes") { popUpTo("signIn") { inclusive = true } } },
                )
            }
            composable("notes") {
                NotesScreen(
                    viewModel { NotesViewModel(client) },
                    onOpen = { id -> navigation.navigate("editor?id=$id") },
                    onNew = { navigation.navigate("editor") },
                    onSignedOut = signedOut,
                )
            }
            composable(
                "editor?id={id}",
                arguments = listOf(
                    navArgument("id") {
                        type = NavType.StringType
                        nullable = true
                    },
                ),
            ) { entry ->
                val id = entry.arguments?.getString("id")
                NoteEditorScreen(
                    viewModel(key = "editor-$id") { NoteEditorViewModel(client, id) },
                    onDone = { navigation.popBackStack() },
                    onSignedOut = signedOut,
                )
            }
        }
    }
}
