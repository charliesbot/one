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

## Setup for Each New Android App

### Prerequisites
- [ ] App already published on Google Play Store (initial version uploaded manually)
- [ ] App has a release keystore (.jks file)
- [ ] GitHub repository created for the app

---

## Step 1: Add gradle-play-publisher Plugin to Your App

### 1.1 Update `gradle/libs.versions.toml`

**Add to `[versions]` section:**
```toml
gradlePlayPublisher = "3.11.0"
```

**Add to `[plugins]` section:**
```toml
gradle-play-publisher = { id = "com.github.triplet.play", version.ref = "gradlePlayPublisher" }
```

### 1.2 Update `app/build.gradle.kts`

**Add plugin (and ensure it's declared in root `build.gradle.kts` with `apply false`):**
```kotlin
plugins {
    // ... existing plugins
    alias(libs.plugins.gradle.play.publisher)
}
```

**Add version property support (before `android {}` block):**
```kotlin
// Support for runtime version override from CI.
// If missing (local dev), default to current date (YYMMDD) to match CI style.
val defaultVersionCode = java.text.SimpleDateFormat("yyMMdd").format(java.util.Date())
val versionCodeProperty: String = (project.findProperty("versionCode") as String?) ?: defaultVersionCode
val versionNameProperty: String = (project.findProperty("versionName") as String?) ?: "1.$defaultVersionCode-dev"
```

**Update defaultConfig:**
```kotlin
defaultConfig {
    applicationId = "com.example.yourapp"
    // ... other settings
    versionCode = versionCodeProperty.toInt()
    versionName = versionNameProperty
}
```

**Add play configuration (after `android {}` block):**
```kotlin
play {
    val serviceAccountFile = rootProject.file("play-store-service-account.json")
    if (serviceAccountFile.exists()) {
        serviceAccountCredentials.set(serviceAccountFile)
    }
    track.set("production")
    releaseStatus.set(com.github.triplet.gradle.androidpublisher.ReleaseStatus.COMPLETED)
    defaultToAppBundles.set(true)
}
```

### 1.3 If You Have a Wear OS Module

Repeat the same steps in `onewearos/build.gradle.kts` (or your wear module).

**Add version property support (before `android {}` block):**
```kotlin
// Support for runtime version override from CI.
// If missing (local dev), default to current date (YYMMDD) to match CI style.
val defaultVersionCode = java.text.SimpleDateFormat("yyMMdd").format(java.util.Date())
val versionCodeProperty: String = (project.findProperty("versionCode") as String?) ?: defaultVersionCode
val versionNameProperty: String = (project.findProperty("versionName") as String?) ?: "1.$defaultVersionCode-dev"
```

**Important**: Use `versionCode + 1` for Wear OS to avoid conflicts:
```kotlin
defaultConfig {
    applicationId = "com.example.yourapp"  // Same as phone app
    versionCode = versionCodeProperty.toInt() + 1  // Add +1 here!
    versionName = versionNameProperty
}
```

### 1.4 Update `.gitignore`

Add these lines to prevent committing service account credentials:
```
play-store-service-account.json
service-account.json
```

---

## Step 2: Create Release Notes

### Create directory structure:
```
.github/
  whatsnew/
    en-US/
      default.txt
    es-ES/
      default.txt
```

### English (`en-US/default.txt`):
```
Bug fixes and performance improvements.

Thank you for using [Your App Name]!
```

### Spanish (`es-ES/default.txt`):
```
Corrección de errores y mejoras de rendimiento.

¡Gracias por usar [Your App Name]!
```

Add more locales as needed (e.g., `fr-FR`, `de-DE`, etc.)

---

## Step 3: Copy GitHub Actions Workflow

### Copy the workflow file from ONE:
```bash
cp /path/to/one/.github/workflows/deploy-play-store.yml .github/workflows/
```

Or create `.github/workflows/deploy-play-store.yml` with this content:

```yaml
name: Deploy to Google Play Store

on:
  # Weekly schedule: Every Monday at 10:00 AM UTC
  schedule:
    - cron: '0 10 * * 1'

  # Manual trigger for ad-hoc deployments
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
          SUFFIX="${{ github.event.inputs.version_suffix }}"
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

          # Create keystore.properties for build
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
          echo "google-services.json created in app/ and onewearos/ directories."

      - name: Make gradlew executable
        run: chmod +x ./gradlew

      - name: Build and publish phone app to Play Store
        run: |
          ./gradlew :app:publishBundle \
            -PversionCode=$VERSION_CODE \
            -PversionName=$VERSION_NAME \
            --no-daemon \
            --stacktrace

      - name: Build and publish Wear OS app to Play Store
        run: |
          ./gradlew :onewearos:publishBundle \
            -PversionCode=$VERSION_CODE \
            -PversionName=$VERSION_NAME \
            --no-daemon \
            --stacktrace

      - name: Upload Crashlytics mapping files
        run: |
          ./gradlew :app:uploadCrashlyticsSymbolFileRelease \
            -PversionCode=$VERSION_CODE \
            -PversionName=$VERSION_NAME \
            --no-daemon || echo "::warning::Crashlytics upload failed for app"

          ./gradlew :onewearos:uploadCrashlyticsSymbolFileRelease \
            -PversionCode=$VERSION_CODE \
            -PversionName=$VERSION_NAME \
            --no-daemon || echo "::warning::Crashlytics upload failed for onewearos"

      - name: Cleanup sensitive files
        if: always()
        run: |
          rm -f keystore.jks keystore.properties app/google-services.json onewearos/google-services.json

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

**Note**: Uncomment the Wear OS step if your app has a wear module.

---

## Step 4: Grant Play Console Permissions

Your service account needs permission to release **this specific app**.

1. Go to: https://play.google.com/console
2. Click **"Users and permissions"** (left sidebar under "Setup")
3. Find `github-actions-deploy@android-play-store-automation.iam.gserviceaccount.com` in the list
4. Click on it to edit
5. Under **"App permissions"**:
   - Click **"Add app"**
   - Select your new app
   - Check **"Releases"** or **"Release to production"**
6. Click **"Save"**

**That's it!** The service account can now release your new app.

---

## Step 5: Prepare Your Keystore

### Find your app's keystore:
Typically located at `~/.android/your-app-keystore.jks`

### Get keystore information:
You need 4 pieces of information:
1. **Keystore file path**
2. **Key alias** (e.g., `key0`, `upload`, `release`)
3. **Keystore password**
4. **Key password**

These are usually in your local `keystore.properties` file or build configuration.

### Encode the keystore to Base64:
```bash
base64 -i ~/.android/your-app-keystore.jks | pbcopy
```

This copies the encoded keystore to your clipboard.

---

## Step 5.5: Prepare Your `google-services.json`

This file is required for Firebase (Crashlytics, Analytics, etc.) to function in your app, both locally and in CI.

### Get your `google-services.json` file:
1.  Go to your Firebase Console (console.firebase.google.com).
2.  Select your project.
3.  Find your Android app (with `applicationId` `com.charliesbot.one`).
4.  Download the `google-services.json` file. It's usually found in the `app/` directory of your local project. If you have a Wear OS module, use the same file for both.

### Encode the `google-services.json` to Base64:
```bash
base64 -w 0 path/to/your/app/google-services.json
```
(On macOS, use `base64 app/google-services.json | pbcopy` to copy directly to clipboard. On Linux, omit `-w 0` for default line wrapping, or use it for one long line).

This copies the encoded JSON to your clipboard (or prints it to the console).

---

## Step 6: Add GitHub Secrets

1. Go to your GitHub repo: `https://github.com/charliesbot/your-new-app`
2. **Settings** → **Secrets and variables** → **Actions**
3. Click **"New repository secret"** for each:

| Secret Name              | Value                                   | Example             |
|--------------------------|-----------------------------------------|---------------------|
| `KEYSTORE_FILE_BASE64`   | Paste from clipboard (Step 5)           | Long base64 string  |
| `KEYSTORE_KEY_ALIAS`     | Your key alias                          | `key0` or `upload`  |
| `KEYSTORE_STORE_PASSWORD`| Your keystore password                  | From your keystore.properties |
| `KEYSTORE_KEY_PASSWORD`  | Your key password                       | From your keystore.properties |
| `GOOGLE_SERVICES_JSON_BASE64` | Paste from clipboard (Step 5.5)      | Long base64 string  |

**Security Note**: Never commit keystore files or passwords to git!

---

## Step 7: Test the Deployment

### Manual Test Run:

1. Go to your GitHub repo → **Actions** tab
2. Click **"Deploy to Google Play Store"** workflow
3. Click **"Run workflow"** (button on the right)
4. Leave version suffix empty
5. Click green **"Run workflow"** button
6. Monitor the build (~15-20 minutes)

### What to Check:

✅ Workflow completes successfully
✅ Green checkmarks on all steps
✅ No red error messages

### Verify in Play Console:

1. Go to: https://play.google.com/console
2. Select your app
3. Go to **"Release"** → **"Production"**
4. You should see a new release with:
   - Phone AAB with today's date as versionCode (e.g., `251208`)
   - Wear AAB with versionCode + 1 (if applicable)
   - Release notes in your configured languages

---

## Step 8: Enable Automatic Weekly Deployments

The workflow is already configured to run **every Monday at 10:00 AM UTC**.

After successful manual test, it will automatically deploy every week!

### For Same-Day Hotfixes:
1. Go to Actions → "Deploy to Google Play Store"
2. Click "Run workflow"
3. Enter version suffix: `1`, `2`, `3`, etc.
4. This creates versions like `2512081`, `2512082`

---

## Version Strategy

### How Versions Work:
- **versionCode**: Date-based `YYMMDD` (e.g., `251208` for Dec 8, 2025)
- **versionName**: `1.YYMMDD` (e.g., `1.251208`)
- **Phone app**: Gets base versionCode (e.g., `251208`)
- **Wear OS app**: Gets versionCode + 1 (e.g., `251209`) to avoid Play Store conflicts

### Same-Day Deployments:
Use version suffix to create: `2512081`, `2512082`, etc.

---

### Troubleshooting

### "File google-services.json is missing"
- This file is essential for Firebase (Crashlytics, etc.)
- Ensure you have followed "Step 5.5: Prepare Your `google-services.json`"
- Verify that the `GOOGLE_SERVICES_JSON_BASE64` secret exists and contains the correct Base64 encoded content.

### "Error: Service account not authorized"
- Make sure you completed Step 4 (Play Console permissions)
- Check that you selected the correct app and granted "Releases" permission

### "Error: Version code must be greater than X"
- Google Play requires monotonically increasing version codes
- If you already uploaded a higher version manually, use a version suffix
- Or wait for the next day when the date-based version will be higher

### "Error: APKs must have unique version codes"
- Phone and Wear OS apps sharing the same applicationId must have different version codes
- Make sure Wear OS uses `versionCodeProperty.toInt() + 1`

### "Build failed: versionCode property is required"
- The workflow is working correctly - it requires version properties
- This error is intentional to prevent building without proper versioning
- The workflow sets these automatically via `-PversionCode` and `-PversionName`

### "Authentication failed"
- Check that Workload Identity Federation is properly configured
- Verify the service account email is correct in the workflow
- Make sure your repo is under `charliesbot/` organization/username

---

## Quick Checklist for New Apps

Use this checklist when setting up a new app:

- [ ] Add gradle-play-publisher plugin to `build.gradle.kts` files (and root `build.gradle.kts` with `apply false`)
- [ ] Add version property support to build files
- [ ] Update `.gitignore` to exclude service account JSON
- [ ] Create `.github/whatsnew/` directories with release notes
- [ ] Copy GitHub Actions workflow file
- [ ] Grant Play Console permissions to service account for new app
- [ ] Encode keystore to Base64
- [ ] Add 4 GitHub Secrets (keystore-related)
- [ ] Prepare and encode `google-services.json`
- [ ] Add `GOOGLE_SERVICES_JSON_BASE64` secret to GitHub
- [ ] Test manual deployment via GitHub Actions
- [ ] Verify release appears in Play Console
- [ ] Weekly deployments now automatic!

---

## Cost Considerations

### Google Cloud
- Workload Identity Federation: **Free**
- Google Play Developer API calls: **Free** (within quota)
- Service Account: **Free**

### GitHub Actions
- Public repos: **Unlimited** free minutes
- Private repos: **2,000 free minutes/month**
- Each deployment takes ~15-20 minutes
- Weekly = ~4 deployments/month = ~60-80 minutes/month
- Well within free tier!

---

## Resources

- **gradle-play-publisher docs**: https://github.com/Triple-T/gradle-play-publisher
- **Google Play Publishing API**: https://developers.google.com/android-publisher
- **Workload Identity Federation**: https://cloud.google.com/iam/docs/workload-identity-federation
- **GitHub Actions docs**: https://docs.github.com/en/actions
- **Firebase Crashlytics**: https://firebase.google.com/docs/crashlytics

---

## Summary

### What You Did Once (for ONE Fasting Tracker):
✅ Created Google Cloud project
✅ Enabled Google Play Developer API
✅ Set up Workload Identity Pool and Provider
✅ Created service account
✅ Configured OIDC authentication

### What You Do for Each New App (Quick!):
1. Add plugin to build files (5 min)
2. Grant Play Console permissions (2 min)
3. Add keystore secrets to GitHub (3 min)
4. Copy workflow file (1 min)
5. Test deployment (20 min)

**Total setup time per new app: ~30 minutes!**

---

**Last updated**: December 2025
**Infrastructure**: `android-play-store-automation` (Project #984604330802)
**Service Account**: `github-actions-deploy@android-play-store-automation.iam.gserviceaccount.com`
