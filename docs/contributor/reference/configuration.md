# Configuration

Settings that change between environments come from environment variables. Everything else lives in `apps/services/api/src/main/resources/application.yml` and in the web image's nginx template.

## API (`apps/services/api`)

| Variable | Example | Purpose |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/api` | Database |
| `SPRING_DATASOURCE_USERNAME` | `api` | Database user |
| `SPRING_DATASOURCE_PASSWORD` | | Database password, from a secret |
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` | `https://id.example.com` | Token issuer; the signing keys are discovered from it |
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI` | `http://mock-oauth2-server:8080/default/jwks` | Optional: where to fetch the keys when the issuer URL is not reachable (local stack only) |

Fixed in `application.yml`: the token audience `api`, port 8080 for the API and 8081 for Actuator (`health`, `info`, `prometheus`), `ProblemDetail` errors in English, Liquibase migrations, Hibernate in `validate` mode.

Under the `local` profile, which `bootRun` uses, Docker Compose provides the database and the issuer is `http://localhost:8090/default`.

## Web app image (`apps/services/api-app`)

| Variable | Example | Purpose |
| --- | --- | --- |
| `API_UPSTREAM` | `http://api:8080` | Where nginx proxies `/api/` |
| `OIDC_AUTHORITY` | `https://id.example.com` | Identity provider, served to the app in `/config.json` |
| `OIDC_CLIENT_ID` | `api-app` | Client id of the web app at the identity provider |
| `OIDC_ORIGIN` | `https://id.example.com` | Origin of the identity provider, allowed by the content security policy |

The app reads `/config.json` at startup, so one image runs in every environment. The development server serves `apps/services/api-app/public/config.json`, which points to the local mock.
