# Contract first

The OpenAPI contract in `apps/contracts/api` is written before the code and is the single description of the API.

## What is generated from it

| Consumer | Generator | Effect |
| --- | --- | --- |
| The API | openapi-generator (`interfaceOnly`), in the `:apps:contracts:api` project | `NotesApi` and the `*Dto` models; `NotesController implements NotesApi`, so the API stops compiling when it drifts from the contract |
| The web app | hey-api (`@hey-api/client-angular`) | A typed client that goes through Angular's `HttpClient` |
| The Bruno collection | `bru import openapi` | One request per operation, with the contract's examples |
| The native clients | openapi-generator (`kotlin`, `multiplatform`), in `:apps:clients:shared` | An internal Ktor client behind the library's HTTP adapter, which stops compiling when it drifts from the contract (see [Native clients](clients.md)) |

## Why

- **One source of truth.** Paths, payloads, constraints and errors are written once; every consumer follows them.
- **Breaking changes are visible.** A change to the contract shows up in review as a contract change, before any code.
- **Validation is declared.** Lengths, bounds and formats in the contract become bean validation annotations on the generated code. The domain still validates its own invariants: the contract checks the shape, the value objects check the meaning.
- **Clients outlive deployments.** The version lives in the path (`/api/v1`): an installed CLI does not update with the server.

## Layout

`openapi.yaml` is the entry point (info, servers, security, paths). Shared building blocks (`Problem`, common responses) live in `common.yaml`; each module has its own file and tag (`notes.yaml`). Today one generation serves the single module; a second module will need one generation per tag, so that each module owns its generated code (issue #14).

## The rule

Change the contract first, then regenerate the collection (`./gradlew :apps:bruno:updateCollection`), then implement. `check` fails when the collection drifts. See [Change the API contract](../contributor/how-to/change-the-api-contract.md).
