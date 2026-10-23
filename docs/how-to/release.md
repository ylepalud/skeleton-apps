# Release

`develop` is deployed continuously; a release marks a version on `main` and tags the images already published. Background: [Supply chain](../explanation/supply-chain.md).

1. **Bring `main` up to date** with a pull request from `develop` to `main`, merged with a **merge commit** (the `main` ruleset only allows that method):

   ```bash
   gh pr create --base main --head develop --title "chore: release develop to main" --body "..."
   gh pr merge <number> --merge
   ```

2. **release-please opens the release pull request** on `main` (`chore(main): release skeleton-apps X.Y.Z`) with the changelog and the version computed from the Conventional Commits since the last release.
3. **Merge it with the admin bypass.** It was created with the workflow's token, so no CI runs on it and the ruleset blocks it; it only touches `CHANGELOG.md` and the version manifest:

   ```bash
   gh pr merge <number> --merge --admin
   ```

   Issue #12 replaces this step with a GitHub App token.

4. **The release workflow tags the images.** For each image, it walks back from the release commit to the newest commit with a published image and adds the `vX.Y.Z` tag to it, without rebuilding.

While the version is below 1.0.0, a `feat` commit bumps the minor version and a breaking change also bumps the minor version.
