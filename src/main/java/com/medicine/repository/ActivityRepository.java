package com.medicine.repository;

import com.medicine.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    // 최신 활동 조회 (최근 순)
    List<Activity> findTop20ByOrderByCreatedAtDesc();

    // 최신 활동 조회 (최대 50개, 사용자별 읽음 상태 관리용)
    List<Activity> findTop50ByOrderByCreatedAtDesc();

    // 읽지 않은 활동 개수
    long countByIsReadFalse();

    // 특정 기간 이후의 활동 조회
    @Query("SELECT a FROM Activity a WHERE a.createdAt > :since ORDER BY a.createdAt DESC")
    List<Activity> findRecentActivities(LocalDateTime since);

    // 읽지 않은 활동 조회
    List<Activity> findByIsReadFalseOrderByCreatedAtDesc();

    // 수신자별 최신 활동 조회 (최대 50개)
    List<Activity> findTop50ByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    // 수신자별 활동 개수
    long countByRecipientId(Long recipientId);

    // 수신자별 활동 삭제
    void deleteByIdAndRecipientId(Long activityId, Long recipientId);

    // 수신자별 모든 활동 삭제
    void deleteByRecipientId(Long recipientId);
}
