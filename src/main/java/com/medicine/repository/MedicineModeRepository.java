package com.medicine.repository;

import com.medicine.model.MedicineMode;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicineModeRepository extends CrudRepository<MedicineMode, String> {
}
