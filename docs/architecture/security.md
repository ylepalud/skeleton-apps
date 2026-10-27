# Security

The API is meant to be public. Security therefore means exposing everything properly rather than exposing nothing.

## Identity

- **An OpenID Connect provider issues the tokens.** Locally it is `mock-oauth2-server`; in production a real provider (Zitadel is planned). Switching only changes configuration: see [Switch the identity provider](../contributor/how-to/switch-identity-provider.md).
- **The web app signs in with Authorization Code and PKCE** (angular-auth-oidc-client), and renews with refresh tokens rather than a hidden iframe, so the content security policy never allows framing the provider. Its interceptor attaches the token to `/api/` requests only.
- **The API is a pure resource server.** It checks the token's signature, issuer and audience (`api`) on every request and keeps no session, so there is no cookie and nothing for CSRF to abuse. A backend-for-frontend is a possible evolution (issue #20).

## Authorization

- **No anonymous route.** Every application route requires a token; only Actuator, on the management port, is open, so that probes work.
- **A resource belongs to the token's subject.** `CurrentOwner` is the only way an owner enters a use case, and every repository read is scoped to the owner. Someone else's note answers 404, exactly like a missing one, so a caller cannot probe which notes exist. `NotesEndToEndTest` proves it through the whole chain with real tokens.

## Surface

| Control | Where |
| --- | --- |
| Same origin for the app and the API, no CORS configuration | nginx proxies `/api/` |
| Content security policy without `unsafe-inline` or `unsafe-eval`, `frame-ancestors 'none'` | nginx template |
| Request bodies limited to 64 KiB, before authentication | `RequestSizeLimitFilter` (413) and nginx `client_max_body_size` |
| Input validated twice: shape by the contract, meaning by the domain | generated interfaces, value objects |
| Errors never leak internals | `ProblemDetailsExceptionHandler` |
| Actuator on port 8081 only: `health`, `info`, `prometheus` | `application.yml`, `ActuatorTest` |
| Images run as non-root, with no shell for the API | distroless `nonroot`, `nginx-unprivileged` |

A chunked body without `Content-Length` escapes the API's own size check; nginx bounds it for the web app, and the ingress will have to bound it for direct API calls (issue #17).

## Supply chain

Dependencies, images and the build itself are part of the attack surface. See [Supply chain](supply-chain.md).
