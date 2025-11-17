package com.medicine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * FCM 토큰 저장 모델
 * Redis에 저장되어 사용자별 푸시 알림 전송에 사용됨
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FcmToken implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 사용자 ID (username)
     */
    private String userId;

    /**
     * FCM 등록 토큰
     */
    private String token;

    /**
     * 토큰 등록 시간 (밀리초)
     */
    private Long registeredAt;

    /**
     * 마지막 사용 시간 (밀리초)
     */
    private Long lastUsedAt;

    public FcmToken(String userId, String token) {
        this.userId = userId;
        this.token = token;
        this.registeredAt = System.currentTimeMillis();
        this.lastUsedAt = System.currentTimeMillis();
    }
}
