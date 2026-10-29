package dev.ylp.client

import dev.ylp.client.auth.adapter.out.memory.InMemoryTokenStore
import dev.ylp.client.auth.adapter.out.oidc.KtorIdentityProvider
import dev.ylp.client.auth.application.CurrentSession
import dev.ylp.client.auth.application.SignIn
import dev.ylp.client.auth.application.SignOut
import dev.ylp.client.auth.domain.AuthConfig
import dev.ylp.client.auth.domain.AuthenticationException
import dev.ylp.client.auth.domain.AuthorizationBrowser
import dev.ylp.client.auth.domain.IdentityProvider
import dev.ylp.client.auth.domain.TokenStore
import dev.ylp.client.generated.apis.NotesApi
import dev.ylp.client.notes.adapter.out.http.HttpNoteGateway
import dev.ylp.client.notes.application.CreateNote
import dev.ylp.client.notes.application.DeleteNote
import dev.ylp.client.notes.application.GetNote
import dev.ylp.client.notes.application.ListNotes
import dev.ylp.client.notes.application.UpdateNote
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * The entry point of a native client: give it the API address, the OpenID Connect client and
 * the platform adapters, and use its use cases. It builds every other object.
 *
 * [apiBaseUrl] ends with the API prefix, such as `https://example.org/api/v1`.
 */
class SharedClient internal constructor(
    apiBaseUrl: String,
    auth: AuthConfig,
    browser: AuthorizationBrowser,
    tokenStore: TokenStore,
    engine: HttpClientEngine?,
) {
    constructor(
        apiBaseUrl: String,
        auth: AuthConfig,
        browser: AuthorizationBrowser,
        tokenStore: TokenStore = InMemoryTokenStore(),
    ) : this(apiBaseUrl, auth, browser, tokenStore, engine = null)

    private val identityProvider: IdentityProvider = KtorIdentityProvider(auth, httpClient(engine) {})
    private val notes =
        HttpNoteGateway(
            NotesApi(
                apiBaseUrl.trimEnd('/'),
                httpClient(engine) {
                    install(Auth) {
                        bearer {
                            // Read at each request: a sign-in or sign-out takes effect at once.
                            cacheTokens = false
                            sendWithoutRequest { true }
                            loadTokens { tokenStore.load()?.let { BearerTokens(it.accessToken, it.refreshToken) } }
                            refreshTokens {
                                val refreshToken = oldTokens?.refreshToken
                                try {
                                    refreshToken
                                        ?.let { identityProvider.refresh(it) }
                                        ?.also { tokenStore.save(it) }
                                        ?.let { BearerTokens(it.accessToken, it.refreshToken) }
                                } catch (_: AuthenticationException) {
                                    // The session is over: the next call answers SignedOut.
                                    tokenStore.clear()
                                    null
                                }
                            }
                        }
                    }
                },
            ),
        )

    val listNotes = ListNotes(notes)
    val getNote = GetNote(notes)
    val createNote = CreateNote(notes)
    val updateNote = UpdateNote(notes)
    val deleteNote = DeleteNote(notes)
    val signIn = SignIn(identityProvider, browser, tokenStore, auth.redirectUri)
    val signOut = SignOut(tokenStore)
    val currentSession = CurrentSession(tokenStore)

    private companion object {
        val json =
            Json {
                ignoreUnknownKeys = true
            }

        fun httpClient(
            engine: HttpClientEngine?,
            configure: io.ktor.client.HttpClientConfig<*>.() -> Unit,
        ): HttpClient {
            val block: io.ktor.client.HttpClientConfig<*>.() -> Unit = {
                expectSuccess = false
                install(ContentNegotiation) {
                    json(json)
                    json(json, ContentType.Application.ProblemJson)
                }
                configure()
            }
            return if (engine == null) HttpClient(block) else HttpClient(engine, block)
        }
    }
}
