# Google Sign-In Setup Instructions

## Problem
The Client ID you're currently using (`242891288998-...`) is an **Android OAuth Client ID**.
For the ID token flow we coded, you need a **Web Client ID** instead.

## Solution: Create a Web Client ID

### Step 1: Go to Google Cloud Console
- https://console.cloud.google.com/apis/credentials

### Step 2: Create OAuth 2.0 Client ID (Web Application)
1. Click **"Create Credentials"** → **OAuth client ID**
2. Application type: **Web application**
3. Name it: `PeerPrep Web` (or anything)
4. Click **Create**
5. Copy the new **Client ID** (format: `xxx-yyy.apps.googleusercontent.com`)

### Step 3: Replace in LoginActivity.kt
Replace this line:
```kotlin
private val serverClientId: String = "242891288998-ubhgtf03voqjdi4fc9b1oes2jcoia9vi.apps.googleusercontent.com"
```

With your NEW Web Client ID:
```kotlin
private val serverClientId: String = "YOUR_NEW_WEB_CLIENT_ID.apps.googleusercontent.com"
```

### Step 4: Add yourself as Test User
Google Cloud Console → OAuth consent screen → **Test users** → Add your Gmail

### Why This Matters
- **Android Client ID**: Used for Google Play Services SDK (old flow)
- **Web Client ID**: Required for requesting ID tokens (what we coded)
- Google won't issue an ID token to an Android client ID

## Test Again
After replacing the Web Client ID:
1. Rebuild app
2. Tap "Login with Google"
3. Should see Google account picker (not error)

---

If you're stuck getting the Web Client ID, reply with a screenshot of the "Create OAuth client ID" screen and I'll walk you through it step-by-step.

