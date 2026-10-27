# 0010. PKCE and refresh tokens in the web app, same origin as the API

- Status: accepted
- Date: 2026-10-23

## Context

The web app signs in against an OpenID Connect provider and calls the API. Silent renewal through a hidden iframe forces the content security policy to allow framing the provider.

## Options

- Authorization Code with PKCE, refresh tokens, nginx serving the app and proxying `/api/` on one origin
- Silent renewal through an iframe
- A backend-for-frontend holding the tokens

## Decision

Chosen: Authorization Code with PKCE, refresh tokens, nginx serving the app and proxying `/api/` on one origin.

## Consequences

- No CORS configuration, no iframe in the content security policy.
- Tokens live in the browser; a backend-for-frontend stays a possible evolution.

More: [Security](../security.md#identity).
