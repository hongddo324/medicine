package com.medicine.repository;

import com.medicine.model.DailyComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DailyCommentRepository extends JpaRepository<DailyComment, Long> {

    List<DailyComment> findByDailyIdOrderByCreatedAtAsc(Long dailyId);

    List<DailyComment> findByParentCommentIdOrderByCreatedAtAsc(Long parentCommentId);
}
