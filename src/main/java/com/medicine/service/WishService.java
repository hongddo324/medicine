package com.medicine.service;

import com.medicine.model.Activity;
import com.medicine.model.User;
import com.medicine.model.Wish;
import com.medicine.model.WishSchedule;
import com.medicine.repository.WishRepository;
import com.medicine.repository.WishScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WishService {

    private final WishRepository wishRepository;
    private final WishScheduleRepository wishScheduleRepository;
    private final FileStorageService fileStorageService;
    private final ActivityService activityService;
    private final WebSocketService webSocketService;

    /**
     * 모든 위시리스트 조회
     */
    public List<Wish> getAllWishes() {
        return wishRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * 특정 사용자의 위시리스트 조회
     */
    public List<Wish> getUserWishes(Long userId) {
        return wishRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 위시 ID로 조회
     */
    public Optional<Wish> getWishById(Long wishId) {
        return wishRepository.findById(wishId);
    }

    /**
     * 위시 생성
     */
    @Transactional
    public Wish createWish(User user, String title, String description, Wish.WishCategory category,
                           Double latitude, Double longitude, String address, MultipartFile imageFile, Long dailyId) throws IOException {
        Wish wish = new Wish();
        wish.setUser(user);
        wish.setTitle(title);
        wish.setDescription(description);
        wish.setCategory(category);
        wish.setLatitude(latitude);
        wish.setLongitude(longitude);
        wish.setAddress(address);
        wish.setDailyId(dailyId);

        // 이미지 업로드
        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = fileStorageService.storeWishImage(imageFile);
            wish.setImageUrl(imageUrl);
        }

        Wish saved = wishRepository.save(wish);

        // 활동 기록 생성
        try {
            String message = user.getDisplayName() + "님이 위시를 추가했습니다: " + title;
            activityService.createActivity(user, Activity.ActivityType.WISH_ADDED, message, saved.getId());
        } catch (Exception e) {
            log.error("Failed to create activity for wish", e);
        }

        // WebSocket 실시간 업데이트 전송
        try {
            webSocketService.broadcastWishUpdate(saved, "CREATE");
        } catch (Exception e) {
            log.error("Failed to broadcast wish create via WebSocket", e);
        }

        return saved;
    }

    /**
     * 위시 수정
     */
    @Transactional
    public Wish updateWish(Long wishId, User user, String title, String description, Wish.WishCategory category,
                           Double latitude, Double longitude, String address, MultipartFile imageFile, Long dailyId) throws IOException {
        Wish wish = wishRepository.findById(wishId)
                .orElseThrow(() -> new IllegalArgumentException("위시를 찾을 수 없습니다."));

        // 권한 확인
        if (!wish.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }

        wish.setTitle(title);
        wish.setDescription(description);
        wish.setCategory(category);
        wish.setLatitude(latitude);
        wish.setLongitude(longitude);
        wish.setAddress(address);
        wish.setDailyId(dailyId);

        // 이미지 업로드
        if (imageFile != null && !imageFile.isEmpty()) {
            // 기존 이미지 삭제
            if (wish.getImageUrl() != null) {
                fileStorageService.deleteFile(wish.getImageUrl());
            }
            String imageUrl = fileStorageService.storeWishImage(imageFile);
            wish.setImageUrl(imageUrl);
        }

        Wish updated = wishRepository.save(wish);

        // WebSocket 실시간 업데이트 전송
        try {
            webSocketService.broadcastWishUpdate(updated, "UPDATE");
        } catch (Exception e) {
            log.error("Failed to broadcast wish update via WebSocket", e);
        }

        return updated;
    }

    /**
     * 위시 완료 토글
     */
    @Transactional
    public Wish toggleWishCompletion(Long wishId, User user) {
        Wish wish = wishRepository.findById(wishId)
                .orElseThrow(() -> new IllegalArgumentException("위시를 찾을 수 없습니다."));

        // 권한 확인
        if (!wish.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }

        wish.setCompleted(!wish.getCompleted());
        Wish updated = wishRepository.save(wish);

        // WebSocket 실시간 업데이트 전송
        try {
            webSocketService.broadcastWishUpdate(updated, "UPDATE");
        } catch (Exception e) {
            log.error("Failed to broadcast wish completion toggle via WebSocket", e);
        }

        return updated;
    }

    /**
     * 위시 삭제
     */
    @Transactional
    public void deleteWish(Long wishId, User user) {
        Wish wish = wishRepository.findById(wishId)
                .orElseThrow(() -> new IllegalArgumentException("위시를 찾을 수 없습니다."));

        // 권한 확인
        if (!wish.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        // 이미지 삭제
        if (wish.getImageUrl() != null) {
            fileStorageService.deleteFile(wish.getImageUrl());
        }

        wishRepository.delete(wish);

        // WebSocket 실시간 업데이트 전송
        try {
            webSocketService.broadcastWishUpdate(wish, "DELETE");
        } catch (Exception e) {
            log.error("Failed to broadcast wish delete via WebSocket", e);
        }
    }

    /**
     * 일정 생성
     */
    @Transactional
    public WishSchedule createSchedule(Long wishId, LocalDateTime scheduledDate, String title, String description) {
        Wish wish = wishRepository.findById(wishId)
                .orElseThrow(() -> new IllegalArgumentException("위시를 찾을 수 없습니다."));

        WishSchedule schedule = new WishSchedule();
        schedule.setWish(wish);
        schedule.setScheduledDate(scheduledDate);
        schedule.setTitle(title != null ? title : wish.getTitle());
        schedule.setDescription(description);

        WishSchedule saved = wishScheduleRepository.save(schedule);

        // 활동 기록 생성
        try {
            String message = wish.getUser().getDisplayName() + "님이 일정을 추가했습니다: " + saved.getTitle();
            activityService.createActivity(wish.getUser(), Activity.ActivityType.SCHEDULE_ADDED, message, saved.getId());
        } catch (Exception e) {
            log.error("Failed to create activity for schedule", e);
        }

        return saved;
    }

    /**
     * 특정 위시의 일정 조회
     */
    public List<WishSchedule> getWishSchedules(Long wishId) {
        return wishScheduleRepository.findByWishIdOrderByScheduledDateAsc(wishId);
    }

    /**
     * 특정 사용자의 모든 일정 조회
     */
    public List<WishSchedule> getUserSchedules(Long userId) {
        return wishScheduleRepository.findByUserId(userId);
    }

    /**
     * 특정 날짜 범위의 일정 조회
     */
    public List<WishSchedule> getSchedulesByDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return wishScheduleRepository.findByUserIdAndScheduledDateBetween(userId, startDate, endDate);
    }

    /**
     * 일정 완료 토글
     */
    @Transactional
    public WishSchedule toggleScheduleCompletion(Long scheduleId, User user) {
        WishSchedule schedule = wishScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        // 권한 확인
        if (!schedule.getWish().getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }

        schedule.setCompleted(!schedule.getCompleted());
        return wishScheduleRepository.save(schedule);
    }

    /**
     * 일정 삭제
     */
    @Transactional
    public void deleteSchedule(Long scheduleId, User user) {
        WishSchedule schedule = wishScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        // 권한 확인
        if (!schedule.getWish().getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        wishScheduleRepository.delete(schedule);
    }
}
