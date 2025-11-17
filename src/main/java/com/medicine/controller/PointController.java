package com.medicine.controller;

import com.medicine.model.PointItem;
import com.medicine.model.User;
import com.medicine.service.PointService;
import com.medicine.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;
    private final UserService userService;

    /**
     * 포인트 아이템 목록 조회
     */
    @GetMapping("/items")
    public ResponseEntity<List<PointItem>> getPointItems() {
        List<PointItem> items = pointService.getAvailableItems();
        return ResponseEntity.ok(items);
    }

    /**
     * 포인트 아이템 구매
     */
    @PostMapping("/purchase/{itemId}")
    public ResponseEntity<?> purchaseItem(@PathVariable Long itemId, HttpSession session) {
        try {
            String username = (String) session.getAttribute("username");
            if (username == null) {
                return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
            }

            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            pointService.purchaseItem(user, itemId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("remainingPoints", user.getPoints());
            response.put("message", "구매가 완료되었습니다.");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("포인트 아이템 구매 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "구매에 실패했습니다."));
        }
    }
}
