package com.medicine.repository;

import com.medicine.model.Wish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishRepository extends JpaRepository<Wish, Long> {

    /**
     * ID로 위시 조회 (User 함께 로드)
     */
    @Query("SELECT w FROM Wish w JOIN FETCH w.user WHERE w.id = :id")
    Optional<Wish> findByIdWithUser(@Param("id") Long id);

    /**
     * 모든 위시리스트를 최신순으로 조회
     */
    List<Wish> findAllByOrderByCreatedAtDesc();

    /**
     * 특정 사용자의 위시리스트를 최신순으로 조회
     */
    List<Wish> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 카테고리별 위시리스트 조회
     */
    List<Wish> findByCategoryOrderByCreatedAtDesc(Wish.WishCategory category);

    /**
     * 완료되지 않은 위시리스트 조회
     */
    List<Wish> findByCompletedFalseOrderByCreatedAtDesc();

    /**
     * 특정 사용자의 완료되지 않은 위시리스트 조회
     */
    List<Wish> findByUserIdAndCompletedFalseOrderByCreatedAtDesc(Long userId);
}
