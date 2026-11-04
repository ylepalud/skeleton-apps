# Switch the identity provider

Locally, `mock-oauth2-server` plays the provider. Any OpenID Connect provider works the same way; moving to it is configuration only.

## At the provider

1. Create an **API** (or resource) whose tokens carry the audience `api` in `aud`.
1. Create a **web application client** for the web app: Authorization Code with PKCE, no client secret, refresh tokens enabled, redirect URI `https://<your host>/callback`, post-logout redirect URI `https://<your host>`. <!-- client:web -->
1. Create a **native application client** for the Android and iOS apps: Authorization Code with PKCE, no client secret, refresh tokens enabled, redirect URI `dev.ylp.notes://callback`. <!-- client:mobile -->
1. Create a **native application client** for the CLI, with the device authorization grant, or the loopback redirect `http://127.0.0.1:8770/callback`. <!-- client:cli -->
1. Make sure access tokens are JWTs signed with an asymmetric algorithm (ES256, or RSA of at least 3072 bits).

## In the API

| Variable | Value |
| --- | --- |
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` | the provider's issuer URL |

The audience `api` is fixed in `application.yml`; change it there if the provider uses another one. Do not set `..._JWK_SET_URI`: the keys are discovered from the issuer.

<!-- client:web:start -->
## In the web app image

| Variable | Value |
| --- | --- |
| `OIDC_AUTHORITY` | the issuer URL |
| `OIDC_CLIENT_ID` | the web application client's id |
| `OIDC_ORIGIN` | the issuer's origin (scheme and host), allowed by the content security policy |
<!-- client:web:end -->

<!-- client:native:start -->
## In the native clients

The apps take the issuer and their client id at build time (`mobileOidcIssuer`, `mobileOidcClientId`), the CLI from `NOTES_OIDC_ISSUER` and `NOTES_OIDC_CLIENT_ID`; see each client's run page.
<!-- client:native:end -->

## Check

Sign in through a client, create a note, then call the API with a token for another user and check that the note answers 404.
