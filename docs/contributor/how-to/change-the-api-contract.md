# Change the API contract

The contract changes first; everything else follows. Background: [Contract first](../../architecture/contract-first.md).

1. **Edit the contract** in `apps/contracts/api`: the module's file (`notes.yaml`) for its paths and schemas, `common.yaml` for shared building blocks. Give every request body an `example`: the Bruno requests are generated from it, and stay empty without one. Mirror the domain's limits (`maxLength`, `minimum`...) in the schema.
2. **Regenerate the collection** and commit the result:

   ```bash
   ./gradlew :apps:bruno:updateCollection
   ```

3. **Implement the server side.** `./gradlew :apps:services:api:compileJava` regenerates the interfaces and shows what the controller must now implement. Map DTOs to commands and domain types in the module's REST mapper; never let a `*Dto` reach `application` or `domain`.
4. **Update the web app.** The client is regenerated on every build (`npm run generate:api` in `apps/services/api-app` when using the development server); TypeScript then shows what to adapt.
5. **Update the native clients.** The Kotlin client in `:apps:clients:shared` is regenerated on every build; `./gradlew :apps:clients:shared:compileKotlinJvm` shows where `HttpNoteGateway` and the domain no longer match. Mirror a new limit in the module's draft (`NoteDraft`), and keep the public API within [Swift export's limits](../../architecture/clients.md#within-swift-exports-limits).
6. **Test it.** Controller test for statuses and validation, use case and domain tests for the behaviour, and the end-to-end test when ownership or a new route is involved: every new route must prove that someone else's resource answers 404.
7. **Verify:**

   ```bash
   ./gradlew check
   ```

   `check` fails if the collection drifts from the contract, or if the controller no longer matches the generated interface.

A breaking change (removed field, new required field, changed meaning) needs a new major version path rather than an edit of `/api/v1`.
