package com.medicine.repository;

import com.medicine.model.PointItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointItemRepository extends JpaRepository<PointItem, Long> {

    List<PointItem> findByAvailableTrueOrderByPointsAsc();

    List<PointItem> findAllByOrderByPointsAsc();
}
