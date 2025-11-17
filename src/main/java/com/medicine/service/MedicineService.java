package com.medicine.service;

import com.medicine.model.MedicineRecord;
import com.medicine.model.User;
import com.medicine.repository.MedicineRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicineService {

    private final MedicineRecordRepository medicineRecordRepository;

    public MedicineRecord getTodayRecord(MedicineRecord.MedicineType medicineType) {
        LocalDate today = LocalDate.now();
        return medicineRecordRepository.findByDateAndMedicineType(today, medicineType)
                .orElse(createDefaultRecord(today, medicineType));
    }

    public MedicineRecord markAsTaken(User user, MedicineRecord.MedicineType medicineType) {
        LocalDate today = LocalDate.now();
        MedicineRecord record = medicineRecordRepository.findByDateAndMedicineType(today, medicineType)
                .orElse(createDefaultRecord(today, medicineType));

        record.setTaken(true);
        record.setTakenTime(LocalDateTime.now());
        record.setTakenBy(user);

        MedicineRecord saved = medicineRecordRepository.save(record);
        log.debug("Medicine marked as taken - Type: {}, Date: {}, User: {}, Time: {}",
                medicineType, today, user.getUsername(), saved.getTakenTime());
        return saved;
    }

    public MedicineRecord cancelTaken(User user, MedicineRecord.MedicineType medicineType) {
        LocalDate today = LocalDate.now();
        MedicineRecord record = medicineRecordRepository.findByDateAndMedicineType(today, medicineType)
                .orElse(createDefaultRecord(today, medicineType));

        record.setTaken(false);
        record.setTakenTime(null);
        record.setTakenBy(null);

        MedicineRecord saved = medicineRecordRepository.save(record);
        log.debug("Medicine marked as cancelled - Type: {}, Date: {}, User: {}",
                medicineType, today, user.getUsername());
        return saved;
    }

    public List<MedicineRecord> getMonthRecords(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // JPA를 사용하여 날짜 범위로 조회
        List<MedicineRecord> records = medicineRecordRepository.findByDateBetweenOrderByDateDescMedicineTypeAsc(startDate, endDate);

        log.debug("Retrieved {} records for {}-{}", records.size(), year, month);
        return records;
    }

    public Map<String, Object> getMonthCalendarData(int year, int month) {
        List<MedicineRecord> records = getMonthRecords(year, month);

        // Group records by date
        Map<LocalDate, List<MedicineRecord>> recordsByDate = records.stream()
                .filter(MedicineRecord::isTaken)
                .collect(Collectors.groupingBy(MedicineRecord::getDate));

        Map<String, Object> calendarData = new HashMap<>();
        List<Map<String, Object>> events = new ArrayList<>();

        for (Map.Entry<LocalDate, List<MedicineRecord>> entry : recordsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<MedicineRecord> dayRecords = entry.getValue();

            // Check if both morning and evening medicine are taken
            boolean hasMorning = dayRecords.stream()
                    .anyMatch(r -> r.getMedicineType() == MedicineRecord.MedicineType.MORNING && r.isTaken());
            boolean hasEvening = dayRecords.stream()
                    .anyMatch(r -> r.getMedicineType() == MedicineRecord.MedicineType.EVENING && r.isTaken());

            // If at least one medicine is taken but not both, mark as incomplete
            boolean isIncomplete = !(hasMorning && hasEvening);

            // Get the first taken time for display
            String time = dayRecords.get(0).getTakenTime().toLocalTime().toString();

            Map<String, Object> event = new HashMap<>();
            event.put("date", date.toString());
            event.put("time", time);
            event.put("title", "약 복용");
            event.put("incomplete", isIncomplete);
            event.put("hasMorning", hasMorning);
            event.put("hasEvening", hasEvening);

            log.debug("Calendar event - Date: {}, Morning: {}, Evening: {}, Incomplete: {}",
                    date, hasMorning, hasEvening, isIncomplete);

            events.add(event);
        }

        calendarData.put("events", events);
        return calendarData;
    }

    private MedicineRecord createDefaultRecord(LocalDate date, MedicineRecord.MedicineType medicineType) {
        MedicineRecord record = new MedicineRecord();
        record.setDate(date);
        record.setMedicineType(medicineType);
        record.setTaken(false);
        return record;
    }
}
