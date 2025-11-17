package com.medicine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("MedicineMode")
public class MedicineMode implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;  // 항상 "medicine_mode" 사용 (단일 설정)

    private Mode mode;  // ONCE_A_DAY, MORNING_EVENING, THREE_TIMES

    public enum Mode {
        ONCE_A_DAY("하루 한번"),
        MORNING_EVENING("아침/저녁"),
        THREE_TIMES("아침/점심/저녁");

        private final String displayName;

        Mode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
