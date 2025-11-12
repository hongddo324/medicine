package com.medicine.controller;

import com.medicine.model.MedicineRecord;
import com.medicine.model.Role;
import com.medicine.model.User;
import com.medicine.service.MedicineService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;

    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        MedicineRecord todayRecord = medicineService.getTodayRecord();

        model.addAttribute("user", user);
        model.addAttribute("todayRecord", todayRecord);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("canTakeMedicine", user.getRole() == Role.FATHER);

        log.debug("Home page accessed by user: {}, today's record: {}",
            user.getUsername(), todayRecord.isTaken() ? "taken" : "not taken");

        return "medicine";
    }

    @PostMapping("/api/medicine/take")
    @ResponseBody
    public ResponseEntity<?> takeMedicine(HttpSession session) {
        User user = (User) session.getAttribute("user");

        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        if (user.getRole() != Role.FATHER) {
            log.warn("Unauthorized medicine take attempt by user: {} with role: {}",
                user.getUsername(), user.getRole());
            return ResponseEntity.status(403).body(Map.of("error", "약 복용 기록 권한이 없습니다."));
        }

        MedicineRecord record = medicineService.markAsTaken(user.getUsername());

        log.info("Medicine taken - User: {}, Date: {}, Time: {}",
            user.getUsername(), record.getDate(), record.getTakenTime());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("taken", true);
        response.put("takenTime", record.getTakenTime().toString());
        response.put("takenBy", record.getTakenBy());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/medicine/calendar/{year}/{month}")
    @ResponseBody
    public ResponseEntity<?> getCalendarData(@PathVariable int year,
                                            @PathVariable int month,
                                            HttpSession session) {
        User user = (User) session.getAttribute("user");

        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        Map<String, Object> calendarData = medicineService.getMonthCalendarData(year, month);

        log.debug("Calendar data requested by user: {} for {}-{}", user.getUsername(), year, month);

        return ResponseEntity.ok(calendarData);
    }
}
