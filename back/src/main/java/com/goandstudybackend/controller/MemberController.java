package com.goandstudybackend.controller;

import com.goandstudybackend.dto.request.AddWishlistRequest;
import com.goandstudybackend.dto.request.AiFeedbackRequest;
import com.goandstudybackend.dto.request.AiRecommendationRequest;
import com.goandstudybackend.dto.request.BookRequestRequest;
import com.goandstudybackend.dto.request.ChangePasswordRequest;
import com.goandstudybackend.dto.request.ChatbotFeedbackRequest;
import com.goandstudybackend.dto.request.ChatbotMessageRequest;
import com.goandstudybackend.dto.request.RenewalRequestRequest;
import com.goandstudybackend.dto.request.ReadingProgressRequest;
import com.goandstudybackend.dto.request.ReserveBookRequest;
import com.goandstudybackend.dto.request.SetReadingGoalRequest;
import com.goandstudybackend.dto.request.SubmitReviewRequest;
import com.goandstudybackend.dto.request.UpdateProfileRequest;
import com.goandstudybackend.dto.response.AiRecommendationResponse;
import com.goandstudybackend.dto.response.ApiResponse;
import com.goandstudybackend.dto.response.ChatbotMessageResponse;
import com.goandstudybackend.dto.response.MemberRecommendationResponse;
import com.goandstudybackend.service.AiRecommendationService;
import com.goandstudybackend.service.BookMilestoneService;
import com.goandstudybackend.service.BookQueueService;
import com.goandstudybackend.service.BookRequestService;
import com.goandstudybackend.service.LoanService;
import com.goandstudybackend.service.MemberService;
import com.goandstudybackend.service.MlRecommendationService;
import com.goandstudybackend.service.NotificationService;
import com.goandstudybackend.service.ReadingGoalService;
import com.goandstudybackend.service.RenewalRequestService;
import com.goandstudybackend.service.ReservationService;
import com.goandstudybackend.service.ReviewService;
import com.goandstudybackend.service.RagChatbotService;
import com.goandstudybackend.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/member")
@RequiredArgsConstructor
public class MemberController {

    private final ReadingGoalService readingGoalService;
    private final BookQueueService bookQueueService;
    private final BookMilestoneService bookMilestoneService;
    private final BookRequestService bookRequestService;
    private final RenewalRequestService renewalRequestService;
    private final LoanService loanService;
    private final AiRecommendationService aiRecommendationService;
    private final WishlistService wishlistService;
    private final ReservationService reservationService;
    private final ReviewService reviewService;
    private final MemberService memberService;
    private final NotificationService notificationService;
    private final MlRecommendationService mlRecommendationService;
    private final RagChatbotService ragChatbotService;

    @Operation(summary = "Get reading goal")
    @GetMapping("/reading-goal")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyReadingGoal(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Reading goal fetched", readingGoalService.getMyReadingGoal(authentication.getName())));
    }

    @Operation(summary = "Set reading goal")
    @PostMapping("/reading-goal")
    public ResponseEntity<ApiResponse<Map<String, Object>>> setReadingGoal(
            Authentication authentication,
            @Valid @RequestBody SetReadingGoalRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Reading goal saved", readingGoalService.setReadingGoal(authentication.getName(), request)));
    }

    @Operation(summary = "Delete reading goal")
    @DeleteMapping("/reading-goal")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteReadingGoal(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Reading goal deleted", readingGoalService.deleteReadingGoal(authentication.getName())));
    }

    @Operation(summary = "Request a book")
    @PostMapping("/book-requests")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestBook(
            Authentication authentication,
            @Valid @RequestBody BookRequestRequest request
    ) {
        String memberId = authentication.getName();
        String memberName = memberService.getMemberName(memberId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Book request created", bookRequestService.requestBook(memberId, memberName, request)));
    }

    @Operation(summary = "Get my book requests")
    @GetMapping("/book-requests")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyBookRequests(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Book requests fetched", bookRequestService.getMyBookRequests(authentication.getName())));
    }

    @Operation(summary = "Cancel book request")
    @DeleteMapping("/book-requests/{requestId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelBookRequest(Authentication authentication, @PathVariable String requestId) {
        return ResponseEntity.ok(ApiResponse.success("Book request cancelled", bookRequestService.cancelBookRequest(authentication.getName(), requestId)));
    }

    @Operation(summary = "Request renewal")
    @PostMapping("/renewal-requests")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestRenewal(
            Authentication authentication,
            @Valid @RequestBody RenewalRequestRequest request
    ) {
        String memberId = authentication.getName();
        String memberName = memberService.getMemberName(memberId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Renewal request created", renewalRequestService.requestRenewal(memberId, memberName, request)));
    }

    @Operation(summary = "Get my renewal requests")
    @GetMapping("/renewal-requests")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyRenewalRequests(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Renewal requests fetched", renewalRequestService.getMyRenewalRequests(authentication.getName())));
    }

    @Operation(summary = "Cancel renewal request")
    @DeleteMapping("/renewal-requests/{requestId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelRenewalRequest(Authentication authentication, @PathVariable String requestId) {
        return ResponseEntity.ok(ApiResponse.success("Renewal request cancelled", renewalRequestService.cancelRenewalRequest(authentication.getName(), requestId)));
    }

    @Operation(summary = "Mark loan as currently reading")
    @PatchMapping("/loans/{loanId}/reading")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAsCurrentlyReading(
            Authentication authentication,
            @PathVariable String loanId,
            @Valid @RequestBody ReadingProgressRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Reading progress updated", loanService.markAsCurrentlyReading(authentication.getName(), loanId, request)));
    }

    @Operation(summary = "Get currently reading books")
    @GetMapping("/currently-reading")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentlyReading(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Currently reading fetched", loanService.getCurrentlyReading(authentication.getName())));
    }

    @Operation(summary = "Get AI recommendations")
    @PostMapping("/ai/recommend")
    public ResponseEntity<ApiResponse<AiRecommendationResponse>> getAiRecommendations(
            Authentication authentication,
            @Valid @RequestBody AiRecommendationRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("AI recommendations fetched", aiRecommendationService.getAiRecommendations(authentication.getName(), request)));
    }

    @Operation(summary = "Get precomputed AI recommendations")
    @GetMapping("/ai/recommendations")
    public ResponseEntity<ApiResponse<AiRecommendationResponse>> getPrecomputedRecommendations(
            Authentication authentication,
            @RequestParam(defaultValue = "15") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success("AI recommendations fetched", aiRecommendationService.getPrecomputedRecommendations(authentication.getName(), Math.min(limit, 20))));
    }

    @Operation(summary = "Submit AI feedback")
    @PostMapping("/ai/feedback")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitAiFeedback(
            Authentication authentication,
            @Valid @RequestBody AiFeedbackRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("AI feedback recorded", aiRecommendationService.submitAiFeedback(authentication.getName(), request)));
    }

    @Operation(summary = "Get my active loans")
    @GetMapping("/loans")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyActiveLoans(
            Authentication authentication,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(ApiResponse.success("Loans fetched", loanService.getMyActiveLoans(authentication.getName(), status)));
    }

    @Operation(summary = "Get reading history")
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyReadingHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String genre
    ) {
        return ResponseEntity.ok(ApiResponse.success("History fetched", loanService.getMyReadingHistory(authentication.getName(), page, genre)));
    }

    @Operation(summary = "Get wishlist")
    @GetMapping("/wishlist")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyWishlist(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Wishlist fetched", wishlistService.getMyWishlist(authentication.getName())));
    }

    @Operation(summary = "Add to wishlist")
    @PostMapping("/wishlist")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addToWishlist(
            Authentication authentication,
            @Valid @RequestBody AddWishlistRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Wishlist updated", wishlistService.addToWishlist(authentication.getName(), request)));
    }

    @Operation(summary = "Remove from wishlist")
    @DeleteMapping("/wishlist/{bookId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> removeFromWishlist(Authentication authentication, @PathVariable String bookId) {
        return ResponseEntity.ok(ApiResponse.success("Wishlist updated", wishlistService.removeFromWishlist(authentication.getName(), bookId)));
    }

    @Operation(summary = "Reserve book")
    @PostMapping("/reservations")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reserveBook(
            Authentication authentication,
            @Valid @RequestBody ReserveBookRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Reservation created", reservationService.reserveBook(authentication.getName(), request)));
    }

    @Operation(summary = "Get reservations")
    @GetMapping("/reservations")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyReservations(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Reservations fetched", reservationService.getMyReservations(authentication.getName())));
    }

    @Operation(summary = "Cancel reservation")
    @DeleteMapping("/reservations/{reservationId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelReservation(Authentication authentication, @PathVariable String reservationId) {
        return ResponseEntity.ok(ApiResponse.success("Reservation cancelled", reservationService.cancelReservation(authentication.getName(), reservationId)));
    }

    @Operation(summary = "Submit review")
    @PostMapping("/reviews")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitReview(
            Authentication authentication,
            @Valid @RequestBody SubmitReviewRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Review submitted", reviewService.submitReview(authentication.getName(), request)));
    }

    @Operation(summary = "Get my reviews")
    @GetMapping("/reviews")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyReviews(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Reviews fetched", reviewService.getMyReviews(authentication.getName())));
    }

    @Operation(summary = "Get my profile")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Profile fetched", memberService.getMyProfile(authentication.getName())));
    }

    @Operation(summary = "Update my profile")
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated", memberService.updateProfile(authentication.getName(), request)));
    }

    @Operation(summary = "Change member password")
    @PutMapping("/profile/password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Password updated", memberService.changePassword(authentication.getName(), request)));
    }

    @Operation(summary = "Get my notifications")
    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyNotifications(
            Authentication authentication,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched", notificationService.getNotifications(authentication.getName(), status)));
    }

    @Operation(summary = "Get unread notification count")
    @GetMapping("/notifications/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUnreadCount(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Unread count fetched", memberService.getUnreadCount(authentication.getName())));
    }

    @Operation(summary = "Mark notification as read")
    @PatchMapping("/notifications/{notifId}/read")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAsRead(Authentication authentication, @PathVariable String notifId) {
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", notificationService.markAsRead(authentication.getName(), notifId)));
    }

    @Operation(summary = "Get ML recommendations (top picks + similar)")
    @GetMapping("/recommendations/sections")
    public ResponseEntity<ApiResponse<MemberRecommendationResponse>> getRecommendationSections(
            Authentication authentication,
            @RequestParam(defaultValue = "5") int topN,
            @RequestParam(defaultValue = "10") int similarN
    ) {
        MemberRecommendationResponse rec =
                mlRecommendationService.getRecommendations(authentication.getName(), topN, similarN);

        return ResponseEntity.ok(ApiResponse.success(
            "Recommendations fetched",
            rec
        ));
    }

    @Operation(summary = "Submit ML recommendation feedback")
    @PostMapping("/recommendations/feedback")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitMlFeedback(
            Authentication authentication,
            @RequestParam String bookId,
            @RequestParam String action
    ) {
        mlRecommendationService.submitFeedback(authentication.getName(), bookId, action);
        return ResponseEntity.ok(ApiResponse.success("Feedback submitted", Map.of("bookId", bookId, "action", action)));
    }

    @Operation(summary = "Send chatbot message")
    @PostMapping("/chatbot/message")
    public ResponseEntity<ApiResponse<ChatbotMessageResponse>> sendChatbotMessage(
            Authentication authentication,
            @Valid @RequestBody ChatbotMessageRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Chatbot response fetched", ragChatbotService.processMessage(authentication.getName(), request)));
    }

    @Operation(summary = "Get chatbot sessions")
    @GetMapping("/chatbot/sessions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getChatbotSessions(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Chatbot sessions fetched", ragChatbotService.getMySessions(authentication.getName())));
    }

    @Operation(summary = "Get chatbot session history")
    @GetMapping("/chatbot/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getChatbotSession(
            Authentication authentication,
            @PathVariable String sessionId
    ) {
        return ResponseEntity.ok(ApiResponse.success("Chatbot session fetched", ragChatbotService.getSessionHistory(authentication.getName(), sessionId)));
    }

    @Operation(summary = "Submit chatbot feedback")
    @PostMapping("/chatbot/feedback")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitChatbotFeedback(
            Authentication authentication,
            @Valid @RequestBody ChatbotFeedbackRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Chatbot feedback saved", ragChatbotService.submitFeedback(authentication.getName(), request)));
    }

    // Book Queue Endpoints
    @Operation(summary = "Get member's book queues")
    @GetMapping("/queues")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyQueues(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Queues fetched", bookQueueService.getMemberQueue(authentication.getName())));
    }

    @Operation(summary = "Create book queue")
    @PostMapping("/queues")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createQueue(
            Authentication authentication,
            @RequestParam String queueName,
            @RequestBody List<String> bookIds
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Queue created", bookQueueService.createQueue(authentication.getName(), queueName, bookIds)));
    }

    @Operation(summary = "Update book queue")
    @PutMapping("/queues/{queueId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateQueue(
            Authentication authentication,
            @PathVariable String queueId,
            @RequestBody List<String> bookIds
    ) {
        return ResponseEntity.ok(ApiResponse.success("Queue updated", bookQueueService.updateQueue(authentication.getName(), queueId, bookIds)));
    }

    @Operation(summary = "Delete book queue")
    @DeleteMapping("/queues/{queueId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteQueue(
            Authentication authentication,
            @PathVariable String queueId
    ) {
        bookQueueService.deleteQueue(authentication.getName(), queueId);
        return ResponseEntity.ok(ApiResponse.success("Queue deleted", Map.of()));
    }

    @Operation(summary = "Mark book as completed in queue")
    @PostMapping("/queues/{queueId}/books/{bookId}/complete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markQueueBookCompleted(
            Authentication authentication,
            @PathVariable String queueId,
            @PathVariable String bookId
    ) {
        return ResponseEntity.ok(ApiResponse.success("Book marked as completed", bookQueueService.markBookCompleted(authentication.getName(), queueId, bookId)));
    }

    @Operation(summary = "Start first book in queue")
    @PostMapping("/queues/{queueId}/start")
    public ResponseEntity<ApiResponse<Map<String, Object>>> startQueue(
            Authentication authentication,
            @PathVariable String queueId
    ) {
        bookQueueService.startFirstBook(authentication.getName(), queueId);
        return ResponseEntity.ok(ApiResponse.success("Queue started", Map.of()));
    }

    // Book Milestone Endpoints
    @Operation(summary = "Get member's book milestones")
    @GetMapping("/milestones")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyMilestones(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Milestones fetched", bookMilestoneService.getMemberMilestones(authentication.getName())));
    }

    @Operation(summary = "Create book milestone")
    @PostMapping("/milestones")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createMilestone(
            Authentication authentication,
            @RequestParam String bookId,
            @RequestParam LocalDate targetDate,
            @RequestParam(required = false) String notes
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Milestone created", bookMilestoneService.createMilestone(authentication.getName(), bookId, targetDate, notes)));
    }

    @Operation(summary = "Update book milestone")
    @PutMapping("/milestones/{milestoneId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateMilestone(
            Authentication authentication,
            @PathVariable String milestoneId,
            @RequestParam(required = false) LocalDate targetDate,
            @RequestParam(required = false) String notes
    ) {
        return ResponseEntity.ok(ApiResponse.success("Milestone updated", bookMilestoneService.updateMilestone(authentication.getName(), milestoneId, targetDate, notes)));
    }

    @Operation(summary = "Delete book milestone")
    @DeleteMapping("/milestones/{milestoneId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteMilestone(
            Authentication authentication,
            @PathVariable String milestoneId
    ) {
        bookMilestoneService.deleteMilestone(authentication.getName(), milestoneId);
        return ResponseEntity.ok(ApiResponse.success("Milestone deleted", Map.of()));
    }

    @Operation(summary = "Mark milestone as completed")
    @PostMapping("/milestones/{milestoneId}/complete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> completeMilestone(
            Authentication authentication,
            @PathVariable String milestoneId
    ) {
        return ResponseEntity.ok(ApiResponse.success("Milestone completed", bookMilestoneService.markMilestoneCompleted(authentication.getName(), milestoneId)));
    }

    @Operation(summary = "Update milestone progress")
    @PatchMapping("/milestones/{milestoneId}/progress")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateMilestoneProgress(
            Authentication authentication,
            @PathVariable String milestoneId,
            @RequestParam int progressPercent
    ) {
        return ResponseEntity.ok(ApiResponse.success("Progress updated", bookMilestoneService.updateProgress(authentication.getName(), milestoneId, progressPercent)));
    }

    @Operation(summary = "Get my detailed analytics")
    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyAnalytics(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Analytics fetched", memberService.getMyAnalytics(authentication.getName())));
    }

    @Operation(summary = "Update neural controls (recommendation preferences)")
    @PutMapping("/neural-controls")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateNeuralControls(
            Authentication authentication,
            @RequestParam(required = false) String inferenceLevel,
            @RequestParam(required = false) String privacyProjection,
            @RequestParam(required = false) Boolean obfuscationMode
    ) {
        return ResponseEntity.ok(ApiResponse.success("Neural controls updated",
                memberService.updateNeuralControls(authentication.getName(), inferenceLevel, privacyProjection, obfuscationMode)));
    }

}
