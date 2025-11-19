package com.medicine.service;

import com.medicine.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast a message to all connected clients on a specific topic
     */
    public void broadcastMessage(String topic, WebSocketMessage message) {
        try {
            messagingTemplate.convertAndSend("/topic/" + topic, message);
            log.debug("Broadcasted message to /topic/{}: type={}, action={}",
                     topic, message.getType(), message.getAction());
        } catch (Exception e) {
            log.error("Failed to broadcast message to /topic/{}", topic, e);
        }
    }

    /**
     * Send activity notification to all clients
     */
    public void broadcastActivity(Object activity) {
        WebSocketMessage message = new WebSocketMessage(
            "ACTIVITY",
            activity,
            "CREATE",
            null
        );
        broadcastMessage("activities", message);
    }

    /**
     * Broadcast daily post update (create/update/delete)
     */
    public void broadcastDailyUpdate(Object daily, String action) {
        WebSocketMessage message = new WebSocketMessage(
            "DAILY",
            daily,
            action,
            null
        );
        broadcastMessage("dailies", message);
    }

    /**
     * Broadcast wish update (create/update/delete)
     */
    public void broadcastWishUpdate(Object wish, String action) {
        WebSocketMessage message = new WebSocketMessage(
            "WISH",
            wish,
            action,
            null
        );
        broadcastMessage("wishes", message);
    }

    /**
     * Send a message to a specific user
     */
    public void sendToUser(String username, String destination, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(username, destination, payload);
            log.debug("Sent message to user {}: destination={}", username, destination);
        } catch (Exception e) {
            log.error("Failed to send message to user {}", username, e);
        }
    }
}
