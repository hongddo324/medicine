package com.medicine.service;

import com.medicine.model.Role;
import com.medicine.model.User;
import com.medicine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @PostConstruct
    public void initDefaultUsers() {
        // PostgreSQL에서는 수동으로 계정 생성 필요 (INITIAL_DATA.md 참조)
        log.info("User service initialized. Please create users manually using INITIAL_DATA.md");
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

    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }
}
