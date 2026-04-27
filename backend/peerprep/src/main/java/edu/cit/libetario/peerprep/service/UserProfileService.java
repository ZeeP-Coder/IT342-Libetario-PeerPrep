package edu.cit.libetario.peerprep.service;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.cit.libetario.peerprep.dto.UpdateUserProfileRequest;
import edu.cit.libetario.peerprep.dto.UserProfileResponse;
import edu.cit.libetario.peerprep.entity.User;
import edu.cit.libetario.peerprep.repository.UserRepository;

@Service
public class UserProfileService {

    private static final String GOOGLE_AUTH_PASSWORD = "GOOGLE_AUTH";

    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserProfileResponse getProfile(String email) {
        User user = findByEmail(email);
        return toResponse(user);
    }

    public UserProfileResponse updateProfile(UpdateUserProfileRequest request) {
        User user = findByEmail(request.email());

        user.setFullName(request.fullName().trim());
        user.setUniversity(request.university().trim());
        user.setMajor(request.major().trim());

        User savedUser = userRepository.save(user);
        return toResponse(savedUser);
    }

    private User findByEmail(String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }

        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private UserProfileResponse toResponse(User user) {
        boolean googleAuth = GOOGLE_AUTH_PASSWORD.equalsIgnoreCase(user.getPasswordHash());
        return new UserProfileResponse(
                user.getFullName(),
                user.getEmail(),
                user.getUniversity(),
                user.getMajor(),
                googleAuth);
    }
}
