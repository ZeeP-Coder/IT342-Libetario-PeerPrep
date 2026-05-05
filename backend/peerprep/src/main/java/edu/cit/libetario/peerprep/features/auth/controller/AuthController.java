package edu.cit.libetario.peerprep.features.auth.controller;

import edu.cit.libetario.peerprep.features.auth.dto.AuthResponse;
import edu.cit.libetario.peerprep.features.auth.dto.LoginRequest;
import edu.cit.libetario.peerprep.features.auth.dto.RegisterRequest;
import edu.cit.libetario.peerprep.features.auth.service.AuthService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;
    private final String frontendUrl;

    public AuthController(
            AuthService authService,
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.authService = authService;
        this.clientRegistrationRepositoryProvider = clientRegistrationRepositoryProvider;
        this.frontendUrl = frontendUrl;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/google")
    public ResponseEntity<AuthResponse> googleAuth() {
        if (clientRegistrationRepositoryProvider.getIfAvailable() == null) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/login?google=not-configured"))
                    .build();
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/oauth2/authorization/google"))
                .build();
    }
}