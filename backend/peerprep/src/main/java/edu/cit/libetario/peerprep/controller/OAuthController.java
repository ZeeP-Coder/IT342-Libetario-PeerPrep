package edu.cit.libetario.peerprep.controller;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import edu.cit.libetario.peerprep.dto.AuthResponse;
import edu.cit.libetario.peerprep.entity.User;
import edu.cit.libetario.peerprep.repository.UserRepository;

@RestController
@RequestMapping("/api/auth")
public class OAuthController {

    private static final String GOOGLE_AUTH_PASSWORD = "GOOGLE_AUTH";
    private static final String NOT_SET = "Not Set";
    private static final String DEFAULT_GOOGLE_ANDROID_CLIENT_ID =
            "242891288998-bu2al00p956edtgepklq4f1ntoscitv2.apps.googleusercontent.com";

    private final UserRepository userRepository;
    private final String frontendUrl;
    private final String googleAndroidClientId;
    private final RestClient restClient;


    public OAuthController(
            UserRepository userRepository,
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${app.google.android-client-id:" + DEFAULT_GOOGLE_ANDROID_CLIENT_ID + "}")
                    String googleAndroidClientId) {
        this.userRepository = userRepository;
        this.frontendUrl = frontendUrl;
        this.googleAndroidClientId = googleAndroidClientId;
        this.restClient = RestClient.create();
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> request) {
        String idTokenString = request.get("idToken");
        if (idTokenString == null || idTokenString.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "idToken is required"));
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> tokenInfo = restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .scheme("https")
                    .host("oauth2.googleapis.com")
                    .path("/tokeninfo")
                    .queryParam("id_token", idTokenString)
                    .build())
                .retrieve()
                .body(Map.class);

            if (tokenInfo == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid Google Token"));
            }

            String audience = asString(tokenInfo.get("aud"));
            if (!googleAndroidClientId.equals(audience)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Token audience mismatch"));
            }

            String email = asString(tokenInfo.get("email"));
            String name = asString(tokenInfo.get("name"));

            if (email == null || email.isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Google account email is missing"));
            }

            User user = upsertGoogleUser(email, name);
            return ResponseEntity.ok(new AuthResponse(
                    true,
                    "Login successful",
                    user.getFullName(),
                    user.getEmail()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error verifying Google Token: " + e.getMessage()));
        }
    }

    @GetMapping("/login-success")
    public ResponseEntity<Void> loginSuccess(@AuthenticationPrincipal OAuth2User oauthUser) {
        if (oauthUser == null) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/login?google=error"))
                    .build();
        }
    
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");

        if (email == null || email.isBlank()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/login?google=error"))
                    .build();
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String resolvedName = (name == null || name.isBlank()) ? "Google User" : name;

        User user = upsertGoogleUser(normalizedEmail, resolvedName);

        boolean profileNeedsUpdate = GOOGLE_AUTH_PASSWORD.equalsIgnoreCase(user.getPasswordHash())
                && (isNotSetValue(user.getUniversity()) || isNotSetValue(user.getMajor()));

        URI successRedirect = UriComponentsBuilder
            .fromUriString(frontendUrl + "/login")
            .queryParam("google", "success")
            .queryParam("email", normalizedEmail)
            .queryParam("fullName", resolvedName)
            .queryParam("profile", profileNeedsUpdate ? "required" : "ok")
            .build()
            .encode()
            .toUri();

        return ResponseEntity.status(HttpStatus.FOUND)
            .location(successRedirect)
                .build();
    }

    private User upsertGoogleUser(String email, String name) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String resolvedName = (name == null || name.isBlank()) ? "Google User" : name;

        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null) {
            user = new User();
            user.setEmail(normalizedEmail);
            user.setFullName(resolvedName);
            user.setUniversity(NOT_SET);
            user.setMajor(NOT_SET);
            user.setPasswordHash(GOOGLE_AUTH_PASSWORD);
            user.setCreatedAt(LocalDateTime.now());
            return userRepository.save(user);
        }

        boolean updated = false;

        if (isNotSetValue(user.getUniversity())) {
            user.setUniversity(NOT_SET);
            updated = true;
        }

        if (isNotSetValue(user.getMajor())) {
            user.setMajor(NOT_SET);
            updated = true;
        }

        if (updated) {
            return userRepository.save(user);
        }

        return user;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean isNotSetValue(String value) {
        if (value == null) {
            return true;
        }

        String normalized = value.trim();
        return normalized.isEmpty()
                || "not set".equalsIgnoreCase(normalized)
                || "google oauth".equalsIgnoreCase(normalized);
    }
}