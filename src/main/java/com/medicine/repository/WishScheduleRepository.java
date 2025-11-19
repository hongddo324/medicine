package com.medicine.repository;

import com.medicine.model.WishSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WishScheduleRepository extends JpaRepository<WishSchedule, Long> {

    /**
     * 특정 위시의 모든 일정 조회
     */
    List<WishSchedule> findByWishIdOrderByScheduledDateAsc(Long wishId);

    /**
     * 특정 날짜 범위의 일정 조회
     */
    @Query("SELECT ws FROM WishSchedule ws WHERE ws.scheduledDate BETWEEN :startDate AND :endDate ORDER BY ws.scheduledDate ASC")
    List<WishSchedule> findByScheduledDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 특정 사용자의 일정 조회 (위시의 사용자 기준)
     */
    @Query("SELECT ws FROM WishSchedule ws WHERE ws.wish.user.id = :userId ORDER BY ws.scheduledDate ASC")
    List<WishSchedule> findByUserId(@Param("userId") Long userId);

    /**
     * 특정 사용자의 특정 날짜 범위 일정 조회
     */
    @Query("SELECT ws FROM WishSchedule ws WHERE ws.wish.user.id = :userId AND ws.scheduledDate BETWEEN :startDate AND :endDate ORDER BY ws.scheduledDate ASC")
    List<WishSchedule> findByUserIdAndScheduledDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 완료되지 않은 일정 조회
     */
    List<WishSchedule> findByCompletedFalseOrderByScheduledDateAsc();
}
