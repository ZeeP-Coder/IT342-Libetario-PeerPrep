package edu.cit.libetario.peerprep.controller;

import java.net.URI;
import java.time.LocalDateTime;

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

        if (!userRepository.existsByEmail(email)) {
            User user = new User();
            user.setEmail(email);
            user.setFullName(name == null || name.isBlank() ? "Google User" : name);
            user.setUniversity("Google OAuth");
            user.setMajor("Not set");
            user.setPasswordHash("GOOGLE_AUTH");
            user.setCreatedAt(LocalDateTime.now());
            userRepository.save(user);
        }

        String resolvedName = (name == null || name.isBlank()) ? "Google User" : name;
        URI successRedirect = UriComponentsBuilder
            .fromUriString(frontendUrl + "/login")
            .queryParam("google", "success")
            .queryParam("email", email)
            .queryParam("fullName", resolvedName)
            .build()
            .encode()
            .toUri();

        return ResponseEntity.status(HttpStatus.FOUND)
            .location(successRedirect)
                .build();
    }
}