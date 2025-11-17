package com.medicine.service;

import com.medicine.model.MedicineRecord;
import com.medicine.model.User;
import com.medicine.repository.MedicineRecordRepository;
import com.medicine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicineSchedulerService {

    private final MedicineRecordRepository medicineRecordRepository;
    private final UserRepository userRepository;
    private final PushNotificationService pushNotificationService;

    // 아침 약 복용 알림: 오전 9시부터 12시까지 1시간마다 실행
    @Scheduled(cron = "0 0 9-11 * * *")  // 9시, 10시, 11시
    public void checkMorningMedicine() {
        log.info("Checking morning medicine at {}", LocalTime.now());
        checkAndNotify(MedicineRecord.MedicineType.MORNING, "아침 약복용을 잊으신건 아니죠?");
    }

    // 저녁 약 복용 알림: 오후 7시부터 10시까지 1시간마다 실행
    @Scheduled(cron = "0 0 19-21 * * *")  // 19시, 20시, 21시
    public void checkEveningMedicine() {
        log.info("Checking evening medicine at {}", LocalTime.now());
        checkAndNotify(MedicineRecord.MedicineType.EVENING, "저녁 약복용을 잊으신건 아니죠?");
    }

    private void checkAndNotify(MedicineRecord.MedicineType medicineType, String message) {
        LocalDate today = LocalDate.now();

        // Get all users
        List<User> allUsers = new ArrayList<>();
        userRepository.findAll().forEach(allUsers::add);

        for (User user : allUsers) {
            // Get all medicine records for today
            List<MedicineRecord> allRecords = new ArrayList<>();
            medicineRecordRepository.findAll().forEach(allRecords::add);

            Optional<MedicineRecord> recordOpt = allRecords.stream()
                    .filter(r -> r.getDate().equals(today) && r.getMedicineType() == medicineType)
                    .findFirst();

            // If no record exists or medicine is not taken, send notification
            if (recordOpt.isEmpty() || !recordOpt.get().isTaken()) {
                log.info("Sending {} medicine reminder to user: {}", medicineType, user.getUsername());
                pushNotificationService.sendNotification(
                        user.getUsername(),
                        "💊 약 복용 알림",
                        message,
                        "/medicine",
                        Map.of(
                                "type", "medicine-reminder",
                                "medicineType", medicineType.name().toLowerCase()
                        )
                );
            }
        }
    }
}
