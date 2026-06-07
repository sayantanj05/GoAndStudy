package com.goandstudybackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeEventService {

    @Value("${realtime.server.url}")
    private String realtimeServerUrl;

    @Value("${realtime.server.secret}")
    private String realtimeSecret;

    private final RestTemplate restTemplate;

    public void broadcast(String event, String room, Object data) {
        try {
            if (realtimeServerUrl == null || realtimeServerUrl.isBlank()) {
                log.warn("Realtime server URL not configured, skipping broadcast for event {}", event);
                return;
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-realtime-secret", realtimeSecret);

            Map<String, Object> body = new HashMap<>();
            body.put("event", event);
            body.put("room", room);
            body.put("data", data);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject(realtimeServerUrl + "/broadcast", request, String.class);
        } catch (Exception exception) {
            log.warn("Realtime broadcast failed for event {}: {}", event, exception.getMessage());
        }
    }

    public void broadcastBookIssued(String memberId, String bookTitle, int newActiveLoans) {
        Map<String, Object> data = Map.of(
                "bookTitle", bookTitle,
                "memberId", memberId,
                "newActiveLoans", newActiveLoans
        );
        broadcast("BOOK_ISSUED", "admin", data);
        broadcast("BOOK_ISSUED", "staff", data);
        broadcast("BOOK_ISSUED", "member-" + memberId, data);
    }

    public void broadcastBookReturned(String memberId, String bookTitle, double fineAmount) {
        Map<String, Object> data = Map.of(
                "bookTitle", bookTitle,
                "memberId", memberId,
                "fineAmount", fineAmount
        );
        broadcast("BOOK_RETURNED", "admin", data);
        broadcast("BOOK_RETURNED", "staff", data);
        broadcast("BOOK_RETURNED", "member-" + memberId, data);
    }

    public void broadcastOverdueDetected(String memberId, String bookTitle) {
        Map<String, Object> data = Map.of("bookTitle", bookTitle, "memberId", memberId);
        broadcast("OVERDUE_DETECTED", "admin", data);
        broadcast("OVERDUE_DETECTED", "staff", data);
    }

    public void broadcastNotificationSent(String memberId, String type, String message) {
        Map<String, Object> data = Map.of("type", type, "message", message);
        broadcast("NOTIFICATION", "member-" + memberId, data);
    }

    public void broadcastMemberRegistered(String memberId, String name) {
        Map<String, Object> data = Map.of("memberId", memberId, "name", name);
        broadcast("MEMBER_REGISTERED", "admin", data);
    }

    public void broadcastFineCollected(String memberId, double amount) {
        Map<String, Object> data = Map.of("memberId", memberId, "amount", amount);
        broadcast("FINE_COLLECTED", "admin", data);
        broadcast("FINE_COLLECTED", "staff", data);
        broadcast("FINE_COLLECTED", "member-" + memberId, data);
    }

    public void broadcastMembershipUpgraded(String memberId, String newType) {
        Map<String, Object> data = Map.of("memberId", memberId, "membershipType", newType);
        broadcast("MEMBERSHIP_UPGRADED", "admin", data);
        broadcast("MEMBERSHIP_UPGRADED", "member-" + memberId, data);
    }

    public void broadcastBookAdded(String bookId, String bookTitle) {
        Map<String, Object> data = Map.of("bookId", bookId, "bookTitle", bookTitle);
        broadcast("BOOK_ADDED", "admin", data);
        broadcast("BOOK_ADDED", "staff", data);
    }

    public void broadcastReservationAvailable(String memberId, String bookTitle) {
        Map<String, Object> data = Map.of("bookTitle", bookTitle);
        broadcast("RESERVATION_AVAILABLE", "member-" + memberId, data);
    }
}
