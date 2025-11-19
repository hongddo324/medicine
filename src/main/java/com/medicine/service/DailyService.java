package com.medicine.service;

import com.medicine.model.Activity;
import com.medicine.model.Daily;
import com.medicine.model.DailyComment;
import com.medicine.model.DailyLike;
import com.medicine.model.User;
import com.medicine.repository.DailyCommentRepository;
import com.medicine.repository.DailyLikeRepository;
import com.medicine.repository.DailyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyService {

    private final DailyRepository dailyRepository;
    private final DailyCommentRepository dailyCommentRepository;
    private final DailyLikeRepository dailyLikeRepository;
    private final FileStorageService fileStorageService;
    private final ActivityService activityService;

    /**
     * 모든 일상 게시물 조회 (최신순)
     */
    public List<Daily> getAllDailies() {
        return dailyRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * 특정 사용자의 일상 게시물 조회
     */
    public List<Daily> getUserDailies(Long userId) {
        return dailyRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 일상 게시물 상세 조회
     */
    public Optional<Daily> getDailyById(Long id) {
        return dailyRepository.findById(id);
    }

    /**
     * 일상 게시물 작성
     */
    @Transactional
    public Daily createDaily(User user, String content, MultipartFile mediaFile) throws IOException {
        Daily daily = new Daily();
        daily.setUser(user);
        daily.setContent(content);

        // 미디어 파일 업로드 처리
        if (mediaFile != null && !mediaFile.isEmpty()) {
            String mediaUrl = fileStorageService.storeDailyMedia(mediaFile);
            daily.setMediaUrl(mediaUrl);

            // 파일 타입 확인
            String contentType = mediaFile.getContentType();
            if (contentType != null) {
                if (contentType.startsWith("image/")) {
                    daily.setMediaType(Daily.MediaType.IMAGE);
                } else if (contentType.startsWith("video/")) {
                    daily.setMediaType(Daily.MediaType.VIDEO);
                }
            }
        }

        Daily saved = dailyRepository.save(daily);
        log.info("Daily post created - User: {}, ID: {}", user.getUsername(), saved.getId());

        // 활동 기록 생성
        try {
            String message = user.getDisplayName() + "님이 일상을 남겼습니다";
            activityService.createActivity(user, Activity.ActivityType.DAILY_POST, message, saved.getId());
        } catch (Exception e) {
            log.error("Failed to create activity for daily post", e);
        }

        return saved;
    }

    /**
     * 일상 게시물 수정
     */
    @Transactional
    public Daily updateDaily(Long id, User user, String content) {
        Daily daily = dailyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다."));

        if (!daily.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }

        daily.setContent(content);
        return dailyRepository.save(daily);
    }

    /**
     * 일상 게시물 삭제
     */
    @Transactional
    public void deleteDaily(Long id, User user) {
        Daily daily = dailyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다."));

        if (!daily.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        // 미디어 파일 삭제
        if (daily.getMediaUrl() != null) {
            fileStorageService.deleteFile(daily.getMediaUrl());
        }

        dailyRepository.delete(daily);
        log.info("Daily post deleted - User: {}, ID: {}", user.getUsername(), id);
    }

    /**
     * 좋아요 토글
     */
    @Transactional
    public boolean toggleLike(Long dailyId, User user) {
        Daily daily = dailyRepository.findById(dailyId)
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다."));

        Optional<DailyLike> existingLike = dailyLikeRepository.findByDailyIdAndUserId(dailyId, user.getId());

        if (existingLike.isPresent()) {
            // 좋아요 취소
            dailyLikeRepository.delete(existingLike.get());
            daily.setLikesCount(Math.max(0, daily.getLikesCount() - 1));
            dailyRepository.save(daily);
            log.info("Daily like removed - User: {}, DailyId: {}", user.getUsername(), dailyId);
            return false;
        } else {
            // 좋아요 추가
            DailyLike like = new DailyLike();
            like.setDaily(daily);
            like.setUser(user);
            dailyLikeRepository.save(like);

            daily.setLikesCount(daily.getLikesCount() + 1);
            dailyRepository.save(daily);
            log.info("Daily like added - User: {}, DailyId: {}", user.getUsername(), dailyId);

            // 활동 기록 생성
            try {
                String message = user.getDisplayName() + "님이 일상에 좋아요를 눌렀습니다";
                activityService.createActivity(user, Activity.ActivityType.DAILY_LIKE, message, dailyId);
            } catch (Exception e) {
                log.error("Failed to create activity for daily like", e);
            }

            return true;
        }
    }

    /**
     * 댓글 추가
     */
    @Transactional
    public DailyComment addComment(Long dailyId, User user, String content, Long parentCommentId) {
        Daily daily = dailyRepository.findById(dailyId)
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다."));

        DailyComment comment = new DailyComment();
        comment.setDaily(daily);
        comment.setUser(user);
        comment.setContent(content);

        // 대댓글인 경우
        if (parentCommentId != null) {
            DailyComment parentComment = dailyCommentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new IllegalArgumentException("부모 댓글을 찾을 수 없습니다."));
            comment.setParentComment(parentComment);
        }

        DailyComment saved = dailyCommentRepository.save(comment);
        log.info("Daily comment added - User: {}, DailyId: {}, ParentCommentId: {}",
                user.getUsername(), dailyId, parentCommentId);

        // 활동 기록 생성
        try {
            String message = user.getDisplayName() + "님이 일상에 댓글을 남겼습니다";
            activityService.createActivity(user, Activity.ActivityType.DAILY_COMMENT, message, dailyId);
        } catch (Exception e) {
            log.error("Failed to create activity for daily comment", e);
        }

        return saved;
    }

    /**
     * 댓글 조회
     */
    public List<DailyComment> getComments(Long dailyId) {
        return dailyCommentRepository.findByDailyIdOrderByCreatedAtAsc(dailyId);
    }

    /**
     * 댓글 삭제
     */
    @Transactional
    public void deleteComment(Long commentId, User user) {
        DailyComment comment = dailyCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!comment.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        dailyCommentRepository.delete(comment);
        log.info("Daily comment deleted - User: {}, CommentId: {}", user.getUsername(), commentId);
    }

    /**
     * 사용자가 좋아요를 눌렀는지 확인
     */
    public boolean isLikedByUser(Long dailyId, Long userId) {
        return dailyLikeRepository.existsByDailyIdAndUserId(dailyId, userId);
    }
}
