package com.medicine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("MedicineRecord")
public class MedicineRecord implements Serializable {

    @Id
    private String id;

    @Indexed
    private LocalDate date;

    private LocalDateTime takenTime;

    private String takenBy;

    private boolean taken;
}
