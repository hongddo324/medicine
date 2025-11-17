package com.medicine.repository;

import com.medicine.model.MealCheck;
import com.medicine.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MealCheckRepository extends JpaRepository<MealCheck, Long> {

    List<MealCheck> findByDateOrderByMealTypeAsc(LocalDate date);

    Optional<MealCheck> findByDateAndMealType(LocalDate date, MealCheck.MealType mealType);

    List<MealCheck> findByUploadedByOrderByDateDesc(User user);

    List<MealCheck> findByDateBetweenOrderByDateDescMealTypeAsc(LocalDate startDate, LocalDate endDate);
}
