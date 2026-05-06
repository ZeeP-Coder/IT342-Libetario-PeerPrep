# PeerPrep Regression Test Results (Part 4)

Date: 2026-05-06
Branch: Vertical-Slice-Refactoring-and-Full-Regression-Testing
Scope: Backend, Web, Mobile

## 1) Automated/Build Evidence

### Backend
Command:
- `backend/peerprep`: `./mvnw.cmd test`

Result:
- PASS
- Tests run: 1
- Failures: 0
- Errors: 0
- Build: SUCCESS

### Web
Commands:
- `web/PeerPrep`: `npm run -s build`
- `web/PeerPrep`: `npm run -s lint`

Result:
- Build: PASS
- Lint: PASS with warning (1)
- Warning: `StudyGroupsPage.tsx` React hooks dependency warning (`react-hooks/exhaustive-deps`) on `refreshDashboard`

### Mobile
Command:
- `mobile`: `./gradlew.bat testDebugUnitTest --console=plain`

Result:
- PASS
- `:app:testDebugUnitTest UP-TO-DATE`
- Build successful

## 2) Backend Functional Regression Scenarios (API)

Target base URL: `http://localhost:8081`

Results:
1. AUTH_GOOGLE_ENDPOINT: PASS (HTTP 200)
2. AUTH_REGISTER_USER1: PASS (HTTP 201)
3. AUTH_LOGIN_USER1: PASS (HTTP 200)
4. PROFILE_UPDATE: PASS (HTTP 200)
5. GROUP_CREATE: PASS (HTTP 201)
6. GROUP_DASHBOARD: PASS (group visible in my groups)
7. AUTH_REGISTER_USER2: PASS (HTTP 201)
8. GROUP_JOIN_USER2: PASS (HTTP 200)
9. GROUP_LEAVE_USER2: PASS (HTTP 200)
10. GROUP_DELETE_OWNER: PASS (HTTP 200)

Pass count: 10/10
Fail count: 0/10

## 3) Functional Requirement Coverage Status

- User Authentication (register/login/Google endpoint): Covered and passing
- User Profile Management (update): Covered and passing
- Study Group Management (create/dashboard/join/leave/delete): Covered and passing
- Dashboard retrieval: Covered and passing
- Web app regression compile/lint: Covered (build pass, one lint warning)
- Mobile module regression build/tests: Covered (task successful)

## 4) Issues/Regressions Found

1. Severity: Low
   - Area: Web Frontend
   - File: `web/PeerPrep/src/features/study-groups/pages/StudyGroupsPage.tsx`
   - Issue: React hook dependency warning on `refreshDashboard`
   - Impact: No runtime failure observed; maintainability/consistency warning
   - Status: Open (non-blocking)

## 5) Fixes Applied During This Regression Run

- No functional bug fix was required for blocking regressions.
- No code patches were applied in this run.
