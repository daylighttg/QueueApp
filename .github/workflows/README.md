# GitHub Actions Workflows

This directory contains automated workflows for building and publishing QueueApp.

## Available Workflows

### 1. **build.yml** - Build & Test
**Triggers:** Push to `main`, `develop`, or `feature/**` branches; Pull requests to `main` or `develop`

Automatically builds a debug APK and uploads it as an artifact for testing.

- ✅ Builds debug APK
- ✅ Runs on every push to main/develop
- ✅ Validates pull requests
- ✅ Uploads APK artifacts (7-day retention)

### 2. **release.yml** - Build & Publish Release
**Triggers:** Git tags matching `v*` (e.g., `v1.0.0`); Manual trigger via GitHub Actions tab

Creates signed release APKs and publishes them to GitHub Releases.

- ✅ Builds signed release APK
- ✅ Creates GitHub Release with auto-generated notes
- ✅ Attaches APK to release
- ✅ Supports custom keystores via GitHub Secrets

## Setup Instructions

### Configure Keystore for Release Builds

#### Option 1: Use Custom Keystore (Recommended)

1. Encode your keystore to base64:
   - **On Windows (PowerShell):**
     ```powershell
     [Convert]::ToBase64String([IO.File]::ReadAllBytes("path\to\your\release.keystore"))
     ```
   - **On macOS/Linux (Bash):**
     ```bash
     base64 -i /path/to/your/release.keystore | tr -d '\n'
     ```

2. Add GitHub Secrets to your repository:
   - Go to **Settings** → **Secrets and variables** → **Actions**
   - Create the following secrets:
     - `KEYSTORE_BASE64` - Base64-encoded keystore file
     - `KEYSTORE_PASSWORD` - Keystore password
     - `KEY_ALIAS` - Key alias in the keystore
     - `KEY_PASSWORD` - Key password

#### Option 2: Auto-Generate Keystore (for Testing)
If GitHub Secrets are not configured, the workflow automatically generates a temporary keystore for testing purposes.

## Creating a Release

1. Create a git tag:
   ```bash
   git tag -a v1.0.0 -m "Release version 1.0.0"
   git push origin v1.0.0
   ```

2. The GitHub Actions workflow will automatically:
   - Build the signed release APK
   - Create a GitHub Release
   - Attach the APK to the release

## Artifact Retention

- **Debug APKs** (build.yml): 7 days
- **Release APKs** (release.yml): Available in GitHub Release permanently

## Troubleshooting

### Keystore Issues
- Ensure `KEYSTORE_BASE64` secret is properly encoded without newlines
- Verify keystore password and key alias are correct

### Build Failures
- Check that JDK 17 is compatible with your Android Gradle Plugin
- Ensure `gradlew` has execute permissions (automatically set by workflow)

### Release Not Creating
- Ensure tag matches the pattern `v*` (e.g., `v1.0.0`)
- Check that GitHub Token has write permissions (configured in release.yml)

