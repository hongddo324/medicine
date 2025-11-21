package com.medicine.controller;

import com.medicine.model.Daily;
import com.medicine.model.DailyImage;
import com.medicine.model.User;
import com.medicine.service.DailyService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

/**
 * 일상 게시글 공유용 페이지 컨트롤러
 * OG 메타태그를 포함한 퍼머링크 페이지를 렌더링합니다.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DailyPageController {

    private final DailyService dailyService;

    @Value("${app.base-url:}")
    private String baseUrl;

    /**
     * 일상 게시글 상세 페이지 (공유용 퍼머링크)
     * - 로그인된 사용자: 메인 앱으로 리다이렉트하여 해당 게시글 표시
     * - 비로그인 사용자: OG 태그가 포함된 미리보기 페이지 표시
     */
    @GetMapping("/daily/{dailyId}")
    public String dailyDetail(@PathVariable Long dailyId, HttpSession session, Model model) {
        log.info("Daily detail page accessed - dailyId: {}", dailyId);

        Optional<Daily> dailyOpt = dailyService.getDailyById(dailyId);

        if (dailyOpt.isEmpty()) {
            log.warn("Daily not found - dailyId: {}", dailyId);
            return "redirect:/?error=notfound";
        }

        Daily daily = dailyOpt.get();
        User user = (User) session.getAttribute("user");

        // OG 태그용 데이터 설정
        String ogTitle = daily.getUser().getDisplayName() != null
            ? daily.getUser().getDisplayName() + "님의 일상"
            : "일상 기록";

        String ogDescription = daily.getContent();
        if (ogDescription != null && ogDescription.length() > 100) {
            ogDescription = ogDescription.substring(0, 100) + "...";
        }

        // 대표 이미지 결정 (첫 번째 이미지 또는 기본 이미지)
        String ogImage = "/icons/icon-512x512.png"; // 기본 이미지
        if (daily.getImages() != null && !daily.getImages().isEmpty()) {
            DailyImage firstImage = daily.getImages().stream()
                .filter(img -> "IMAGE".equals(img.getMediaType()))
                .findFirst()
                .orElse(daily.getImages().get(0));
            if (firstImage != null && firstImage.getImageUrl() != null) {
                ogImage = firstImage.getImageUrl();
            }
        } else if (daily.getMediaUrl() != null && !"VIDEO".equals(daily.getMediaType())) {
            ogImage = daily.getMediaUrl();
        }

        // 공유 URL 생성
        String shareUrl = "/daily/" + dailyId;

        model.addAttribute("daily", daily);
        model.addAttribute("dailyId", dailyId);
        model.addAttribute("ogTitle", ogTitle);
        model.addAttribute("ogDescription", ogDescription != null ? ogDescription : "우리 가족의 일상을 확인해보세요!");
        model.addAttribute("ogImage", ogImage);
        model.addAttribute("ogUrl", shareUrl);
        model.addAttribute("user", user);

        // 로그인된 사용자는 앱으로 리다이렉트할 수 있도록 플래그 설정
        model.addAttribute("isLoggedIn", user != null);

        log.debug("Daily detail page rendered - dailyId: {}, ogTitle: {}, ogImage: {}",
            dailyId, ogTitle, ogImage);

        return "daily-detail";
    }
}
