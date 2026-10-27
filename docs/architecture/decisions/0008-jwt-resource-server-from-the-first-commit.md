# 0008. JWT resource server from the first commit, mock provider locally

- Status: accepted
- Date: 2026-10-23

## Context

Security added late is security tested late. The production identity provider is not chosen yet.

## Options

- The API is an OAuth2 resource server from day one; locally, `mock-oauth2-server` issues real tokens
- No security until the provider is chosen
- A security profile disabled locally

## Decision

Chosen: The API is an OAuth2 resource server from day one; locally, `mock-oauth2-server` issues real tokens.

## Consequences

- Production security is exercised everywhere, tests included.
- Switching providers is configuration only.
- Running anything locally needs the mock provider container.

More: [Security](../security.md).
