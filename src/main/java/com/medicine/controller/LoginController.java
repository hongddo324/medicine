package com.medicine.controller;

import com.medicine.model.User;
import com.medicine.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Slf4j
@Controller
@RequiredArgsConstructor
public class LoginController {

    private static final org.slf4j.Logger accessLogger = org.slf4j.LoggerFactory.getLogger("com.medicine.access");
    private final UserService userService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String redirect,
                           HttpSession session,
                           Model model) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            log.debug("User already logged in: {}, redirecting to home", user.getUsername());
            return "redirect:/";
        }
        if (redirect != null && !redirect.isEmpty()) {
            model.addAttribute("redirect", redirect);
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                       @RequestParam String password,
                       @RequestParam(required = false) String redirect,
                       HttpSession session,
                       Model model) {

        accessLogger.debug("Login attempt - Username: {}", username);

        Optional<User> userOpt = userService.authenticate(username, password);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            session.setAttribute("user", user);
            session.setMaxInactiveInterval(8640000); // 100 days

            accessLogger.info("Login successful - Username: {}, Role: {}", username, user.getRole());
            log.debug("User logged in: {} with role: {}", username, user.getRole());

            // 리다이렉트 URL이 있으면 해당 URL로 이동
            if (redirect != null && !redirect.isEmpty() && redirect.startsWith("/")) {
                return "redirect:" + redirect;
            }
            return "redirect:/";
        } else {
            accessLogger.warn("Login failed - Username: {}", username);
            log.debug("Login failed for username: {}", username);

            model.addAttribute("error", "아이디 또는 비밀번호가 잘못되었습니다.");
            model.addAttribute("redirect", redirect);
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            accessLogger.info("Logout - Username: {}", user.getUsername());
            log.debug("User logged out: {}", user.getUsername());
        }
        session.invalidate();
        return "redirect:/login";
    }
}
