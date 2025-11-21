package com.medicine.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "comments", indexes = {
    @Index(name = "idx_comment_user_id", columnList = "user_id"),
    @Index(name = "idx_comment_created_at", columnList = "created_at")
})
public class Comment implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;  // 댓글 내용

    @Column(columnDefinition = "TEXT")
    private String imageUrl;  // 첨부 이미지 (Base64)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // 작성자

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;  // 작성 시간

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;  // 대댓글인 경우 부모 댓글

    @ElementCollection
    @CollectionTable(name = "comment_likes", joinColumns = @JoinColumn(name = "comment_id"))
    @Column(name = "user_id")
    private Set<Long> likedUserIds = new HashSet<>();  // 좋아요 누른 사용자 ID 목록

    public int getLikesCount() {
        return likedUserIds != null ? likedUserIds.size() : 0;
    }

    public boolean isLikedBy(Long userId) {
        return likedUserIds != null && likedUserIds.contains(userId);
    }

    public void toggleLike(Long userId) {
        if (likedUserIds == null) {
            likedUserIds = new HashSet<>();
        }

        if (likedUserIds.contains(userId)) {
            likedUserIds.remove(userId);
        } else {
            likedUserIds.add(userId);
        }
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
