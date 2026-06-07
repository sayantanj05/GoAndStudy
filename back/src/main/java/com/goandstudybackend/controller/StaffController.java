package com.goandstudybackend.controller;

import com.goandstudybackend.dto.request.ChangePasswordRequest;
import com.goandstudybackend.dto.request.CollectFineRequest;
import com.goandstudybackend.dto.request.CreateBookRequest;
import com.goandstudybackend.dto.request.IssueLoanRequest;
import com.goandstudybackend.dto.request.RejectRequest;
import com.goandstudybackend.dto.request.ReturnLoanRequest;
import com.goandstudybackend.dto.request.SendNotificationRequest;
import com.goandstudybackend.dto.request.UpdateBookRequest;
import com.goandstudybackend.dto.response.ApiResponse;
import com.goandstudybackend.service.BookService;
import com.goandstudybackend.service.FineService;
import com.goandstudybackend.service.LoanService;
import com.goandstudybackend.service.RenewalRequestService;
import com.goandstudybackend.service.StaffService;
import com.goandstudybackend.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;
    private final LoanService loanService;
    private final BookService bookService;
    private final FineService fineService;
    private final RenewalRequestService renewalRequestService;
    private final NotificationService notificationService;

    @Operation(summary = "Change staff password")
    @PutMapping("/profile/password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> changeOwnPassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Password updated", staffService.changeOwnPassword(authentication.getName(), request)));
    }

    @Operation(summary = "Lookup member")
    @GetMapping("/members/lookup")
    public ResponseEntity<ApiResponse<Map<String, Object>>> lookupMember(Authentication authentication, @RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.success("Member fetched", staffService.lookupMember(q, authentication.getName())));
    }

    @Operation(summary = "Get members for staff")
    @GetMapping("/members")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMembers(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(ApiResponse.success("Members fetched", staffService.getMembers(q)));
    }

    @Operation(summary = "Issue book")
    @PostMapping("/loans/issue")
    public ResponseEntity<ApiResponse<Map<String, Object>>> issueBook(Authentication authentication, @Valid @RequestBody IssueLoanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Book issued", loanService.issueBook(request, authentication.getName())));
    }

    @Operation(summary = "Get active loans")
    @GetMapping("/loans/active")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActiveLoans(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(ApiResponse.success("Active loans fetched", loanService.getActiveLoans(status, q)));
    }

    @Operation(summary = "Get active loans by ISBN and member")
    @GetMapping("/loans/active/by-isbn")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getActiveLoansByIsbn(
            @RequestParam String isbn
    ) {
        List<Map<String, Object>> loans = loanService.getActiveLoansByIsbn(isbn);
        return ResponseEntity.ok(ApiResponse.success("Matching active loans found (" + loans.size() + ")", loans));
    }

    @Operation(summary = "Get active loans by ISBN and member")
    @GetMapping("/loans/active/by-isbn-member")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getActiveLoansByIsbnAndMember(
            @RequestParam String isbn,
            @RequestParam String memberEmail
    ) {
        List<Map<String, Object>> loans = loanService.getActiveLoansByIsbnAndMember(isbn, memberEmail);
        return ResponseEntity.ok(ApiResponse.success("Matching active loans found (" + loans.size() + ")", loans));
    }

    @Operation(summary = "Return book")
    @PostMapping("/loans/{loanId}/return")
    public ResponseEntity<ApiResponse<Map<String, Object>>> returnBook(
            Authentication authentication,
            @PathVariable String loanId,
            @Valid @RequestBody ReturnLoanRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Book returned", loanService.returnBook(loanId, request, authentication.getName())));
    }

    @Operation(summary = "Create return notification")
    @PostMapping("/notifications/return")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createReturnNotification(
            @RequestParam String memberId,
            @RequestParam String bookTitle,
            @RequestParam String bookId,
            @RequestParam String loanId
    ) {
        notificationService.createReturnNotification(memberId, bookTitle, bookId, loanId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Return notification created");
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Notification created", response));
    }

    @Operation(summary = "Renew loan")
    @PatchMapping("/loans/{loanId}/renew")
    public ResponseEntity<ApiResponse<Map<String, Object>>> renewLoan(Authentication authentication, @PathVariable String loanId) {
        return ResponseEntity.ok(ApiResponse.success("Loan renewed", loanService.renewLoan(loanId, authentication.getName())));
    }

    @Operation(summary = "Add book")
    @PostMapping("/books")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addBook(Authentication authentication, @Valid @RequestBody CreateBookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Book added", bookService.addBook(request, authentication.getName())));
    }

    @Operation(summary = "Edit book")
    @PutMapping("/books/{bookId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> editBook(
            Authentication authentication,
            @PathVariable String bookId,
            @Valid @RequestBody UpdateBookRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Book updated", bookService.editBook(bookId, request, authentication.getName())));
    }

    @Operation(summary = "Get books for staff")
    @GetMapping("/books")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBooks(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Boolean available,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Books fetched", bookService.getBooks(q, genre, available, page, size)));
    }

    @Operation(summary = "Get staff book by ID")
    @GetMapping("/books/{bookId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBookById(@PathVariable String bookId) {
        return ResponseEntity.ok(ApiResponse.success("Book fetched", bookService.getBookById(bookId)));
    }

    @Operation(summary = "Get staff book by ISBN")
    @GetMapping("/books/isbn/{isbn}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBookByIsbn(@PathVariable String isbn) {
        return ResponseEntity.ok(ApiResponse.success("Book fetched", bookService.getBookByIsbn(isbn)));
    }

    @Operation(summary = "Get member profile for staff")
    @GetMapping("/members/{memberId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMemberProfile(Authentication authentication, @PathVariable String memberId) {
        return ResponseEntity.ok(ApiResponse.success("Member profile fetched", staffService.getMemberProfile(memberId, authentication.getName())));
    }

    @Operation(summary = "Send overdue notification")
    @PostMapping("/notifications/send")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendNotification(
            Authentication authentication,
            @Valid @RequestBody SendNotificationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Notification sent", staffService.sendNotification(request, authentication.getName())));
    }

    @Operation(summary = "Send bulk overdue notifications")
    @PostMapping("/notifications/bulk-send")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendBulkOverdueNotifications(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Bulk notifications sent", staffService.sendBulkOverdueNotifications(authentication.getName())));
    }

    @Operation(summary = "Get notifications for staff")
    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNotifications(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched", staffService.getNotifications(authentication.getName())));
    }

    @Operation(summary = "Mark staff notification as read")
    @PatchMapping("/notifications/{notifId}/read")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markNotificationAsRead(
            Authentication authentication, 
            @PathVariable String notifId
    ) {
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", 
                notificationService.markAsRead(authentication.getName(), notifId)));
    }

    @Operation(summary = "Get pending fines")
    @GetMapping("/fines/pending")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPendingFines() {
        return ResponseEntity.ok(ApiResponse.success("Pending fines fetched", fineService.getPendingFines()));
    }

    @Operation(summary = "Collect fine")
    @PatchMapping("/fines/{fineId}/collect")
    public ResponseEntity<ApiResponse<Map<String, Object>>> collectFine(
            Authentication authentication,
            @PathVariable String fineId,
            @Valid @RequestBody CollectFineRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Fine collected", fineService.collectFine(fineId, request, authentication.getName())));
    }

    @Operation(summary = "Get staff dashboard")
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStaffDashboard(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Staff dashboard fetched", staffService.getStaffDashboard(authentication.getName())));
    }

    @Operation(summary = "Get pending renewal requests")
    @GetMapping("/renewal-requests")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPendingRenewalRequests() {
        return ResponseEntity.ok(ApiResponse.success("Renewal requests fetched", renewalRequestService.getPendingRenewalRequests()));
    }

    @Operation(summary = "Approve renewal request")
    @PatchMapping("/renewal-requests/{requestId}/approve")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approveRenewal(Authentication authentication, @PathVariable String requestId) {
        return ResponseEntity.ok(ApiResponse.success("Renewal approved", renewalRequestService.approveRenewal(requestId, authentication.getName())));
    }

    @Operation(summary = "Reject renewal request")
    @PatchMapping("/renewal-requests/{requestId}/reject")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rejectRenewal(
            Authentication authentication,
            @PathVariable String requestId,
            @Valid @RequestBody RejectRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Renewal rejected", renewalRequestService.rejectRenewal(requestId, request, authentication.getName())));
    }
}
