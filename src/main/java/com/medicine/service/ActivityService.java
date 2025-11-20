package com.medicine.service;

import com.medicine.model.Activity;
import com.medicine.model.User;
import com.medicine.repository.ActivityReadStatusRepository;
import com.medicine.repository.ActivityRepository;
import com.medicine.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final PushNotificationService pushNotificationService;

    /**
     * 활동 생성 (수신자 지정)
     */
    @Transactional
    public Activity createActivity(User actor, User recipient, Activity.ActivityType activityType, String message, Long referenceId) {
        // 자기 자신에게는 알림을 보내지 않음
        if (actor.getId().equals(recipient.getId())) {
            log.debug("Skipping self-notification for user {}", actor.getId());
            return null;
        }

        Activity activity = new Activity();
        activity.setUser(actor);
        activity.setRecipient(recipient);
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

        // FCM Push 알림 전송
        try {
            sendPushNotification(recipient, activityType, message, referenceId);
        } catch (Exception e) {
            log.error("Failed to send push notification for activity {}", saved.getId(), e);
        }

        return saved;
    }

    /**
     * 활동 생성 (모든 사용자에게 알림 전송 - 일상 게시글, 댓글 등)
     */
    @Transactional
    public void createActivityForAllUsers(User actor, Activity.ActivityType activityType, String message, Long referenceId) {
        try {
            // 모든 사용자 조회
            List<User> allUsers = userRepository.findAll();

            // 각 사용자에게 알림 생성 (자기 자신 제외)
            for (User recipient : allUsers) {
                if (!recipient.getId().equals(actor.getId())) {
                    Activity activity = new Activity();
                    activity.setUser(actor);
                    activity.setRecipient(recipient);
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

                    // FCM Push 알림 전송
                    try {
                        sendPushNotification(recipient, activityType, message, referenceId);
                    } catch (Exception e) {
                        log.error("Failed to send push notification for activity to user {}", recipient.getUsername(), e);
                    }
                }
            }

            log.info("Created {} activities for actor: {}", allUsers.size() - 1, actor.getUsername());
        } catch (Exception e) {
            log.error("Failed to create activities for all users", e);
        }
    }

    /**
     * 활동 생성 (하위 호환성 - deprecated)
     */
    @Deprecated
    @Transactional
    public Activity createActivity(User user, Activity.ActivityType activityType, String message, Long referenceId) {
        // 모든 유저에게 알림 전송으로 변경
        createActivityForAllUsers(user, activityType, message, referenceId);
        return null;
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
     * 사용자별 최근 활동 조회 (수신자 기준, 최대 50개)
     */
    public List<Map<String, Object>> getRecentActivitiesForUser(User recipient) {
        List<Activity> activities = activityRepository.findTop50ByRecipientIdOrderByCreatedAtDesc(recipient.getId());

        return activities.stream()
                .map(activity -> {
                    Map<String, Object> activityMap = new HashMap<>();
                    activityMap.put("id", activity.getId());
                    activityMap.put("message", activity.getMessage());
                    activityMap.put("activityType", activity.getActivityType().name());
                    activityMap.put("referenceId", activity.getReferenceId());
                    activityMap.put("createdAt", activity.getCreatedAt());
                    activityMap.put("isRead", activity.getIsRead());

                    // Actor(수행자) 정보 추가
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
     * 사용자별 읽지 않은 활동 개수 (항상 0 반환 - 읽음=삭제 정책으로 변경됨)
     */
    public long getUnreadCount(User user) {
        return 0; // 읽지 않은 알림 = 존재하는 모든 알림 (읽으면 자동 삭제)
    }

    /**
     * 사용자별 읽지 않은 활동 조회 (수신자 기준 모든 활동)
     */
    public List<Map<String, Object>> getUnreadActivitiesForUser(User recipient) {
        // 읽음 = 삭제이므로, 존재하는 모든 활동이 읽지 않은 활동
        return getRecentActivitiesForUser(recipient);
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
     * 활동 읽음 처리 (읽으면 자동 삭제)
     */
    @Transactional
    public void markAsRead(User user, Long activityId) {
        // 읽음 = 삭제
        activityRepository.deleteByIdAndRecipientId(activityId, user.getId());
        log.debug("Activity {} deleted (read) for user {}", activityId, user.getUsername());
    }

    /**
     * 여러 활동 읽음 처리 (읽으면 자동 삭제)
     */
    @Transactional
    public void markMultipleAsRead(User user, Set<Long> activityIds) {
        for (Long activityId : activityIds) {
            activityRepository.deleteByIdAndRecipientId(activityId, user.getId());
        }
        log.debug("Marked {} activities as read (deleted) for user {}", activityIds.size(), user.getUsername());
    }

    /**
     * 모든 활동 읽음 처리 (모두 삭제)
     */
    @Transactional
    public void markAllAsRead(User user) {
        long count = activityRepository.countByRecipientId(user.getId());
        activityRepository.deleteByRecipientId(user.getId());
        log.info("Deleted all {} activities for user {}", count, user.getUsername());
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
     * 활동 삭제 (수신자별)
     */
    @Transactional
    public void deleteActivity(Long activityId, User recipient) {
        activityRepository.deleteByIdAndRecipientId(activityId, recipient.getId());
        log.info("Activity {} deleted for user {}", activityId, recipient.getUsername());
    }

    /**
     * 모든 활동 삭제 (수신자별)
     */
    @Transactional
    public void deleteAllActivities(User recipient) {
        activityRepository.deleteByRecipientId(recipient.getId());
        log.info("All activities deleted for user {}", recipient.getUsername());
    }

    /**
     * FCM Push 알림 전송 (Activity 타입별로 적절한 제목과 URL 설정)
     */
    private void sendPushNotification(User recipient, Activity.ActivityType activityType, String message, Long referenceId) {
        try {
            // 알림 제목 생성
            String title = getNotificationTitle(activityType);

            // 알림 클릭 시 이동할 URL 생성
            String url = getNotificationUrl(activityType, referenceId);

            // 추가 데이터 설정 (Service Worker에서 사용)
            Map<String, String> data = new HashMap<>();
            data.put("activityType", activityType.name());
            if (referenceId != null) {
                data.put("referenceId", referenceId.toString());
            }
            data.put("type", activityType.name());

            // FCM 푸시 알림 전송
            pushNotificationService.sendNotification(
                    recipient.getUsername(),
                    title,
                    message,
                    url,
                    data
            );

            log.debug("[FCM] Push notification sent - Recipient: {}, Type: {}", recipient.getUsername(), activityType);

        } catch (Exception e) {
            log.error("[FCM] Failed to send push notification - Recipient: {}, Type: {}",
                    recipient.getUsername(), activityType, e);
        }
    }

    /**
     * Activity 타입별 알림 제목 생성
     */
    private String getNotificationTitle(Activity.ActivityType activityType) {
        switch (activityType) {
            case COMMENT:
            case COMMENT_REPLY:
                return "💬 응원 메시지";
            case DAILY_POST:
                return "📸 새 일상 게시";
            case DAILY_COMMENT:
                return "💬 일상 댓글";
            case DAILY_LIKE:
                return "❤️ 일상 좋아요";
            case WISH_ADDED:
                return "⭐ 새 위시 추가";
            case SCHEDULE_ADDED:
                return "📅 새 일정 추가";
            case PROFILE_UPDATED:
                return "👤 프로필 업데이트";
            case MEDICINE_TAKEN:
                return "💊 약 복용 완료";
            case MEAL_UPLOADED:
                return "🍽️ 식단 등록";
            default:
                return "🔔 새 알림";
        }
    }

    /**
     * Activity 타입별 알림 클릭 URL 생성
     */
    private String getNotificationUrl(Activity.ActivityType activityType, Long referenceId) {
        String baseUrl = "/medicine";

        switch (activityType) {
            case WISH_ADDED:
            case SCHEDULE_ADDED:
                return baseUrl + "?tab=wishTab" + (referenceId != null ? "&wishId=" + referenceId : "");

            case DAILY_POST:
            case DAILY_COMMENT:
            case DAILY_LIKE:
                return baseUrl + "?tab=dailyTab" + (referenceId != null ? "&dailyId=" + referenceId : "");

            case MEDICINE_TAKEN:
            case MEAL_UPLOADED:
                return baseUrl + "?tab=healthTab";

            case COMMENT:
            case COMMENT_REPLY:
                return baseUrl + "?tab=homeTab";

            case PROFILE_UPDATED:
                return baseUrl + "?tab=profileTab";

            default:
                return baseUrl + "?tab=activityTab";
        }
    }
}
