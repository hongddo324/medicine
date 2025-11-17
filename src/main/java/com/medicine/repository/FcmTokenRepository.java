package com.medicine.repository;

import com.medicine.model.FcmToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * FCM 토큰 Redis 저장소
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class FcmTokenRepository {

    private final RedisTemplate<String, FcmToken> fcmTokenRedisTemplate;
    private static final String FCM_TOKEN_KEY_PREFIX = "fcm:token:";
    private static final long TOKEN_EXPIRY_DAYS = 90; // 90일 후 자동 삭제

    /**
     * FCM 토큰 저장
     */
    public void save(FcmToken fcmToken) {
        String key = FCM_TOKEN_KEY_PREFIX + fcmToken.getToken();
        fcmTokenRedisTemplate.opsForValue().set(key, fcmToken, TOKEN_EXPIRY_DAYS, TimeUnit.DAYS);
        log.info("FCM token saved for user: {}", fcmToken.getUserId());
    }

    /**
     * 특정 사용자의 모든 FCM 토큰 조회
     */
    public List<FcmToken> findByUserId(String userId) {
        Set<String> keys = fcmTokenRedisTemplate.keys(FCM_TOKEN_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        return keys.stream()
                .map(key -> fcmTokenRedisTemplate.opsForValue().get(key))
                .filter(Objects::nonNull)
                .filter(token -> userId.equals(token.getUserId()))
                .collect(Collectors.toList());
    }

    /**
     * 모든 FCM 토큰 조회
     */
    public List<FcmToken> findAll() {
        Set<String> keys = fcmTokenRedisTemplate.keys(FCM_TOKEN_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        return keys.stream()
                .map(key -> fcmTokenRedisTemplate.opsForValue().get(key))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * FCM 토큰 삭제
     */
    public void delete(String token) {
        String key = FCM_TOKEN_KEY_PREFIX + token;
        fcmTokenRedisTemplate.delete(key);
        log.info("FCM token deleted: {}", token);
    }

    /**
     * 특정 사용자의 모든 토큰 삭제
     */
    public void deleteByUserId(String userId) {
        List<FcmToken> tokens = findByUserId(userId);
        tokens.forEach(token -> delete(token.getToken()));
        log.info("All FCM tokens deleted for user: {}", userId);
    }

    /**
     * 토큰 존재 여부 확인
     */
    public boolean exists(String token) {
        String key = FCM_TOKEN_KEY_PREFIX + token;
        return Boolean.TRUE.equals(fcmTokenRedisTemplate.hasKey(key));
    }
}
