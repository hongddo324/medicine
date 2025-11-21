package com.medicine.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 앱 버전 관리 API 컨트롤러
 * PWA 클라이언트가 서버 버전을 확인하여 업데이트 여부를 판단
 */
@Slf4j
@RestController
@RequestMapping("/api/app")
public class AppVersionController {

    @Value("${app.version:1.0.0}")
    private String appVersion;

    /**
     * 현재 앱 버전 조회
     * 인증 없이 호출 가능한 공개 API
     */
    @GetMapping("/version")
    public ResponseEntity<Map<String, String>> getVersion() {
        log.debug("App version check requested - current version: {}", appVersion);
        return ResponseEntity.ok(Map.of("version", appVersion));
    }
}
