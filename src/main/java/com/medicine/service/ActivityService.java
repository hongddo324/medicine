package com.medicine.service;

import com.medicine.model.Activity;
import com.medicine.model.User;
import com.medicine.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;

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

        return activityRepository.save(activity);
    }

    /**
     * 최근 활동 조회 (최대 20개)
     */
    public List<Activity> getRecentActivities() {
        return activityRepository.findTop20ByOrderByCreatedAtDesc();
    }

    /**
     * 특정 기간 이후의 활동 조회
     */
    public List<Activity> getActivitiesSince(LocalDateTime since) {
        return activityRepository.findRecentActivities(since);
    }

    /**
     * 읽지 않은 활동 개수
     */
    public long getUnreadCount() {
        return activityRepository.countByIsReadFalse();
    }

    /**
     * 읽지 않은 활동 조회
     */
    public List<Activity> getUnreadActivities() {
        return activityRepository.findByIsReadFalseOrderByCreatedAtDesc();
    }

    /**
     * 활동 읽음 처리
     */
    @Transactional
    public void markAsRead(Long activityId) {
        activityRepository.findById(activityId).ifPresent(activity -> {
            activity.setIsRead(true);
            activityRepository.save(activity);
        });
    }

    /**
     * 모든 활동 읽음 처리
     */
    @Transactional
    public void markAllAsRead() {
        List<Activity> unreadActivities = activityRepository.findByIsReadFalseOrderByCreatedAtDesc();
        unreadActivities.forEach(activity -> activity.setIsRead(true));
        activityRepository.saveAll(unreadActivities);
    }
}
