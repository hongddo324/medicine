package com.medicine.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis를 사용하여 사용자별 활동 읽음 상태를 관리하는 저장소
 *
 * Key 구조: "activity:read:{userId}"
 * Value: Set<activityId>
 *
 * 이를 통해 각 사용자가 독립적으로 자신의 활동 읽음 상태를 관리할 수 있습니다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ActivityReadStatusRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_PREFIX = "activity:read:";

    /**
     * 특정 사용자의 읽음 상태 키를 생성
     */
    private String getUserKey(Long userId) {
        return KEY_PREFIX + userId;
    }

    /**
     * 사용자가 활동을 읽음으로 표시
     */
    public void markAsRead(Long userId, Long activityId) {
        try {
            String key = getUserKey(userId);
            redisTemplate.opsForSet().add(key, activityId.toString());
            log.debug("Activity {} marked as read for user {}", activityId, userId);
        } catch (Exception e) {
            log.error("Failed to mark activity {} as read for user {}", activityId, userId, e);
        }
    }

    /**
     * 여러 활동을 한번에 읽음으로 표시
     */
    public void markMultipleAsRead(Long userId, Set<Long> activityIds) {
        try {
            if (activityIds == null || activityIds.isEmpty()) {
                return;
            }

            String key = getUserKey(userId);
            String[] values = activityIds.stream()
                    .map(String::valueOf)
                    .toArray(String[]::new);
            redisTemplate.opsForSet().add(key, (Object[]) values);
            log.debug("Marked {} activities as read for user {}", activityIds.size(), userId);
        } catch (Exception e) {
            log.error("Failed to mark multiple activities as read for user {}", userId, e);
        }
    }

    /**
     * 사용자가 특정 활동을 읽었는지 확인
     */
    public boolean isRead(Long userId, Long activityId) {
        try {
            String key = getUserKey(userId);
            Boolean isMember = redisTemplate.opsForSet().isMember(key, activityId.toString());
            return Boolean.TRUE.equals(isMember);
        } catch (Exception e) {
            log.error("Failed to check read status of activity {} for user {}", activityId, userId, e);
            return false;
        }
    }

    /**
     * 사용자가 읽은 모든 활동 ID 조회
     */
    public Set<Long> getReadActivityIds(Long userId) {
        try {
            String key = getUserKey(userId);
            Set<Object> members = redisTemplate.opsForSet().members(key);

            if (members == null || members.isEmpty()) {
                return Set.of();
            }

            return members.stream()
                    .map(obj -> Long.parseLong(obj.toString()))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Failed to get read activity IDs for user {}", userId, e);
            return Set.of();
        }
    }

    /**
     * 사용자의 읽지 않은 활동 개수 조회
     */
    public long getUnreadCount(Long userId, Set<Long> allActivityIds) {
        try {
            Set<Long> readIds = getReadActivityIds(userId);
            return allActivityIds.stream()
                    .filter(id -> !readIds.contains(id))
                    .count();
        } catch (Exception e) {
            log.error("Failed to get unread count for user {}", userId, e);
            return 0;
        }
    }

    /**
     * 특정 활동을 읽지 않음으로 표시 (읽음 취소)
     */
    public void markAsUnread(Long userId, Long activityId) {
        try {
            String key = getUserKey(userId);
            redisTemplate.opsForSet().remove(key, activityId.toString());
            log.debug("Activity {} marked as unread for user {}", activityId, userId);
        } catch (Exception e) {
            log.error("Failed to mark activity {} as unread for user {}", activityId, userId, e);
        }
    }

    /**
     * 사용자의 모든 읽음 상태 초기화
     */
    public void clearAllReadStatus(Long userId) {
        try {
            String key = getUserKey(userId);
            redisTemplate.delete(key);
            log.debug("Cleared all read status for user {}", userId);
        } catch (Exception e) {
            log.error("Failed to clear read status for user {}", userId, e);
        }
    }

    /**
     * 특정 활동을 모든 사용자의 읽음 상태에서 제거 (활동 삭제 시 사용)
     */
    public void removeActivityFromAllUsers(Long activityId) {
        try {
            // 모든 사용자의 읽음 상태 키 조회
            Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                for (String key : keys) {
                    redisTemplate.opsForSet().remove(key, activityId.toString());
                }
                log.debug("Removed activity {} from all users' read status", activityId);
            }
        } catch (Exception e) {
            log.error("Failed to remove activity {} from all users", activityId, e);
        }
    }
}
