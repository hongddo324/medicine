package com.medicine.service;

import com.medicine.model.Role;
import com.medicine.model.User;
import com.medicine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @PostConstruct
    public void initDefaultUsers() {
        // Create default users if not exist
        createUserIfNotExists("admin", "admin123", Role.ADMIN, "관리자");
        createUserIfNotExists("father", "father123", Role.FATHER, "아버지");
        createUserIfNotExists("family", "family123", Role.FAMILY, "가족");
        createUserIfNotExists("guest", "guest123", Role.OTHER, "게스트");

        log.info("Default users initialized");
    }

    private void createUserIfNotExists(String username, String password, Role role, String displayName) {
        Optional<User> existingUser = userRepository.findByUsername(username);
        if (existingUser.isEmpty()) {
            User user = new User();
            user.setId(UUID.randomUUID().toString());
            user.setUsername(username);
            user.setPassword(password);
            user.setRole(role);
            user.setDisplayName(displayName);
            userRepository.save(user);
            log.debug("Created user: {}", username);
        }
    }

    public Optional<User> authenticate(String username, String password) {
        log.debug("Attempting authentication for user: {}", username);
        Optional<User> user = userRepository.findByUsername(username);

        if (user.isPresent() && user.get().getPassword().equals(password)) {
            log.debug("Authentication successful for user: {}", username);
            return user;
        }

        log.debug("Authentication failed for user: {}", username);
        return Optional.empty();
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        userRepository.findAll().forEach(users::add);
        return users;
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
    }
}
