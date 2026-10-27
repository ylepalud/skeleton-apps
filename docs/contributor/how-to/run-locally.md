# Run locally

Three ways, from the most faithful to the fastest to iterate with. All of them use Docker for PostgreSQL and the mock identity provider; see [Local stack](../reference/local-stack.md) for ports and accounts.

## The full stack, as in production

```bash
./gradlew startLocalStack
```

It builds the API image (Jib) and the web app image (nginx), starts everything from `compose.yaml` and waits until the API is ready. Open http://localhost:8088 and sign in with any username.

After a change, run it again: only the containers whose image changed are recreated. `./gradlew stopLocalStack` stops everything and keeps the data; `./gradlew resetDatabase` also deletes the database.

## The API from source

```bash
./gradlew :services:api:bootRun
```

The `local` profile starts PostgreSQL and the identity provider through Docker Compose and keeps them running between restarts. The API listens on http://localhost:8080/api/v1. Get a token and call it:

```bash
TOKEN=$(curl -s -X POST localhost:8090/default/token \
  -d grant_type=client_credentials -d client_id=alice -d client_secret=x | jq -r .access_token)
curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/v1/notes
```

Or use the Bruno collection in `bruno/` with the `BootRun` environment.

Do not run the API in `bootRun` and the full stack at the same time.

## The web app with live reload

With the API in `bootRun`:

```bash
cd services/api-app
npm ci          # once
npm run generate:api
npm start
```

Open http://localhost:4200. The development server proxies `/api` to `localhost:8080` and reads `public/config.json`, which points to the local identity provider.

## Check it works end to end

```bash
E2E_BASE_URL=http://localhost:8088 ./gradlew :services:api-app:npmRunE2e -PrunE2e=true
cd bruno && npm ci && npm run smoke
```
