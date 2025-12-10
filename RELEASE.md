# Release Process

This project uses [release-please](https://github.com/googleapis/release-please) for automated release management.

## How does it work?

### Important Note about GitHub Actions

By default, this workflow uses the built-in `GITHUB_TOKEN`. This means that workflows triggered by `release.created` events will run normally. However, if you need other workflows to be triggered by Release Please's pull requests (e.g., CI checks on the Release PR), you may need to configure a Personal Access Token (PAT).

From GitHub's documentation:
> When you use the repository's `GITHUB_TOKEN` to perform tasks, events triggered by the `GITHUB_TOKEN` will not create a new workflow run.

For most use cases, the default `GITHUB_TOKEN` is sufficient. You only need a PAT if you want CI checks to run on Release Please PRs.

### 1. Conventional Commits

Release-please is based on [Conventional Commits](https://www.conventionalcommits.org/). Use the following prefixes when committing:

- `feat:` - New features (bumps MINOR version)
- `fix:` - Bug fixes (bumps PATCH version)
- `docs:` - Documentation changes
- `style:` - Code formatting (no functional changes)
- `refactor:` - Code refactoring
- `perf:` - Performance improvements
- `test:` - Adding or modifying tests
- `build:` - Build system or dependencies
- `ci:` - CI/CD changes
- `chore:` - Other changes

**Breaking Changes:** Add `BREAKING CHANGE:` in the commit body or use `!` after the type (e.g., `feat!:`) to bump the MAJOR version.

Examples:
```bash
git commit -m "feat: Add new API endpoint for train data"
git commit -m "fix: Correct authentication issue"
git commit -m "feat!: API change - removed old format"
```

You can also use scopes for better organization:
```bash
git commit -m "feat(api): Add new train data endpoint"
git commit -m "fix(auth): Correct JWT token validation"
```

For breaking changes with detailed explanation:
```bash
git commit -m "feat!: Change API response format

BREAKING CHANGE: The API now returns data in a new format.
Old clients will need to update their integration."
```

### 2. Release-Please Workflow

When you push commits to the `main` branch:

1. **Release-Please Action** analyzes commits since the last release
2. Creates/updates a **Release PR** with:
   - Automatically generated CHANGELOG
   - Updated version in `pom.xml`
3. When you merge the Release PR:
   - A new **GitHub Release** is created with the changelog
   - A **Git Tag** is created
   - **Build jobs are triggered automatically** for all three platforms

### 3. Multi-Platform Build Process

After the release is created by release-please:

1. **Parallel builds** on three platforms:
   - **Linux** (ubuntu-latest)
   - **Windows** (windows-latest)
   - **macOS** (macos-latest)
2. Each build:
   - Runs `mvn clean package -Prelease`
   - Creates platform-specific jpackage distributions
   - Creates archives (`.tar.gz` for Linux/macOS, `.zip` for Windows)
   - **Uploads directly to the GitHub Release** using `gh` CLI
3. **Final job** combines and uploads:
   - Documentation archive (`r2p-documentation.zip`)
   - Combined test results from all platforms (`r2p-test-results.zip`)

All artifacts are automatically attached to the GitHub Release:
- `r2p-linux.tar.gz` - Linux distribution
- `r2p-windows.zip` - Windows distribution
- `r2p-macos.tar.gz` - macOS distribution
- `r2p-documentation.zip` - API and manual documentation (HTML + PDF)
- `r2p-test-results.zip` - Test reports from all platforms

## Manual Release

The release process is fully automated. You don't need to trigger anything manually.

However, if you want to test the build jobs without creating a release, you can modify the workflow to run on different triggers or create a separate test workflow.

## Getting Started

### Repository Settings

Make sure the following setting is enabled in your repository:
- Go to **Settings** > **Actions** > **General**
- Under "Workflow permissions", ensure "Allow GitHub Actions to create and approve pull requests" is enabled

### First Release

The first time you may need to:

1. Ensure the `.release-please-manifest.json` file reflects your current version
2. Make commits following the Conventional Commits format
3. Push to the `main` branch
4. Release Please will create a Release PR
5. Review and merge the Release PR to create your first release

## Configuration

- **release-please-config.json** - Configuration for release-please (changelog format, commit types)
- **.release-please-manifest.json** - Current version tracking
- **.github/workflows/release-please.yml** - Complete release workflow (release-please + multi-platform builds)

## Available Outputs

The release-please action provides several outputs that are used in the workflow:

| Output           | Description                                                      | Used in Workflow              |
| ---------------- | ---------------------------------------------------------------- | ----------------------------- |
| `release_created` | `true` if a release was created, `false` otherwise               | Controls build job execution  |
| `tag_name`       | The tag name of the release (e.g., `v1.2.3`)                     | Used for `gh release upload`  |
| `version`        | Full semantic version number (e.g., `1.2.3`)                     | Available but not used        |
| `major`          | Major version number                                             | Available but not used        |
| `minor`          | Minor version number                                             | Available but not used        |
| `patch`          | Patch version number                                             | Available but not used        |
| `sha`            | Git SHA that the release was tagged at                           | Available but not used        |
| `html_url`       | URL to the GitHub release                                        | Available but not used        |
| `upload_url`     | Upload URL for release assets                                    | Not needed (using `gh` CLI)   |

These outputs are accessed using: `${{ steps.release.outputs.<output_name> }}`

## Workflow Visualization

```
┌─────────────────────────────────────────────────────────────┐
│ Developer pushes commit(s) to main branch                   │
│ (following Conventional Commits format)                     │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ Release Please analyzes commits                             │
│ - Creates/updates Release PR with:                          │
│   • Updated CHANGELOG.md                                    │
│   • Bumped version in pom.xml                               │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ Developer reviews and merges Release PR                     │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ Release Please creates:                                     │
│ - GitHub Release with changelog                             │
│ - Git tag (e.g., v1.0.0)                                    │
└────────┬─────────────┬─────────────┬────────────────────────┘
         │             │             │
         ▼             ▼             ▼
    ┌────────┐   ┌────────┐   ┌────────┐
    │ Linux  │   │Windows │   │ macOS  │
    │ Build  │   │ Build  │   │ Build  │
    └───┬────┘   └───┬────┘   └───┬────┘
        │            │            │
        │ mvn clean package -Prelease
        │            │            │
        ▼            ▼            ▼
    Upload       Upload       Upload
    .tar.gz      .zip         .tar.gz
         │            │            │
         └────────┬───┴────────────┘
                  ▼
         ┌────────────────────┐
         │  Upload docs +     │
         │  test results      │
         └────────────────────┘
```

## Troubleshooting

### Release PR is not created

- Ensure your commits follow the Conventional Commits format
- Check that you're pushing to the `main` branch
- Verify that the workflow has permissions enabled in repository settings
- Check the Actions tab for any workflow errors

### Build fails on specific platform

- Check the Maven build logs in the Actions tab
- Ensure `mvn clean package -Prelease` works locally on that platform
- Verify that JDK 25 is available and compatible

### Release artifacts are not uploaded

- Ensure the `gh` CLI command has proper permissions
- Check that `GITHUB_TOKEN` has `contents: write` permission
- Verify the release was actually created before uploads are attempted

### CI checks don't run on Release PR

This is expected behavior when using the default `GITHUB_TOKEN`. If you need CI checks on Release PRs, configure a Personal Access Token (PAT) with appropriate permissions.

## Changelog

The changelog is automatically generated from commit messages and is visible in the Release PR and the GitHub Release.

## Technical Details

The workflow uses the GitHub CLI (`gh`) tool to upload release artifacts. The `gh` CLI is pre-installed on all GitHub-hosted runners and provides a simple way to interact with GitHub releases:

```bash
gh release upload <tag_name> <file_path>
```

This approach is recommended by the release-please documentation and eliminates the need for third-party actions.

## Further Information

- [Release Please Documentation](https://github.com/googleapis/release-please)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [GitHub CLI Documentation](https://cli.github.com/manual/)

