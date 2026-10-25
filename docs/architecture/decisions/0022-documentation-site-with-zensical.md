# 0022. Documentation site built by Zensical, published to GitHub Pages from `main`

- Status: accepted
- Date: 2026-10-25

## Context

The Markdown in `docs/` must stay readable on GitHub, and also be published as a site with navigation, search and an API reference. Material for MkDocs, the usual choice, is in maintenance and reaches its end of life in 2027; its successor, Zensical, is still at version 0.0.x.

## Options

- Zensical, reading `docs/` and a minimal `mkdocs.yml`, pinned by image digest
- Material for MkDocs, then a migration before its end of life
- Starlight or Docusaurus, which want the pages in their own folder layout and front matter

## Decision

Chosen: Zensical, reading `docs/` and a minimal `mkdocs.yml`, pinned by image digest. The configuration uses nothing Zensical cannot read (no MkDocs plugin, no hook), so falling back to Material for MkDocs means changing the image. Every tool (Zensical, Redocly for the API reference, markdownlint) runs in a pinned container through Gradle tasks outside `check`; the `docs` job of CI fails on a lint error, an em dash or a broken link or anchor. The site is published from `main`, so it describes the latest release, and only when the repository opts in with `DOCS_PAGES`.

## Consequences

- The Markdown stays the source and reads the same on GitHub; no page is written twice.
- A pre-1.0 tool can change between versions: Renovate proposes each version, and the strict build in the pull request shows what broke.
- Docker is the only prerequisite for the documentation, as for the local stack.
- A generated project gets the site without Pages: it publishes only once Pages is enabled and the variable set.
- The API reference is a standalone page, without the site's navigation.

More: [Documentation site](../../operations/ci-cd.md#documentation-site).
