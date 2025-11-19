package com.medicine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    private String type;  // MESSAGE_TYPE: "ACTIVITY", "DAILY_UPDATE", "WISH_UPDATE", etc.
    private Object data;  // The actual data payload
    private String action; // ACTION_TYPE: "CREATE", "UPDATE", "DELETE"
    private Long userId;  // Optional: target user ID for user-specific messages
}
