# Local stack

One `compose.yaml` at the root, compose project `skeleton-apps`. Services without a profile start for both `bootRun` and the full stack; the `app` profile adds the application images.

| Service | Profile | Host port | Role |
| --- | --- | --- | --- |
| `postgres` | default | random | PostgreSQL 18, database `api`, user `api`, password `local-only` (local only), volume `postgres-data` |
| `mock-oauth2-server` | default | 8090 | OpenID Connect provider, issuer `http://localhost:8090/default` |
| `api` | `app` | 8091 (management) | The API image; its HTTP port is reached through `web` only |
| `web` | `app` | 8088 | The web app image; `/api/` is proxied to `api:8080` |

## URLs

| URL | What |
| --- | --- |
| <http://localhost:8088> | The web app, and the API under `/api/v1` |
| <http://localhost:8090/default/.well-known/openid-configuration> | The identity provider's discovery document |
| <http://localhost:8091/actuator/health/readiness> | Readiness of the API container |
| <http://localhost:8080/api/v1> | The API in `bootRun` |
| <http://localhost:8081/actuator> | Actuator of the API in `bootRun` |
| <http://localhost:4200> | The web app's development server |
| `http://localhost:8088/api/v1` on an emulator | The API for the Android app, after `adb reverse tcp:8088 tcp:8088` and `adb reverse tcp:8090 tcp:8090` |

## Test accounts

The mock identity provider accepts any user:

- **In the browser**, type any username on its sign-in page; it becomes the token's subject, hence the owner of the notes you create.
- **From the command line**, the client credentials grant turns the client id into the subject:

```bash
curl -s -X POST localhost:8090/default/token \
  -d grant_type=client_credentials -d client_id=alice -d client_secret=x
```

Every token is addressed to the `api` audience.

## Tokens inside the stack

Browsers get their tokens from `localhost:8090`, so the issuer is `http://localhost:8090/default`, an address the `api` container cannot reach. The container therefore checks that issuer but fetches the signing keys from `http://mock-oauth2-server:8080/default/jwks`. This only exists locally; a real provider is reachable at its issuer URL.

The Android app gets its tokens the same way: `adb reverse` makes the emulator's `localhost:8090` the machine's, so its tokens carry the same issuer. Through `10.0.2.2`, the emulator's usual alias for the machine, the issuer would differ and the API would answer 401.
