# Google Drive Sync - OAuth Token Refresh Fix Plan

## Problem Summary

The current Google Drive sync implementation has critical OAuth issues:
1. **Desktop**: Authorization code is used directly as access token (completely broken)
2. **Desktop/Android**: No refresh token storage - sync fails after 1 hour when token expires
3. **Android**: Token refresh relies on Google Play Services but isn't explicitly handled
4. **Inconsistent**: iOS implementation is correct, but differs from Kotlin side

## Implementation Approach

### Phase 1: Create Proper Credential Storage Format

**File**: `feedSync/googledrive/src/commonMain/kotlin/.../GoogleDriveCredentials.kt` (new)

Create a serializable credentials class similar to Dropbox's pattern:

```kotlin
@Serializable
data class GoogleDriveCredentials(
    val accessToken: String,
    val refreshToken: String?,
    val expiresAtMillis: Long,
    val tokenType: String = "Bearer"
)
```

- Use kotlinx.serialization for JSON storage
- Store in `GoogleDriveSettings` as JSON string (same pattern as Dropbox)

---

### Phase 2: Fix Desktop (JVM) OAuth Implementation

**Files to modify**:
- `shared/src/jvmMain/.../GoogleDriveSyncViewModel.desktop.kt`
- `feedSync/googledrive/src/commonJvmAndroidMain/.../GoogleDriveDataSourceJvm.kt`
- `desktopApp/src/jvmMain/resources/props.properties`

#### 2.1 Add client credentials to props.properties
```properties
google_drive_client_id=<your-client-id>
google_drive_client_secret=<your-client-secret>
```

#### 2.2 Implement token exchange in ViewModel
Replace the broken code that uses auth code as token:

```kotlin
fun handleGoogleDriveAuthResponse(authorizationCode: String) {
    viewModelScope.launch {
        try {
            // Exchange code for tokens using GoogleAuthorizationCodeTokenRequest
            val tokenResponse = GoogleAuthorizationCodeTokenRequest(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                "https://oauth2.googleapis.com/token",
                clientId,
                clientSecret,
                authorizationCode,
                "urn:ietf:wg:oauth:2.0:oob"
            ).execute()

            val credentials = GoogleDriveCredentials(
                accessToken = tokenResponse.accessToken,
                refreshToken = tokenResponse.refreshToken,
                expiresAtMillis = System.currentTimeMillis() + (tokenResponse.expiresInSeconds * 1000)
            )

            // Save as JSON
            googleDriveSettings.setGoogleDriveData(Json.encodeToString(credentials))
            // ... rest of flow
        } catch (e: Exception) {
            // Handle error
        }
    }
}
```

#### 2.3 Implement automatic token refresh in DataSource

Update `GoogleDriveDataSourceJvm` to:
1. Parse credentials from JSON
2. Check token expiry before API calls
3. Use `GoogleRefreshTokenRequest` when token expired
4. Update stored credentials after refresh

```kotlin
private suspend fun getOrRefreshCredentials(): GoogleCredential {
    val creds = parseCredentials()

    if (creds.expiresAtMillis < System.currentTimeMillis() + 60_000) { // 1 min buffer
        val refreshed = GoogleRefreshTokenRequest(
            httpTransport, jsonFactory,
            creds.refreshToken, clientId, clientSecret
        ).execute()

        // Update stored credentials
        val newCreds = creds.copy(
            accessToken = refreshed.accessToken,
            expiresAtMillis = System.currentTimeMillis() + (refreshed.expiresInSeconds * 1000)
        )
        saveCredentials(newCreds)
        return createCredential(newCreds)
    }
    return createCredential(creds)
}
```

---

### Phase 3: Fix Android OAuth Implementation

**Files to modify**:
- `shared/src/androidMain/.../GoogleDriveAuth.kt`
- `androidApp/src/main/.../GoogleDriveSyncActivity.kt`
- `shared/src/commonMobileMain/.../GoogleDriveSyncViewModel.kt`

#### Option A: Use silentSignIn for token refresh (Simpler)
- Before each sync operation, call `googleSignInClient.silentSignIn()`
- This refreshes the token automatically via Google Play Services
- Get fresh access token from the result

```kotlin
suspend fun getFreshAccessToken(context: Context): String? {
    val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null

    // silentSignIn refreshes token if needed
    val result = GoogleSignIn.getClient(context, gso).silentSignIn().await()

    return withContext(Dispatchers.IO) {
        GoogleAuthUtil.getToken(context, result.account!!, SCOPES)
    }
}
```

#### Option B: Store refresh token (More robust but complex)
- Request `requestServerAuthCode(clientId)` in GoogleSignInOptions
- Exchange server auth code for refresh token on first auth
- Store and use refresh token like desktop

**Recommendation**: Option A is simpler and leverages Google Play Services properly.

---

### Phase 4: Align iOS Implementation (Minor)

**Files**:
- `iosApp/Source/Accounts/GoogleDrive/Data/GoogleDriveDataSourceIos.swift`

iOS implementation is mostly correct using `GIDSignIn` and `fetcherAuthorizer`.

Minor improvements:
- Add explicit error handling for expired sessions
- Call `restorePreviousSignIn` on app launch to refresh session

---

### Phase 5: Update DataSource Interface

**File**: `feedSync/googledrive/src/commonMain/.../GoogleDriveDataSource.kt`

Add method to handle token refresh:
```kotlin
interface GoogleDriveDataSource {
    // ... existing methods

    suspend fun ensureValidToken(): Boolean  // Check/refresh token before operations
}
```

---

## Files to Modify

| File | Changes |
|------|---------|
| `feedSync/googledrive/build.gradle.kts` | Add kotlinx.serialization dependency |
| `feedSync/googledrive/src/commonMain/.../GoogleDriveCredentials.kt` | New: Serializable credentials class |
| `feedSync/googledrive/src/commonMain/.../GoogleDriveDataSource.kt` | Add `ensureValidToken()` method |
| `feedSync/googledrive/src/commonJvmAndroidMain/.../GoogleDriveDataSourceJvm.kt` | Implement token parsing, refresh logic |
| `shared/src/jvmMain/.../GoogleDriveSyncViewModel.desktop.kt` | Implement token exchange |
| `shared/src/androidMain/.../GoogleDriveAuth.kt` | Add silentSignIn refresh logic |
| `shared/src/commonMobileMain/.../GoogleDriveSyncViewModel.kt` | Use refreshed tokens |
| `desktopApp/src/jvmMain/resources/props.properties` | Add Google client_id and client_secret |

---

## Implementation Order

1. Create `GoogleDriveCredentials` data class with serialization
2. Update `GoogleDriveSettings` to store/retrieve JSON credentials
3. Fix Desktop ViewModel - implement token exchange
4. Fix Desktop DataSource - implement token refresh
5. Fix Android - add silentSignIn before token usage
6. Test all platforms

---

## User Decisions

- **OAuth Setup**: Need to set up both desktop and Android credentials
- **Desktop Flow**: OOB (copy-paste code) - user copies authorization code from browser
- **Secret Storage**: props.properties (matches existing Dropbox pattern)

---

## Pre-Implementation: Google Cloud Setup

Before implementing, you need to set up OAuth credentials in Google Cloud Console:

### Step 1: Create/Configure Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing one
3. Enable the **Google Drive API**:
    - Go to "APIs & Services" → "Library"
    - Search for "Google Drive API" and enable it

### Step 2: Configure OAuth Consent Screen

1. Go to "APIs & Services" → "OAuth consent screen"
2. Choose "External" user type
3. Fill in required fields (app name, support email)
4. Add scopes:
    - `https://www.googleapis.com/auth/drive.file`
    - `https://www.googleapis.com/auth/drive.appdata`
5. Add test users (your email) while in testing mode

### Step 3: Create Desktop OAuth Credentials

1. Go to "APIs & Services" → "Credentials"
2. Click "Create Credentials" → "OAuth client ID"
3. Application type: **Desktop app**
4. Name: "FeedFlow Desktop"
5. Copy the **Client ID** and **Client Secret**

### Step 4: Create Android OAuth Credentials

1. Click "Create Credentials" → "OAuth client ID"
2. Application type: **Android**
3. Name: "FeedFlow Android"
4. Package name: `com.prof18.feedflow` (verify in AndroidManifest.xml)
5. Get SHA-1 fingerprint:
   ```bash
   # Debug key
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android

   # Release key (for production)
   keytool -list -v -keystore your-release-key.keystore -alias your-alias
   ```
6. Copy the **Client ID** (no secret needed for Android)

### Step 5: Create iOS OAuth Credentials (if needed)

1. Click "Create Credentials" → "OAuth client ID"
2. Application type: **iOS**
3. Bundle ID: `com.prof18.feedflow`
4. Copy the **Client ID**

---

## Configuration Values Needed

After setup, you'll have:

```
# Desktop (for props.properties)
google_drive_client_id=<desktop-client-id>.apps.googleusercontent.com
google_drive_client_secret=<desktop-client-secret>

# Android (for build.gradle or AndroidManifest)
google_drive_android_client_id=<android-client-id>.apps.googleusercontent.com

# iOS (for Info.plist)
google_drive_ios_client_id=<ios-client-id>.apps.googleusercontent.com
```
