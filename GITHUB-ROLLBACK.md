# GitHub rollback workflow

This repo stores the source files and packaging scripts for Chroma Cross.

Large generated files are intentionally ignored:

- `node_modules`
- Electron `dist`
- `.zip` archives
- generated `.exe` files
- bundled Node runtimes
- Android build output

Use Git commits and tags for rollback:

```bash
git log --oneline --decorate
git tag
git checkout <commit-or-tag>
```

Recommended release flow:

1. Commit source changes.
2. Tag stable versions, for example `v1.0.0`.
3. Put generated zip/exe packages in GitHub Releases, not in the normal Git repository.

Example:

```bash
git tag v1.0.0
git push origin main --tags
```
