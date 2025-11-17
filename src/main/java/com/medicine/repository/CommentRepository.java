package com.medicine.repository;

import com.medicine.model.Comment;
import com.medicine.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 최상위 댓글만 조회 (대댓글 제외)
    Page<Comment> findByParentCommentIsNullOrderByCreatedAtDesc(Pageable pageable);

    // 특정 댓글의 대댓글 조회
    List<Comment> findByParentCommentOrderByCreatedAtAsc(Comment parentComment);

    // 특정 사용자의 댓글 조회
    List<Comment> findByUserOrderByCreatedAtDesc(User user);

    // 특정 시간 이후 생성된 댓글 조회 (NEW 뱃지용)
    @Query("SELECT c FROM Comment c WHERE c.createdAt > :since ORDER BY c.createdAt DESC")
    List<Comment> findRecentComments(LocalDateTime since);
}
