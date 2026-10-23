# Switch the identity provider

Locally, `mock-oauth2-server` plays the provider. Any OpenID Connect provider works the same way; moving to it is configuration only.

## At the provider

1. Create an **API** (or resource) whose tokens carry the audience `api` in `aud`.
2. Create a **web application client** for the web app: Authorization Code with PKCE, no client secret, refresh tokens enabled, redirect URI `https://<your host>/callback`, post-logout redirect URI `https://<your host>`.
3. Make sure access tokens are JWTs signed with an asymmetric algorithm (ES256, or RSA of at least 3072 bits).

## In the API

| Variable | Value |
| --- | --- |
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` | the provider's issuer URL |

The audience `api` is fixed in `application.yml`; change it there if the provider uses another one. Do not set `..._JWK_SET_URI`: the keys are discovered from the issuer.

## In the web app image

| Variable | Value |
| --- | --- |
| `OIDC_AUTHORITY` | the issuer URL |
| `OIDC_CLIENT_ID` | the web application client's id |
| `OIDC_ORIGIN` | the issuer's origin (scheme and host), allowed by the content security policy |

## Check

Sign in through the web app, create a note, then call the API with a token for another user and check that the note answers 404.
