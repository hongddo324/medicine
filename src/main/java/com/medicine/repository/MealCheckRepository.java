package com.medicine.repository;

import com.medicine.model.MealCheck;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MealCheckRepository extends CrudRepository<MealCheck, String> {

    List<MealCheck> findByDate(LocalDate date);

    MealCheck findByDateAndMealType(LocalDate date, MealCheck.MealType mealType);
}
