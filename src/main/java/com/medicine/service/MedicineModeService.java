package com.medicine.service;

import com.medicine.model.MedicineMode;
import com.medicine.repository.MedicineModeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicineModeService {

    private static final String MODE_ID = "medicine_mode";
    private final MedicineModeRepository medicineModeRepository;

    /**
     * 현재 약 복용 모드 조회
     */
    public MedicineMode getCurrentMode() {
        return medicineModeRepository.findById(MODE_ID)
                .orElse(createDefaultMode());
    }

    /**
     * 약 복용 모드 변경
     */
    public MedicineMode updateMode(MedicineMode.Mode mode) {
        MedicineMode medicineMode = medicineModeRepository.findById(MODE_ID)
                .orElse(new MedicineMode());

        medicineMode.setId(MODE_ID);
        medicineMode.setMode(mode);

        MedicineMode saved = medicineModeRepository.save(medicineMode);
        log.info("Medicine mode updated to: {}", mode);
        return saved;
    }

    /**
     * 기본 모드 생성 (아침/저녁)
     */
    private MedicineMode createDefaultMode() {
        MedicineMode mode = new MedicineMode();
        mode.setId(MODE_ID);
        mode.setMode(MedicineMode.Mode.MORNING_EVENING);
        return medicineModeRepository.save(mode);
    }
}
