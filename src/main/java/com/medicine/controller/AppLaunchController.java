package com.medicine.controller;

import com.medicine.model.User;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * PWA 앱 실행을 위한 공개 진입점 컨트롤러
 * SNS 공유 링크에서 PWA를 실행할 때 사용
 */
@Slf4j
@Controller
public class AppLaunchController {

    /**
     * PWA 앱 실행 페이지
     * 인증 없이 접근 가능하며, PWA 앱을 실행하거나 로그인을 유도함
     */
    @GetMapping("/app/launch")
    public String launchApp(@RequestParam(required = false) String tab,
                           @RequestParam(required = false) Long postId,
                           @RequestParam(required = false) String redirect,
                           HttpSession session,
                           Model model) {

        User user = (User) session.getAttribute("user");
        boolean isLoggedIn = user != null;

        // 리다이렉트 URL 구성
        String targetUrl = "/";
        if (tab != null) {
            targetUrl += "?tab=" + tab;
            if (postId != null) {
                targetUrl += "&postId=" + postId;
            }
        } else if (redirect != null) {
            targetUrl = redirect;
        }

        // 이미 로그인된 상태면 바로 리다이렉트
        if (isLoggedIn) {
            log.debug("User already logged in, redirecting to: {}", targetUrl);
            return "redirect:" + targetUrl;
        }

        model.addAttribute("targetUrl", targetUrl);
        model.addAttribute("tab", tab);
        model.addAttribute("postId", postId);
        model.addAttribute("isLoggedIn", isLoggedIn);

        return "app-launch";
    }
}
