package com.medicine.service;

import com.medicine.model.Activity;
import com.medicine.model.User;
import com.medicine.repository.ActivityReadStatusRepository;
import com.medicine.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityReadStatusRepository activityReadStatusRepository;
    private final WebSocketService webSocketService;

    /**
     * 활동 생성
     */
    @Transactional
    public Activity createActivity(User user, Activity.ActivityType activityType, String message, Long referenceId) {
        Activity activity = new Activity();
        activity.setUser(user);
        activity.setActivityType(activityType);
        activity.setMessage(message);
        activity.setReferenceId(referenceId);
        activity.setIsRead(false);

        Activity saved = activityRepository.save(activity);

        // WebSocket 실시간 알림 전송
        try {
            webSocketService.broadcastActivity(saved);
        } catch (Exception e) {
            log.error("Failed to broadcast activity via WebSocket", e);
        }

        return saved;
    }

    /**
     * 최근 활동 조회 (최대 20개)
     * @deprecated 사용자별 읽음 상태를 포함하지 않음. getRecentActivitiesForUser 사용 권장
     */
    @Deprecated
    public List<Activity> getRecentActivities() {
        return activityRepository.findTop20ByOrderByCreatedAtDesc();
    }

    /**
     * 사용자별 읽음 상태를 포함한 최근 활동 조회 (최대 50개)
     */
    public List<Map<String, Object>> getRecentActivitiesForUser(User user) {
        List<Activity> activities = activityRepository.findTop50ByOrderByCreatedAtDesc();
        Set<Long> readActivityIds = activityReadStatusRepository.getReadActivityIds(user.getId());

        return activities.stream()
                .map(activity -> {
                    Map<String, Object> activityMap = new HashMap<>();
                    activityMap.put("id", activity.getId());
                    activityMap.put("message", activity.getMessage());
                    activityMap.put("activityType", activity.getActivityType().name());
                    activityMap.put("referenceId", activity.getReferenceId());
                    activityMap.put("createdAt", activity.getCreatedAt());
                    activityMap.put("isRead", readActivityIds.contains(activity.getId()));

                    // User 정보 추가
                    if (activity.getUser() != null) {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("id", activity.getUser().getId());
                        userMap.put("username", activity.getUser().getUsername());
                        userMap.put("displayName", activity.getUser().getDisplayName());
                        userMap.put("profileImage", activity.getUser().getProfileImage());
                        activityMap.put("user", userMap);
                    }

                    return activityMap;
                })
                .collect(Collectors.toList());
    }

    /**
     * 특정 기간 이후의 활동 조회
     */
    public List<Activity> getActivitiesSince(LocalDateTime since) {
        return activityRepository.findRecentActivities(since);
    }

    /**
     * 사용자별 읽지 않은 활동 개수
     */
    public long getUnreadCount(User user) {
        List<Activity> allActivities = activityRepository.findTop50ByOrderByCreatedAtDesc();
        Set<Long> allActivityIds = allActivities.stream()
                .map(Activity::getId)
                .collect(Collectors.toSet());

        return activityReadStatusRepository.getUnreadCount(user.getId(), allActivityIds);
    }

    /**
     * 사용자별 읽지 않은 활동 조회
     */
    public List<Map<String, Object>> getUnreadActivitiesForUser(User user) {
        List<Activity> activities = activityRepository.findTop50ByOrderByCreatedAtDesc();
        Set<Long> readActivityIds = activityReadStatusRepository.getReadActivityIds(user.getId());

        return activities.stream()
                .filter(activity -> !readActivityIds.contains(activity.getId()))
                .map(activity -> {
                    Map<String, Object> activityMap = new HashMap<>();
                    activityMap.put("id", activity.getId());
                    activityMap.put("message", activity.getMessage());
                    activityMap.put("activityType", activity.getActivityType().name());
                    activityMap.put("referenceId", activity.getReferenceId());
                    activityMap.put("createdAt", activity.getCreatedAt());
                    activityMap.put("isRead", false);

                    // User 정보 추가
                    if (activity.getUser() != null) {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("id", activity.getUser().getId());
                        userMap.put("username", activity.getUser().getUsername());
                        userMap.put("displayName", activity.getUser().getDisplayName());
                        userMap.put("profileImage", activity.getUser().getProfileImage());
                        activityMap.put("user", userMap);
                    }

                    return activityMap;
                })
                .collect(Collectors.toList());
    }

    /**
     * 읽지 않은 활동 개수 (deprecated)
     * @deprecated 전역 읽음 상태 사용. getUnreadCount(User) 사용 권장
     */
    @Deprecated
    public long getUnreadCount() {
        return activityRepository.countByIsReadFalse();
    }

    /**
     * 읽지 않은 활동 조회 (deprecated)
     * @deprecated 전역 읽음 상태 사용. getUnreadActivitiesForUser(User) 사용 권장
     */
    @Deprecated
    public List<Activity> getUnreadActivities() {
        return activityRepository.findByIsReadFalseOrderByCreatedAtDesc();
    }

    /**
     * 사용자별 활동 읽음 처리
     */
    public void markAsRead(User user, Long activityId) {
        activityReadStatusRepository.markAsRead(user.getId(), activityId);
        log.debug("Activity {} marked as read for user {}", activityId, user.getUsername());
    }

    /**
     * 사용자별 여러 활동을 한번에 읽음 처리
     */
    public void markMultipleAsRead(User user, Set<Long> activityIds) {
        activityReadStatusRepository.markMultipleAsRead(user.getId(), activityIds);
        log.debug("Marked {} activities as read for user {}", activityIds.size(), user.getUsername());
    }

    /**
     * 사용자별 모든 활동 읽음 처리
     */
    public void markAllAsRead(User user) {
        List<Activity> activities = activityRepository.findTop50ByOrderByCreatedAtDesc();
        Set<Long> activityIds = activities.stream()
                .map(Activity::getId)
                .collect(Collectors.toSet());

        activityReadStatusRepository.markMultipleAsRead(user.getId(), activityIds);
        log.info("Marked all {} activities as read for user {}", activityIds.size(), user.getUsername());
    }

    /**
     * 활동 읽음 처리 (deprecated)
     * @deprecated 전역 읽음 상태 사용. markAsRead(User, Long) 사용 권장
     */
    @Deprecated
    @Transactional
    public void markAsRead(Long activityId) {
        activityRepository.findById(activityId).ifPresent(activity -> {
            activity.setIsRead(true);
            activityRepository.save(activity);
        });
    }

    /**
     * 모든 활동 읽음 처리 (deprecated)
     * @deprecated 전역 읽음 상태 사용. markAllAsRead(User) 사용 권장
     */
    @Deprecated
    @Transactional
    public void markAllAsRead() {
        List<Activity> unreadActivities = activityRepository.findByIsReadFalseOrderByCreatedAtDesc();
        unreadActivities.forEach(activity -> activity.setIsRead(true));
        activityRepository.saveAll(unreadActivities);
    }

    /**
     * 특정 활동이 사용자에게 읽혔는지 확인
     */
    public boolean isReadByUser(User user, Long activityId) {
        return activityReadStatusRepository.isRead(user.getId(), activityId);
    }

    /**
     * 사용자의 모든 읽음 상태 초기화
     */
    public void clearAllReadStatus(User user) {
        activityReadStatusRepository.clearAllReadStatus(user.getId());
        log.info("Cleared all read status for user {}", user.getUsername());
    }

    /**
     * 활동 삭제
     */
    @Transactional
    public void deleteActivity(Long activityId) {
        activityRepository.deleteById(activityId);
        // Redis에서도 삭제
        activityReadStatusRepository.removeActivityFromAllUsers(activityId);
        log.info("Activity {} deleted", activityId);
    }

    /**
     * 모든 활동 삭제
     */
    @Transactional
    public void deleteAllActivities() {
        activityRepository.deleteAll();
        log.info("All activities deleted");
    }
}
