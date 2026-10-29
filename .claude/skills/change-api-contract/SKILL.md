---
name: change-api-contract
description: Change the API contract of skeleton-apps. Use when adding or changing an endpoint, a request or response field, a validation constraint or an error of the API.
---

Follow [docs/contributor/how-to/change-the-api-contract.md](../../../docs/contributor/how-to/change-the-api-contract.md) step by step: contract, collection, server, web app, native clients, tests.

Done when:
- the change is in `contracts/api`, every request body has an `example`;
- `./gradlew :bruno:updateCollection` ran and its output is part of the change;
- `:clients:shared` compiles against the new contract, and its tests cover any new mapping;
- every new route has an end-to-end test proving that someone else's resource answers 404;
- `./gradlew check` passes.
