package com.medicine.service;

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
                .filter(c -> c.getParentCommentId() == null)
                .collect(Collectors.toList());

        // 대댓글 그룹핑
        Map<String, List<Comment>> repliesMap = allComments.stream()
                .filter(c -> c.getParentCommentId() != null)
                .collect(Collectors.groupingBy(Comment::getParentCommentId));

        Map<String, List<Comment>> result = new LinkedHashMap<>();
        result.put("comments", parentComments);
        result.put("replies", allComments.stream()
                .filter(c -> c.getParentCommentId() != null)
                .collect(Collectors.toList()));

        return result;
    }

    public Comment createComment(String content, String imageData, User user, String parentCommentId) {
        Comment comment = new Comment();
        comment.setId(UUID.randomUUID().toString());
        comment.setContent(content);
        comment.setImageUrl(imageData);
        comment.setUserId(user.getId());
        comment.setUsername(user.getUsername());
        comment.setDisplayName(user.getDisplayName());
        comment.setProfileImage(user.getProfileImage());
        comment.setCreatedAt(LocalDateTime.now());
        comment.setParentCommentId(parentCommentId);
        comment.setLikedUserIds(new HashSet<>());

        Comment saved = commentRepository.save(comment);
        log.debug("Comment created: {} by user: {}", saved.getId(), user.getUsername());
        return saved;
    }

    public Optional<Comment> findById(String commentId) {
        return commentRepository.findById(commentId);
    }

    public Comment toggleLike(String commentId, String userId) {
        Optional<Comment> optComment = commentRepository.findById(commentId);
        if (optComment.isPresent()) {
            Comment comment = optComment.get();
            comment.toggleLike(userId);
            return commentRepository.save(comment);
        }
        return null;
    }

    public void deleteComment(String commentId) {
        // 대댓글도 함께 삭제
        List<Comment> allComments = getAllComments();
        List<String> commentIdsToDelete = new ArrayList<>();
        commentIdsToDelete.add(commentId);

        // 해당 댓글의 모든 대댓글 찾기
        allComments.stream()
                .filter(c -> commentId.equals(c.getParentCommentId()))
                .forEach(c -> commentIdsToDelete.add(c.getId()));

        commentIdsToDelete.forEach(id -> {
            commentRepository.deleteById(id);
            log.debug("Comment deleted: {}", id);
        });
    }
}
