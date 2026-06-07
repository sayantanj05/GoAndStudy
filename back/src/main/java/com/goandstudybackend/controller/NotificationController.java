package com.goandstudybackend.controller;

import com.goandstudybackend.dto.response.ApiResponse;
import com.goandstudybackend.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Create return notification")
    @PostMapping("/return")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createReturnNotification(
            @RequestParam String memberId,
            @RequestParam String bookTitle,
            @RequestParam String bookId,
            @RequestParam String loanId
    ) {
        notificationService.createReturnNotification(memberId, bookTitle, bookId, loanId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Return notification created successfully");
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Notification created", response));
    }
}
