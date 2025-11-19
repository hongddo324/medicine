package com.medicine.repository;

import com.medicine.model.Daily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DailyRepository extends JpaRepository<Daily, Long> {

    List<Daily> findAllByOrderByCreatedAtDesc();

    List<Daily> findByUserIdOrderByCreatedAtDesc(Long userId);
}
