package com.medicine.repository;

import com.medicine.model.Wish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WishRepository extends JpaRepository<Wish, Long> {

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
