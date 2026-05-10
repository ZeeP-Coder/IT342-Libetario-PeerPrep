# PeerPrep Software Test Plan

Date: 2026-05-06
Scope: Backend, Web, Mobile

## Functional Requirements Coverage

1. Authentication
- Registration
- Login
- Google auth entrypoint
- Logout/session handling

2. User Profile
- View profile
- Update profile fields

3. Study Groups
- Create group
- View dashboard/groups
- Join group
- Leave group
- Delete group (owner)
- Search/filter (web/mobile UI)

4. Notifications
- Notification enablement (web/browser/mobile capability)
- Session reminder pathway

5. Dashboard and Navigation
- Access dashboard
- Navigate between feature screens/pages

## Test Cases and Test Steps (Core)

1. AUTH_REGISTER
- Steps: submit valid registration payload
- Expected: 201 + success

2. AUTH_LOGIN
- Steps: submit valid login credentials
- Expected: 200 + success

3. PROFILE_UPDATE
- Steps: update profile fields for existing user
- Expected: 200 + updated profile values

4. GROUP_CREATE
- Steps: create group using creator email
- Expected: 201 + success

5. GROUP_DASHBOARD
- Steps: fetch dashboard for creator
- Expected: created group appears in my groups

6. GROUP_JOIN
- Steps: second user joins group
- Expected: 200 + success

7. GROUP_LEAVE
- Steps: second user leaves group
- Expected: 200 + success

8. GROUP_DELETE
- Steps: owner deletes group
- Expected: 200 + success

## Automated Test Cases

Backend:
- Maven/JUnit context test (`PeerprepApplicationTests`)
- API scenario automation script for register/login/profile/group lifecycle

Web:
- Vite production build regression check
- ESLint regression check

Mobile:
- Gradle unit test task (`testDebugUnitTest`) regression check

## Test Execution Procedure

1. Start backend service
2. Run backend tests (`mvnw test`)
3. Run web build and lint (`npm run build`, `npm run lint`)
4. Run mobile unit task (`gradlew testDebugUnitTest`)
5. Run backend API scenario checks
6. Record PASS/FAIL and issue details

## Pass/Fail Criteria

- PASS: expected behavior and successful command execution
- FAIL: command failure, API error, functional mismatch
- WARNING: non-blocking quality issue
