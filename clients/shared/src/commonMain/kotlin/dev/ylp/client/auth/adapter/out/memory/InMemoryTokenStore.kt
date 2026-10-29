package dev.ylp.client.auth.adapter.out.memory

import dev.ylp.client.auth.domain.TokenStore
import dev.ylp.client.auth.domain.Tokens

/** Keeps the tokens for the life of the process only: tests, and apps that sign in at each launch. */
class InMemoryTokenStore : TokenStore {
    private var tokens: Tokens? = null

    override suspend fun load(): Tokens? = tokens

    override suspend fun save(tokens: Tokens) {
        this.tokens = tokens
    }

    override suspend fun clear() {
        tokens = null
    }
}
