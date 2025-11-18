package com.medicine.controller;

import com.medicine.model.PointItem;
import com.medicine.model.Role;
import com.medicine.model.User;
import com.medicine.service.FileStorageService;
import com.medicine.service.PointItemService;
import com.medicine.service.PointService;
import com.medicine.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private final PointItemService pointItemService;
    private final UserService userService;
    private final FileStorageService fileStorageService;

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

    /**
     * 모든 포인트 아이템 조회 (관리자용)
     */
    @GetMapping("/admin/items")
    public ResponseEntity<?> getAllItems(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        if (user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 필요합니다."));
        }

        List<PointItem> items = pointItemService.getAllItems();
        return ResponseEntity.ok(items);
    }

    /**
     * 포인트 아이템 생성 (관리자용)
     */
    @PostMapping("/admin/items")
    public ResponseEntity<?> createItem(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam Integer points,
            @RequestParam(required = false) String icon,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) MultipartFile image,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        if (user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 필요합니다."));
        }

        try {
            PointItem item = new PointItem();
            item.setName(name);
            item.setDescription(description);
            item.setPoints(points);
            item.setIcon(icon);
            item.setColor(color);
            item.setAvailable(true);

            // 이미지 업로드 처리
            if (image != null && !image.isEmpty()) {
                String imageUrl = fileStorageService.storePointItemImage(image);
                item.setImageUrl(imageUrl);
            }

            PointItem created = pointItemService.createItem(item);
            log.info("Point item created by admin - Name: {}, Points: {}", created.getName(), created.getPoints());

            return ResponseEntity.ok(Map.of("success", true, "item", created));
        } catch (Exception e) {
            log.error("Failed to create point item", e);
            return ResponseEntity.status(500).body(Map.of("error", "상품 등록에 실패했습니다."));
        }
    }

    /**
     * 포인트 아이템 수정 (관리자용)
     */
    @PutMapping("/admin/items/{itemId}")
    public ResponseEntity<?> updateItem(
            @PathVariable Long itemId,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam Integer points,
            @RequestParam(required = false) String icon,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) MultipartFile image,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        if (user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 필요합니다."));
        }

        try {
            PointItem existingItem = pointItemService.getItemById(itemId)
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

            existingItem.setName(name);
            existingItem.setDescription(description);
            existingItem.setPoints(points);
            existingItem.setIcon(icon);
            existingItem.setColor(color);
            if (available != null) {
                existingItem.setAvailable(available);
            }

            // 이미지 업로드 처리
            if (image != null && !image.isEmpty()) {
                String imageUrl = fileStorageService.storePointItemImage(image);
                existingItem.setImageUrl(imageUrl);
            }

            PointItem updated = pointItemService.updateItem(itemId, existingItem);
            log.info("Point item updated by admin - ID: {}, Name: {}", itemId, updated.getName());

            return ResponseEntity.ok(Map.of("success", true, "item", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update point item", e);
            return ResponseEntity.status(500).body(Map.of("error", "상품 수정에 실패했습니다."));
        }
    }

    /**
     * 포인트 아이템 삭제 (관리자용)
     */
    @DeleteMapping("/admin/items/{itemId}")
    public ResponseEntity<?> deleteItem(@PathVariable Long itemId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        if (user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 필요합니다."));
        }

        try {
            pointItemService.deleteItem(itemId);
            log.info("Point item deleted by admin - ID: {}", itemId);

            return ResponseEntity.ok(Map.of("success", true, "message", "상품이 삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete point item", e);
            return ResponseEntity.status(500).body(Map.of("error", "상품 삭제에 실패했습니다."));
        }
    }
}
