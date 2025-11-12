package com.medicine.controller;

import com.medicine.model.Role;
import com.medicine.model.User;
import com.medicine.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @GetMapping
    public String adminPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");

        // 관리자 권한 체크
        if (user == null || user.getRole() != Role.ADMIN) {
            log.warn("Unauthorized access attempt to admin page by user: {}",
                    user != null ? user.getUsername() : "null");
            return "redirect:/";
        }

        // 모든 사용자 목록 조회
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        model.addAttribute("user", user);
        model.addAttribute("roles", Role.values());

        return "admin";
    }

    @PostMapping("/users")
    public String createUser(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String displayName,
            @RequestParam Role role,
            @RequestParam(required = false) MultipartFile profileImage,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User currentUser = (User) session.getAttribute("user");

        // 관리자 권한 체크
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            log.warn("Unauthorized user creation attempt by: {}",
                    currentUser != null ? currentUser.getUsername() : "null");
            redirectAttributes.addFlashAttribute("error", "권한이 없습니다.");
            return "redirect:/";
        }

        try {
            // 중복 사용자명 체크
            if (userService.findByUsername(username).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "이미 존재하는 사용자명입니다.");
                return "redirect:/admin";
            }

            // 프로필 이미지 처리
            String profileImageData = null;
            if (profileImage != null && !profileImage.isEmpty()) {
                try {
                    byte[] imageBytes = profileImage.getBytes();
                    profileImageData = "data:" + profileImage.getContentType() + ";base64,"
                            + Base64.getEncoder().encodeToString(imageBytes);
                } catch (IOException e) {
                    log.error("Failed to process profile image", e);
                }
            }

            // 새 사용자 생성
            User newUser = new User();
            newUser.setId(UUID.randomUUID().toString());
            newUser.setUsername(username);
            newUser.setPassword(password);
            newUser.setDisplayName(displayName);
            newUser.setRole(role);
            newUser.setProfileImage(profileImageData);

            userService.createUser(newUser);

            log.info("New user created: {} by admin: {}", username, currentUser.getUsername());
            redirectAttributes.addFlashAttribute("success", "사용자가 성공적으로 생성되었습니다.");

        } catch (Exception e) {
            log.error("Failed to create user", e);
            redirectAttributes.addFlashAttribute("error", "사용자 생성 중 오류가 발생했습니다.");
        }

        return "redirect:/admin";
    }

    @DeleteMapping("/users/{userId}")
    @ResponseBody
    public String deleteUser(@PathVariable String userId, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");

        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            return "{\"success\": false, \"message\": \"권한이 없습니다.\"}";
        }

        // 자기 자신은 삭제할 수 없음
        if (currentUser.getId().equals(userId)) {
            return "{\"success\": false, \"message\": \"자기 자신은 삭제할 수 없습니다.\"}";
        }

        try {
            userService.deleteUser(userId);
            log.info("User deleted: {} by admin: {}", userId, currentUser.getUsername());
            return "{\"success\": true, \"message\": \"사용자가 삭제되었습니다.\"}";
        } catch (Exception e) {
            log.error("Failed to delete user", e);
            return "{\"success\": false, \"message\": \"삭제 중 오류가 발생했습니다.\"}";
        }
    }
}
