# Automated Google Play Store Deployment - Setup Guide for New Apps

This guide explains how to set up automated weekly deployments to Google Play Store for new Android apps using the infrastructure already created for ONE Fasting Tracker.

## What's Already Done (Reusable) ✅

The following infrastructure is **already set up** and can be reused for all your Android apps:

### Google Cloud Setup

- ✅ **Project**: `android-play-store-automation` (Project #: 984604330802)
- ✅ **Google Play Developer API**: Enabled
- ✅ **Workload Identity Pool**: `github-actions-pool`
- ✅ **GitHub Provider**: `github-provider` (configured for OIDC authentication)
- ✅ **Service Account**: `github-actions-deploy@android-play-store-automation.iam.gserviceaccount.com`
  - Already has permissions to impersonate from any `charliesbot/*` repository
  - Can be reused across multiple apps

### What This Means

You don't need to touch Google Cloud Console again for new apps! The hard part is done.

---

## Prerequisites

Before starting, make sure you have:

- [ ] App already published on Google Play Store (initial version uploaded manually)
- [ ] App has a release keystore (.jks file)
- [ ] GitHub repository created for the app
- [ ] Firebase project set up (for Crashlytics, if using)

---

## Setup Steps

### Step 1: Configure Version Properties in Gradle

Add version property support to your build files so the workflow can inject version codes dynamically.

**In `app/build.gradle.kts` (before `android {}` block):**

```kotlin
// Support for runtime version override from CI
val defaultVersionCode = java.text.SimpleDateFormat("yyMMdd").format(java.util.Date())
val versionCodeProperty: String = (project.findProperty("versionCode") as String?) ?: defaultVersionCode
val versionNameProperty: String = (project.findProperty("versionName") as String?) ?: "1.$defaultVersionCode-dev"
```

**Update `defaultConfig`:**

```kotlin
defaultConfig {
    applicationId = "com.example.yourapp"
    versionCode = versionCodeProperty.toInt()
    versionName = "$versionNameProperty-mobile"  // Add platform suffix
}
```

**If you have a Wear OS module** (e.g., `onewearos/build.gradle.kts`), repeat the same but with:

```kotlin
defaultConfig {
    applicationId = "com.example.yourapp"  // Same as phone app
    versionCode = versionCodeProperty.toInt() + 1  // +1 to avoid conflicts
    versionName = "$versionNameProperty-wear"      // -wear suffix
}
```

---

### Step 2: Create Release Notes

Create release notes files that will be displayed in Play Store.

**Directory structure:**

```
.github/
  whatsnew/
    en-US.txt
    es-ES.txt
```

**Example `en-US.txt`:**

```
Bug fixes and performance improvements.

Thank you for using [Your App Name]!
```

**Example `es-ES.txt`:**

```
Corrección de errores y mejoras de rendimiento.

¡Gracias por usar [Your App Name]!
```

Add more locales as needed (`fr-FR.txt`, `de-DE.txt`, etc.)

---

### Step 3: Copy GitHub Actions Workflow

**Option 1: Copy from ONE project:**

```bash
cp /path/to/one/.github/workflows/deploy-play-store.yml .github/workflows/
```

**Option 2: Create new file** at `.github/workflows/deploy-play-store.yml`

<details>
<summary>Click to expand full workflow YAML</summary>

```yaml
name: Deploy to Google Play Store

on:
  schedule:
    - cron: '0 10 * * 1'  # Every Monday at 10:00 AM UTC
  workflow_dispatch:
    inputs:
      version_suffix:
        description: 'Version suffix (optional, for same-day deploys)'
        required: false
        default: ''

jobs:
  deploy:
    runs-on: ubuntu-latest
    timeout-minutes: 45
    permissions:
      contents: read
      id-token: write

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'gradle'

      - name: Generate version numbers
        id: version
        run: |
          BASE_VERSION=$(date +'%y%m%d')

          # Auto-add hour suffix for manual runs (for uniqueness)
          if [ "${{ github.event_name }}" = "workflow_dispatch" ] && [ -z "${{ github.event.inputs.version_suffix }}" ]; then
            SUFFIX=$(date +'%H')
          else
            SUFFIX="${{ github.event.inputs.version_suffix }}"
          fi

          VERSION_CODE="${BASE_VERSION}${SUFFIX}"
          VERSION_NAME="1.${BASE_VERSION}${SUFFIX}"

          echo "VERSION_CODE=${VERSION_CODE}" >> $GITHUB_ENV
          echo "VERSION_NAME=${VERSION_NAME}" >> $GITHUB_ENV
          echo "::notice::Generated versionCode: ${VERSION_CODE}, versionName: ${VERSION_NAME}"

      - name: Restore keystore
        env:
          KEYSTORE_BASE64: ${{ secrets.KEYSTORE_FILE_BASE64 }}
        run: |
          echo "$KEYSTORE_BASE64" | base64 --decode > keystore.jks
          chmod 600 keystore.jks

          cat > keystore.properties <<EOF
          keyAlias=${{ secrets.KEYSTORE_KEY_ALIAS }}
          storePassword=${{ secrets.KEYSTORE_STORE_PASSWORD }}
          keyPassword=${{ secrets.KEYSTORE_KEY_PASSWORD }}
          storeFile=../keystore.jks
          EOF

      - name: Authenticate to Google Cloud
        uses: google-github-actions/auth@v2
        with:
          workload_identity_provider: 'projects/984604330802/locations/global/workloadIdentityPools/github-actions-pool/providers/github-provider'
          service_account: 'github-actions-deploy@android-play-store-automation.iam.gserviceaccount.com'
          create_credentials_file: true
          export_environment_variables: true

      - name: Create google-services.json
        env:
          GOOGLE_SERVICES_JSON_BASE64: ${{ secrets.GOOGLE_SERVICES_JSON_BASE64 }}
        run: |
          echo "$GOOGLE_SERVICES_JSON_BASE64" | base64 --decode > app/google-services.json
          echo "$GOOGLE_SERVICES_JSON_BASE64" | base64 --decode > onewearos/google-services.json

      - name: Authenticate to Google Cloud
        id: auth
        uses: google-github-actions/auth@v2
        with:
          workload_identity_provider: 'projects/984604330802/locations/global/workloadIdentityPools/github-actions-pool/providers/github-provider'
          service_account: 'github-actions-deploy@android-play-store-automation.iam.gserviceaccount.com'
          create_credentials_file: true
          export_environment_variables: true

      - name: Make gradlew executable
        run: chmod +x ./gradlew

      - name: Build phone app AAB
        run: |
          ./gradlew :app:bundleRelease \
            -PversionCode=$VERSION_CODE \
            -PversionName=$VERSION_NAME \
            --no-daemon \
            --stacktrace

      - name: Build Wear OS app AAB
        run: |
          ./gradlew :onewearos:bundleRelease \
            -PversionCode=$VERSION_CODE \
            -PversionName=$VERSION_NAME \
            --no-daemon \
            --stacktrace

      - name: Upload phone app to Play Store
        uses: r0adkll/upload-google-play@v1
        with:
          serviceAccountJson: ${{ steps.auth.outputs.credentials_file_path }}
          packageName: com.charliesbot.one
          releaseFiles: app/build/outputs/bundle/release/app-release.aab
          debugSymbols: app/build/outputs/native-debug-symbols/release/native-debug-symbols.zip
          track: production
          status: completed
          whatsNewDirectory: .github/whatsnew

      - name: Upload Wear OS app to Play Store
        uses: r0adkll/upload-google-play@v1
        with:
          serviceAccountJson: ${{ steps.auth.outputs.credentials_file_path }}
          packageName: com.charliesbot.one
          releaseFiles: onewearos/build/outputs/bundle/release/onewearos-release.aab
          debugSymbols: onewearos/build/outputs/native-debug-symbols/release/native-debug-symbols.zip
          track: wear:production
          status: completed
          whatsNewDirectory: .github/whatsnew

      - name: Cleanup sensitive files
        if: always()
        run: |
          rm -f keystore.jks keystore.properties app/google-services.json onewearos/google-services.json
          rm -f gha-creds-*.json

      - name: Upload build artifacts (on failure)
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: build-outputs-${{ env.VERSION_CODE }}
          path: |
            app/build/outputs/
            onewearos/build/outputs/
          retention-days: 7
```

</details>

**Important notes:**
- Update `packageName` to match your app
- For **phone-only apps**, remove the Wear OS build and upload steps
- For **other form factors** (TV, Automotive, XR), use the appropriate track prefix (see Appendix A)

---

### Step 4: Grant Workload Identity Pool Access

Allow your GitHub repository to impersonate the service account.

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select project `android-play-store-automation`
3. Navigate to **IAM & Admin** → **Workload Identity Federation**
4. Click on pool: `github-actions-pool`
5. Click **Grant Access** button
6. Select **"Grant access using service account impersonation"**
7. Fill in:
   - **Service account:** `github-actions-deploy@android-play-store-automation.iam.gserviceaccount.com`
   - **Select principals:** Choose `repository` from dropdown
   - **Repository:** Enter `charliesbot/your-repo-name`
8. Click **Save**

---

### Step 5: Grant Play Console Permissions

Give the service account permission to release your specific app.

1. Go to https://play.google.com/console
2. Click **"Users and permissions"** (left sidebar)
3. Find `github-actions-deploy@android-play-store-automation.iam.gserviceaccount.com`
4. Click on it to edit
5. Under **"App permissions"**:
   - Click **"Add app"**
   - Select your app
   - Check **"Releases"**
6. Click **"Save"**

---

### Step 6: Prepare Secrets

Encode your keystore and Firebase config for GitHub Actions.

#### Keystore

Find your keystore file (usually `~/.android/your-app-keystore.jks`) and encode it:

```bash
base64 -i ~/.android/your-app-keystore.jks | pbcopy  # macOS
base64 -w 0 ~/.android/your-app-keystore.jks         # Linux
```

You'll also need from your `keystore.properties`:
- Key alias (e.g., `key0`, `upload`)
- Keystore password
- Key password

#### google-services.json (Firebase)

Download from Firebase Console and encode:

```bash
base64 app/google-services.json | pbcopy  # macOS
base64 -w 0 app/google-services.json      # Linux
```

---

### Step 7: Add GitHub Secrets

1. Go to your GitHub repo → **Settings** → **Secrets and variables** → **Actions**
2. Click **"New repository secret"** for each:

| Secret Name | Value | Source |
|-------------|-------|--------|
| `KEYSTORE_FILE_BASE64` | Base64 encoded keystore | Step 6 |
| `KEYSTORE_KEY_ALIAS` | Key alias | keystore.properties |
| `KEYSTORE_STORE_PASSWORD` | Keystore password | keystore.properties |
| `KEYSTORE_KEY_PASSWORD` | Key password | keystore.properties |
| `GOOGLE_SERVICES_JSON_BASE64` | Base64 encoded JSON | Step 6 |

---

### Step 8: Declare Foreground Service Permissions (If Applicable)

**Skip this step if your app doesn't use foreground services.**

Check your `AndroidManifest.xml` for:
- `<uses-permission android:name="android.permission.FOREGROUND_SERVICE*" />`
- `<service android:foregroundServiceType="..." />`

If found, declare in Play Console:

1. Go to Play Console → Your App → **Policy** → **App content**
2. Find **"Foreground service permissions"**
3. Click **Manage**
4. Select the types your app uses (HEALTH, DATA_SYNC, etc.)
5. Provide use case descriptions
6. Save

**Important:** Declarations must match your manifest or uploads will fail.

---

### Step 9: Test the Deployment

1. Go to GitHub repo → **Actions** tab
2. Click **"Deploy to Google Play Store"** workflow
3. Click **"Run workflow"**
4. Leave version suffix empty (auto-adds hour suffix)
5. Click **"Run workflow"** button
6. Monitor the build (~15-20 minutes)

**Verify in Play Console:**
1. Go to https://play.google.com/console
2. Select your app
3. Check **Release** → **Production** (and **Wear OS** tab if applicable)
4. You should see a new release with version names ending in `-mobile` and `-wear`

**If you see "Ready for review":** Click "Send for review" manually. After several successful reviews, Google usually restores auto-publishing.

---

## You're Done! 🎉

Your app will now automatically deploy **every Monday at 10:00 AM UTC**.

**For hotfixes:**
- Go to Actions → "Deploy to Google Play Store" → "Run workflow"
- Enter custom version suffix (`1`, `2`, etc.) or leave empty for auto hour suffix

---

## Appendix A: Form Factor Tracks

Since March 2023, Google requires non-phone apps to use dedicated tracks with prefixes.

**Track Format:** `[prefix]:trackName`

**Supported Prefixes:**
- **(none)** - Phone/tablet apps → `track: production`
- `wear:` - Wear OS → `track: wear:production`
- `automotive:` - Android Automotive OS → `track: automotive:production`
- `tv:` - Android TV → `track: tv:production`
- `xr:` - Android XR → `track: xr:production`

**Key Points:**
- Phone apps use NO prefix
- All other form factors REQUIRE the prefix
- Apps with same package name go to separate tracks
- The API routes binaries based on track prefix

---

## Appendix B: Version Strategy

### How Versions Work

**Phone app:**
- versionCode: `251208` (YYMMDD)
- versionName: `1.251208-mobile`

**Wear OS app:**
- versionCode: `251209` (YYMMDD + 1)
- versionName: `1.251208-wear`

### Manual Runs
- Auto-appends hour suffix: `25120814` (Dec 8, 2025 at 14:00)
- Version names: `1.25120814-mobile`, `1.25120814-wear`

### Scheduled Runs
- Clean date format: `251208`
- Version names: `1.251208-mobile`, `1.251208-wear`

### Custom Suffixes
- Enter `1`, `2`, `3` in workflow input
- Creates: `2512081`, `2512082`, etc.

---

## Troubleshooting

### Permission Errors

**"Permission 'iam.serviceAccounts.getAccessToken' denied"**
- Complete Step 4 (Workload Identity Pool access)
- Verify correct repository name in grant

**"Service account not authorized"**
- Complete Step 5 (Play Console permissions)
- Verify "Releases" permission is checked

### Upload Errors

**"You must declare foreground service permissions"**
- Complete Step 8
- Ensure declarations match your AndroidManifest.xml

**"Version code must be greater than X"**
- Use a version suffix for same-day deploys
- Or wait until tomorrow for higher date-based version

**"APKs must have unique version codes"**
- Ensure Wear OS uses `versionCodeProperty.toInt() + 1`

### Build Errors

**"File google-services.json is missing"**
- Add `GOOGLE_SERVICES_JSON_BASE64` secret (Step 7)
- Verify Base64 encoding is correct

**"versionCode property is required"**
- This is expected for local builds
- Workflow provides these automatically

---

## Resources

- [r0adkll/upload-google-play](https://github.com/r0adkll/upload-google-play)
- [Form Factor Tracks](https://support.google.com/googleplay/android-developer/answer/13295490)
- [Google Play Publishing API](https://developers.google.com/android-publisher)
- [Workload Identity Federation](https://cloud.google.com/iam/docs/workload-identity-federation)
- [Foreground Service Types](https://developer.android.com/about/versions/14/changes/fgs-types-required)

---

## Summary

**One-time setup (already done):**
✅ Google Cloud project, API, service account, Workload Identity Pool

**Per-app setup (~40 minutes):**
1. Add version properties to build files (5 min)
2. Create release notes files (2 min)
3. Grant Workload Identity Pool access (2 min)
4. Grant Play Console permissions (2 min)
5. Prepare and add secrets to GitHub (5 min)
6. Copy workflow file (1 min)
7. Declare foreground services if needed (3 min)
8. Test deployment (20 min)

**Last updated:** December 2025
**Infrastructure:** `android-play-store-automation` (Project #984604330802)
**Service Account:** `github-actions-deploy@android-play-store-automation.iam.gserviceaccount.com`
