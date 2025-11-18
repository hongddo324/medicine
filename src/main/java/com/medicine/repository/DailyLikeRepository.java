package com.medicine.repository;

import com.medicine.model.DailyLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DailyLikeRepository extends JpaRepository<DailyLike, Long> {

    Optional<DailyLike> findByDailyIdAndUserId(Long dailyId, Long userId);

    boolean existsByDailyIdAndUserId(Long dailyId, Long userId);

    void deleteByDailyIdAndUserId(Long dailyId, Long userId);
}
