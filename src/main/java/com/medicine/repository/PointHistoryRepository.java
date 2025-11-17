package com.medicine.repository;

import com.medicine.model.PointHistory;
import com.medicine.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    List<PointHistory> findByUserOrderByCreatedAtDesc(User user);

    @Query("SELECT SUM(ph.points) FROM PointHistory ph WHERE ph.user = :user")
    Integer getTotalPointsByUser(@Param("user") User user);

    @Query("SELECT ph FROM PointHistory ph WHERE ph.user = :user AND ph.createdAt >= :startDate ORDER BY ph.createdAt DESC")
    List<PointHistory> findByUserAndCreatedAtAfter(@Param("user") User user, @Param("startDate") LocalDateTime startDate);
}
