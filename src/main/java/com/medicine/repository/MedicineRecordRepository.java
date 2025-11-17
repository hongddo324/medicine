package com.medicine.repository;

import com.medicine.model.MedicineRecord;
import com.medicine.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicineRecordRepository extends JpaRepository<MedicineRecord, Long> {

    List<MedicineRecord> findByDateOrderByMedicineTypeAsc(LocalDate date);

    Optional<MedicineRecord> findByDateAndMedicineType(LocalDate date, MedicineRecord.MedicineType medicineType);

    List<MedicineRecord> findByTakenByOrderByDateDesc(User user);

    List<MedicineRecord> findByDateBetweenOrderByDateDescMedicineTypeAsc(LocalDate startDate, LocalDate endDate);

    List<MedicineRecord> findByDateAndTakenTrue(LocalDate date);
}
