package com.medicine.service;

import com.medicine.model.MedicineRecord;
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

    public MedicineRecord getTodayRecord() {
        LocalDate today = LocalDate.now();
        return medicineRecordRepository.findByDate(today).orElse(createDefaultRecord(today));
    }

    public MedicineRecord markAsTaken(String username) {
        LocalDate today = LocalDate.now();
        MedicineRecord record = medicineRecordRepository.findByDate(today)
                .orElse(createDefaultRecord(today));

        record.setTaken(true);
        record.setTakenTime(LocalDateTime.now());
        record.setTakenBy(username);

        MedicineRecord saved = medicineRecordRepository.save(record);
        log.debug("Medicine marked as taken for date: {} by user: {} at {}", today, username, saved.getTakenTime());
        return saved;
    }

    public List<MedicineRecord> getMonthRecords(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<MedicineRecord> records = medicineRecordRepository.findAllByDateBetween(startDate, endDate);
        log.debug("Retrieved {} records for {}-{}", records.size(), year, month);
        return records;
    }

    public Map<String, Object> getMonthCalendarData(int year, int month) {
        List<MedicineRecord> records = getMonthRecords(year, month);

        Map<String, Object> calendarData = new HashMap<>();
        List<Map<String, Object>> events = records.stream()
                .filter(MedicineRecord::isTaken)
                .map(record -> {
                    Map<String, Object> event = new HashMap<>();
                    event.put("date", record.getDate().toString());
                    event.put("time", record.getTakenTime().toLocalTime().toString());
                    event.put("title", "약 복용");
                    return event;
                })
                .collect(Collectors.toList());

        calendarData.put("events", events);
        return calendarData;
    }

    private MedicineRecord createDefaultRecord(LocalDate date) {
        MedicineRecord record = new MedicineRecord();
        record.setId(UUID.randomUUID().toString());
        record.setDate(date);
        record.setTaken(false);
        return record;
    }
}
