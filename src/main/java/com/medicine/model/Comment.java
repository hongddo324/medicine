package com.medicine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("Comment")
public class Comment implements Serializable {

    @Id
    private String id;

    private String content;  // 댓글 내용

    private String imageUrl;  // 첨부 이미지 (Base64)

    @Indexed
    private String userId;  // 작성자 ID

    private String username;  // 작성자 아이디

    private String displayName;  // 작성자 표시 이름

    private String profileImage;  // 작성자 프로필 사진

    private LocalDateTime createdAt;  // 작성 시간

    private String parentCommentId;  // 대댓글인 경우 부모 댓글 ID

    private Set<String> likedUserIds = new HashSet<>();  // 좋아요 누른 사용자 ID 목록

    public int getLikesCount() {
        return likedUserIds != null ? likedUserIds.size() : 0;
    }

    public boolean isLikedBy(String userId) {
        return likedUserIds != null && likedUserIds.contains(userId);
    }

    public void toggleLike(String userId) {
        if (likedUserIds == null) {
            likedUserIds = new HashSet<>();
        }

        if (likedUserIds.contains(userId)) {
            likedUserIds.remove(userId);
        } else {
            likedUserIds.add(userId);
        }
    }
}
