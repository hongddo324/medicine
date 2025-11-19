package com.medicine.service;

import com.medicine.model.Activity;
import com.medicine.model.Comment;
import com.medicine.model.User;
import com.medicine.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final ActivityService activityService;

    public List<Comment> getAllComments() {
        List<Comment> comments = new ArrayList<>();
        commentRepository.findAll().forEach(comments::add);

        // 최신순 정렬
        return comments.stream()
                .sorted(Comparator.comparing(Comment::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public Map<String, List<Comment>> getCommentsGrouped() {
        List<Comment> allComments = getAllComments();

        // 부모 댓글만 필터링
        List<Comment> parentComments = allComments.stream()
                .filter(c -> c.getParentComment() == null)
                .collect(Collectors.toList());

        Map<String, List<Comment>> result = new LinkedHashMap<>();
        result.put("comments", parentComments);
        result.put("replies", allComments.stream()
                .filter(c -> c.getParentComment() != null)
                .collect(Collectors.toList()));

        return result;
    }

    public Comment createComment(String content, String imageData, User user, Long parentCommentId) {
        Comment comment = new Comment();
        comment.setContent(content);
        comment.setImageUrl(imageData);
        comment.setUser(user);

        // 부모 댓글 설정 (대댓글인 경우)
        if (parentCommentId != null) {
            commentRepository.findById(parentCommentId).ifPresent(comment::setParentComment);
        }

        comment.setLikedUserIds(new HashSet<>());

        Comment saved = commentRepository.save(comment);
        log.debug("Comment created: {} by user: {}", saved.getId(), user.getUsername());

        // 활동 기록 생성
        try {
            String message = parentCommentId != null
                ? user.getDisplayName() + "님이 응원메시지에 답글을 남겼습니다"
                : user.getDisplayName() + "님이 응원메시지를 남겼습니다";
            Activity.ActivityType activityType = parentCommentId != null
                ? Activity.ActivityType.COMMENT_REPLY
                : Activity.ActivityType.COMMENT;
            activityService.createActivity(user, activityType, message, saved.getId());
        } catch (Exception e) {
            log.error("Failed to create activity for comment", e);
        }

        return saved;
    }

    public Optional<Comment> findById(Long commentId) {
        return commentRepository.findById(commentId);
    }

    public Comment toggleLike(Long commentId, Long userId) {
        Optional<Comment> optComment = commentRepository.findById(commentId);
        if (optComment.isPresent()) {
            Comment comment = optComment.get();
            comment.toggleLike(userId);
            return commentRepository.save(comment);
        }
        return null;
    }

    public void deleteComment(Long commentId) {
        // 대댓글도 함께 삭제
        Optional<Comment> optComment = commentRepository.findById(commentId);
        if (optComment.isPresent()) {
            Comment comment = optComment.get();

            // 해당 댓글의 모든 대댓글 찾기 및 삭제
            List<Comment> replies = commentRepository.findByParentCommentOrderByCreatedAtAsc(comment);
            replies.forEach(reply -> {
                commentRepository.deleteById(reply.getId());
                log.debug("Reply deleted: {}", reply.getId());
            });

            // 부모 댓글 삭제
            commentRepository.deleteById(commentId);
            log.debug("Comment deleted: {}", commentId);
        }
    }
}
