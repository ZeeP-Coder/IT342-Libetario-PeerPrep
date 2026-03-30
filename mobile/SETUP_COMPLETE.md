# PeerPrep Mobile Auth Setup - Complete Guide

## ✅ What's Done (App-Side)

Your Android app is now fully configured with:

- ✅ **Google Sign-In** (Web Client ID: `242891288998-bu2al00p956edtgepklq4f1ntoscitv2.apps.googleusercontent.com`)
- ✅ **Email/Password Login**
- ✅ **Registration with validation**
- ✅ **Cleartext HTTP support** for localhost testing (10.0.2.2)
- ✅ **Logging** for debugging
- ✅ **Modern UI** (centered card design, no action bar)

---

## 🔧 Next Steps to Get It Working

### 1. Google Cloud Setup (Required for Google Sign-In)
Go to: https://console.cloud.google.com/apis/credentials

#### Add Test User
- **OAuth consent screen** → **Test users** section
- Add your Gmail account email
- This allows you to test Google sign-in

#### Verify Web Client ID
- You should see your Web Client ID created:
  - `242891288998-bu2al00p956edtgepklq4f1ntoscitv2.apps.googleusercontent.com`
- Keep this safe (it's already in your app)

---

### 2. Backend Endpoints (Your Phase 1 Backend)

Your app calls these endpoints:

#### A) Email/Password Login
```
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}

Response (success):
{
  "success": true,
  "user": { "id": "...", "email": "..." },
  "token": "jwt_token_here"
}
```

#### B) Email/Password Register
```
POST /api/auth/register
Content-Type: application/json

{
  "fullName": "John Doe",
  "email": "user@example.com",
  "university": "CIT",
  "major": "Computer Science",
  "password": "password123"
}

Response (success):
{
  "success": true,
  "user": { "id": "...", "email": "..." },
  "token": "jwt_token_here"
}
```

#### C) Google Sign-In (NEW)
```
POST /api/auth/google
Content-Type: application/json

{
  "idToken": "google_id_token_string"
}

Response (success):
{
  "success": true,
  "user": { "id": "...", "email": "..." },
  "token": "jwt_token_here"
}
```

---

### 3. Backend: Implement `/api/auth/google` Endpoint

#### Node.js / Express Example

Install:
```bash
npm install google-auth-library
```

Add to your `.env`:
```
GOOGLE_WEB_CLIENT_ID=242891288998-bu2al00p956edtgepklq4f1ntoscitv2.apps.googleusercontent.com
```

Add endpoint:
```javascript
const { OAuth2Client } = require("google-auth-library");
const client = new OAuth2Client(process.env.GOOGLE_WEB_CLIENT_ID);

app.post("/api/auth/google", async (req, res) => {
  try {
    const { idToken } = req.body;

    // Verify token with Google
    const ticket = await client.verifyIdToken({
      idToken,
      audience: process.env.GOOGLE_WEB_CLIENT_ID,
    });

    const payload = ticket.getPayload();
    const email = payload.email;
    const name = payload.name;
    const picture = payload.picture;

    // TODO: Find or create user in DB
    let user = await User.findOne({ email });
    if (!user) {
      user = await User.create({
        email,
        fullName: name,
        profilePicture: picture,
        isGoogleAuth: true,
        // Set a random password since they're using Google auth
        password: crypto.randomBytes(16).toString("hex"),
      });
    }

    // Create JWT token
    const token = jwt.sign(
      { userId: user._id, email: user.email },
      process.env.JWT_SECRET,
      { expiresIn: "7d" }
    );

    res.json({
      success: true,
      user: { id: user._id, email: user.email, name: user.fullName },
      token,
    });
  } catch (error) {
    console.error("Google auth error:", error);
    res.status(401).json({ success: false, error: error.message });
  }
});
```

#### Java / Spring Boot Example

Add dependency:
```xml
<dependency>
  <groupId>com.google.auth</groupId>
  <artifactId>google-auth-library-oauth2-http</artifactId>
  <version>1.11.0</version>
</dependency>
```

Controller:
```java
@PostMapping("/api/auth/google")
public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> request) {
  try {
    String idToken = request.get("idToken");

    GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
      .setAudience(Arrays.asList(System.getenv("GOOGLE_WEB_CLIENT_ID")))
      .build();

    GoogleIdToken token = verifier.verify(idToken);
    if (token == null) {
      return ResponseEntity.status(401).body(Map.of("success", false, "error", "Invalid token"));
    }

    GoogleIdToken.Payload payload = token.getPayload();
    String email = payload.getEmail();
    String name = (String) payload.get("name");

    // TODO: Find or create user in DB
    User user = userRepository.findByEmail(email)
      .orElseGet(() -> userRepository.save(new User(email, name)));

    String jwtToken = jwtProvider.generateToken(user.getId());

    return ResponseEntity.ok(Map.of(
      "success", true,
      "user", Map.of("id", user.getId(), "email", user.getEmail()),
      "token", jwtToken
    ));
  } catch (Exception e) {
    return ResponseEntity.status(401).body(Map.of("success", false, "error", e.getMessage()));
  }
}
```

---

### 4. Test Your Setup

#### Test Email/Password Login & Register
1. Run your backend (make sure it's on `10.0.2.2:5173` for emulator or your IP for device)
2. Run the Android app
3. Go to **Register** → fill form → **Create Account**
4. Should see success or backend error message
5. Go to **Login** → enter credentials → **Sign in**
6. Should redirect to home or show backend error

#### Test Google Sign-In
1. Make sure you added yourself as a test user in Google Cloud Console
2. Tap **Login with Google**
3. Google account picker should appear
4. Select your test account
5. App should send ID token to backend `/api/auth/google`
6. Should redirect to home or show backend error

---

## 🐛 Debugging Tips

### If Google Sign-In Says "cancelled/failed"
Check Android Studio **Logcat** (View → Tool Windows → Logcat):
```
adb logcat | grep "LoginActivity"
```

Look for messages like:
- `GetCredentialException` = Google account picker issue
- `invalid audience` = Web Client ID mismatch (make sure it matches `GOOGLE_WEB_CLIENT_ID` in backend)
- `network error` = backend `/api/auth/google` not reachable

### If Register/Login Says "CLEARTEXT communication"
You're likely using HTTPS URL instead of HTTP. Make sure your backend is running on HTTP for local testing:
- Emulator: `http://10.0.2.2:5173`
- Physical device: `http://YOUR_MACHINE_IP:5173`

Update in `RetrofitClient.kt` if needed:
```kotlin
private const val BASE_URL = "http://10.0.2.2:5173/"
```

### If Backend Returns 404 on `/api/auth/google`
Make sure your backend has the endpoint implemented. Check your routes/controllers.

---

## 📋 Checklist Before Testing

- [ ] Android app builds successfully ✅ (Done)
- [ ] Backend running locally on HTTP
- [ ] Google Web Client ID added to backend `.env` or config
- [ ] You're added as a test user in Google Cloud Console OAuth consent screen
- [ ] Backend endpoints `/api/auth/login`, `/api/auth/register`, `/api/auth/google` implemented
- [ ] Network is accessible (emulator on 10.0.2.2 or device on your IP)

---

## 🚀 You're Ready!

Your mobile app is now fully functional. All that's left is:
1. Ensure backend endpoints are properly implemented
2. Test email/password flow first (easier to debug)
3. Test Google sign-in second
4. Commit your progress!

Good luck! 🎉

