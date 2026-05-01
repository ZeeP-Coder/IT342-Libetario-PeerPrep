# PeerPrep

## Backend local setup (Supabase + Google)

### 1) Configure Google OAuth client in Google Cloud

Use a Web application OAuth client and set:

- Authorized redirect URI: http://localhost:8081/login/oauth2/code/google
- Authorized JavaScript origin: http://localhost:5173

### 2) Set backend environment variables in PowerShell

Run these in the same terminal before starting the backend:

```powershell
$env:DB_URL="jdbc:postgresql://aws-1-ap-northeast-1.pooler.supabase.com:6543/postgres?sslmode=require"
$env:DB_USERNAME="postgres.popjasyblcoejvdianri"
$env:DB_PASSWORD="YOUR_SUPABASE_DB_PASSWORD"

$env:FRONTEND_URL="http://localhost:5173"

$env:GOOGLE_ANDROID_CLIENT_ID="YOUR_ANDROID_CLIENT_ID.apps.googleusercontent.com"
$env:GOOGLE_CLIENT_ID="YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
$env:GOOGLE_CLIENT_SECRET="YOUR_WEB_CLIENT_SECRET"
```

Notes:

- GOOGLE_ANDROID_CLIENT_ID is used by the mobile Google token login endpoint.
- GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET are used for browser OAuth login.

### 3) Run backend

```powershell
cd backend/peerprep
.\mvnw.cmd spring-boot:run
```

Optional one-command run:

1. Copy backend/peerprep/.env.example to backend/peerprep/.env.local
2. Fill in your values in .env.local
3. Start with:

```powershell
cd backend/peerprep
.\scripts\start-dev.ps1
```

### 4) Verify

- Web app should work at http://localhost:5173 with backend at http://localhost:8081.
- If Google web OAuth is missing, clicking Google sign-in redirects to login with not-configured.
