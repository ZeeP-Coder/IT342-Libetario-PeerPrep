# Registration & Login Troubleshooting Guide

## The Issue
You're getting: **"Error: CLEARTEXT communication to 10.0.2.2 not permitted by network se..."**

## Why This Happens
Android 9+ requires HTTPS by default. Your backend is HTTP (which is fine for local testing), but the network security config wasn't allowing it. We've fixed this.

---

## ✅ What We've Done

1. **Network Security Config Fixed** ✅
   - Now allows HTTP (cleartext) traffic to:
     - `10.0.2.2` (Android emulator localhost)
     - `localhost`
     - `127.0.0.1`
     - `192.168.x.x` (local network IPs)

2. **Added Comprehensive Logging** ✅
   - RegisterActivity now logs every step
   - LoginActivity already has detailed logging
   - Check Android Studio **Logcat** for detailed error messages

---

## How to Debug Now

### Step 1: Check Your Backend is Running

**For Android Emulator (port 5173):**
```bash
# Check if backend is accessible from your PC
curl -X POST http://localhost:5173/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Test","email":"test@test.com","password":"pass123","university":"CIT","major":"CS"}'
```

**For Physical Device:**
Replace `10.0.2.2` with your **computer's local IP**:
- Find your IP: `ipconfig` (Windows) → look for IPv4 address (like `192.168.1.100`)
- Update in `RetrofitClient.kt`:
  ```kotlin
  private const val BASE_URL = "http://192.168.1.100:5173/"
  ```

### Step 2: Check Logcat Output

In Android Studio:
1. **View** → **Tool Windows** → **Logcat**
2. Filter by: `RegisterActivity` or `LoginActivity`
3. When you try to register, look for messages like:

**Success messages:**
```
D/RegisterActivity: registerUser() called with email: test@gmail.com
D/RegisterActivity: Creating RegisterRequest...
D/RegisterActivity: Sending to backend: POST /api/auth/register
D/RegisterActivity: Response code: 200
D/RegisterActivity: Registration successful
```

**Error messages:**
```
E/RegisterActivity: Registration exception: java.net.ConnectException: failed to connect to 10.0.2.2 (port 5173)
E/RegisterActivity: Registration error: {"success":false,"error":"User already exists"}
```

### Step 3: Identify the Problem

**If you see "failed to connect":**
- ❌ Backend is NOT running
- ✅ Solution: Start your backend on port 5173

**If you see "User already exists":**
- ✅ Network is working
- ❌ Backend validation error
- ✅ Solution: Use a different email or delete user from DB

**If you see "404 not found":**
- ✅ Network is working
- ❌ Backend `/api/auth/register` endpoint doesn't exist
- ✅ Solution: Implement the endpoint in your backend

**If you see other HTTP error codes (500, 400, etc.):**
- ✅ Network is working
- ❌ Backend error
- ✅ Solution: Check backend server logs

---

## Verify the Network Config Works

After rebuilding, the app will allow HTTP traffic. Try registering with:
- **Name:** TestUser
- **Email:** testuser@test.com
- **University:** CIT
- **Major:** Computer Science
- **Password:** test123
- **Confirm:** test123

If you still get the **CLEARTEXT** error after rebuild:
1. **Clean rebuild:** File → Invalidate Caches → Restart IDE
2. **Uninstall app** from emulator/device
3. **Run app again** from Android Studio

---

## Common Issues & Solutions

### Issue: "CLEARTEXT still showing"
**Solution:** Make sure you:
- [ ] Rebuilt the app (Gradlew clean build)
- [ ] Uninstalled the old app from emulator/device
- [ ] Ran the new build

### Issue: "Network unreachable"
**Solution:**
- Check backend is running: `curl http://10.0.2.2:5173/api/auth/login`
- For physical device: use your machine IP instead of 10.0.2.2

### Issue: "Backend returns 400/validation error"
**Solution:**
- Check `RegisterRequest` matches backend expected fields
- Make sure `fullName` not `name` is being sent (frontend is correct)
- Verify password requirements match backend

### Issue: "Backend returns 500"
**Solution:**
- Check your backend server console for error logs
- Verify database connection is working
- Check MongoDB/PostgreSQL is running

---

## Next Steps

1. **Rebuild app** (Gradlew clean build)
2. **Uninstall** old app from emulator
3. **Run app** again
4. **Try registering** with test data
5. **Check Logcat** for detailed error messages
6. **Share the Logcat output** if still failing

Your app is now ready for testing! The network config should allow HTTP traffic to your local backend. 🚀

