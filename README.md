# PeerPrep

## Google OAuth setup (local)

1. In Google Cloud Console, set these values for your OAuth Web client:
	- Authorized redirect URI: `http://localhost:8080/login/oauth2/code/google`
	- Authorized JavaScript origin: `http://localhost:5173`
2. In PowerShell, set backend environment variables before running Spring Boot:

```powershell
$env:SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID="your-client-id"
$env:SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET="your-client-secret"
$env:SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_SCOPE="openid,profile,email"
```

3. Start backend on port `8081`, then start frontend on `5173`.
4. Click Google sign-in in the app.

If Google credentials are not set, the app redirects back to login with a clear "not configured" message.
