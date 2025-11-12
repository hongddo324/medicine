package com.medicine.repository;

import com.medicine.model.MedicineRecord;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicineRecordRepository extends CrudRepository<MedicineRecord, String> {
    Optional<MedicineRecord> findByDate(LocalDate date);
    List<MedicineRecord> findAllByDateBetween(LocalDate startDate, LocalDate endDate);
}
