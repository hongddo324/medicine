package com.medicine.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "medicine_records", indexes = {
    @Index(name = "idx_date", columnList = "date"),
    @Index(name = "idx_medicine_type", columnList = "medicine_type"),
    @Index(name = "idx_taken_by", columnList = "taken_by")
})
public class MedicineRecord implements Serializable {

    private static final long serialVersionUID = 2L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "medicine_type", nullable = false, length = 20)
    private MedicineType medicineType;  // MORNING or EVENING

    @Column(name = "taken_time")
    private LocalDateTime takenTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taken_by")
    private User takenBy;

    private boolean taken;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public enum MedicineType {
        MORNING("아침"),
        LUNCH("점심"),
        EVENING("저녁");

        private final String displayName;

        MedicineType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
