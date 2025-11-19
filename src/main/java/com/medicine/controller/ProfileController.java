package com.medicine.controller;

import com.medicine.model.User;
import com.medicine.service.FileStorageService;
import com.medicine.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final FileStorageService fileStorageService;

    @GetMapping("/profile")
    public String profilePage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", user);
        log.debug("Profile page accessed by user: {}", user.getUsername());
        return "profile";
    }

    @PostMapping("/api/profile/update")
    @ResponseBody
    public ResponseEntity<?> updateProfile(
            @RequestParam String displayName,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) MultipartFile profileImage,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        try {
            // Get the latest user data from database
            User updatedUser = userService.findByUsername(user.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String oldDisplayName = updatedUser.getDisplayName();
            String oldProfileImage = updatedUser.getProfileImage();
            boolean passwordChanged = false;

            // Update display name
            if (displayName != null && !displayName.trim().isEmpty()) {
                updatedUser.setDisplayName(displayName.trim());
                log.info("Display name updated - User: {}, Old: {}, New: {}",
                        user.getUsername(), oldDisplayName, displayName.trim());
            }

            // Update password
            if (password != null && !password.isEmpty()) {
                if (password.length() < 6) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "비밀번호는 6자 이상이어야 합니다"));
                }
                updatedUser.setPassword(password);
                passwordChanged = true;
                log.info("Password updated - User: {}", user.getUsername());
            }

            // Update profile image
            if (profileImage != null && !profileImage.isEmpty()) {
                // Delete old profile image if exists
                if (oldProfileImage != null && !oldProfileImage.isEmpty()
                        && oldProfileImage.startsWith("/files/")) {
                    fileStorageService.deleteFile(oldProfileImage);
                }

                // Store new profile image
                String imagePath = fileStorageService.storeProfileImage(profileImage, user.getId().toString());
                updatedUser.setProfileImage(imagePath);
                updatedUser.setProfileImageUpdatedAt(LocalDateTime.now());  // 프로필 사진 업데이트 시각 기록
                log.info("Profile image updated - User: {}, Path: {}, Size: {} bytes",
                        user.getUsername(), imagePath, profileImage.getSize());
            }

            // Save user
            userService.save(updatedUser);

            // Update session
            session.setAttribute("user", updatedUser);

            // Log the update
            StringBuilder changeLog = new StringBuilder("Profile updated - User: " + user.getUsername());
            changeLog.append(", Changes: [");
            if (!oldDisplayName.equals(displayName)) {
                changeLog.append("displayName: ").append(oldDisplayName).append("->").append(displayName);
            }
            if (passwordChanged) {
                if (changeLog.charAt(changeLog.length() - 1) != '[') {
                    changeLog.append(", ");
                }
                changeLog.append("password: changed");
            }
            if (profileImage != null && !profileImage.isEmpty()) {
                if (changeLog.charAt(changeLog.length() - 1) != '[') {
                    changeLog.append(", ");
                }
                changeLog.append("profileImage: updated");
            }
            changeLog.append("]");
            log.info(changeLog.toString());

            return ResponseEntity.ok(Map.of("success", true, "user", updatedUser));

        } catch (Exception e) {
            log.error("Failed to update profile for user: {}", user.getUsername(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "프로필 업데이트 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
