package com.medicine.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("PushSubscription")
public class PushSubscription implements Serializable {

    private static final long serialVersionUID = 1L;

    @Indexed
    private String username;

    @Id
    private String endpoint;

    @JsonProperty("expirationTime")
    private Long expirationTime;

    @JsonProperty("keys")
    private Keys keys;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Keys implements Serializable {
        private static final long serialVersionUID = 1L;

        @JsonProperty("p256dh")
        private String p256dh;

        @JsonProperty("auth")
        private String auth;
    }
}
