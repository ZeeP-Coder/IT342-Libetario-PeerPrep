package edu.cit.libetario.peerprep.controller;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import edu.cit.libetario.peerprep.entity.User;
import edu.cit.libetario.peerprep.repository.UserRepository;

@RestController
@RequestMapping("/api/auth")
public class OAuthController {

    private static final String GOOGLE_AUTH_PASSWORD = "GOOGLE_AUTH";
    private static final String NOT_SET = "Not Set";

    private final UserRepository userRepository;
    private final String frontendUrl;

    public OAuthController(UserRepository userRepository, @Value("${app.frontend-url}") String frontendUrl) {
        this.userRepository = userRepository;
        this.frontendUrl = frontendUrl;
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

        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null) {
            user = new User();
            user.setEmail(normalizedEmail);
            user.setFullName(resolvedName);
            user.setUniversity(NOT_SET);
            user.setMajor(NOT_SET);
            user.setPasswordHash(GOOGLE_AUTH_PASSWORD);
            user.setCreatedAt(LocalDateTime.now());
            userRepository.save(user);
        } else {
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
                userRepository.save(user);
            }
        }

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