package edu.cit.libetario.peerprep.service;

import edu.cit.libetario.peerprep.dto.AuthResponse;
import edu.cit.libetario.peerprep.dto.LoginRequest;
import edu.cit.libetario.peerprep.dto.RegisterRequest;
import edu.cit.libetario.peerprep.entity.User;
import edu.cit.libetario.peerprep.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(normalizedEmail);
        user.setUniversity(request.getUniversity().trim());
        user.setMajor(request.getMajor().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);
        return new AuthResponse(true, "Registration successful");
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        boolean validPassword = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
        if (!validPassword) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        return new AuthResponse(true, "Login successful");
    }
}
