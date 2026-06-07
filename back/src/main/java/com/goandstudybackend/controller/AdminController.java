package com.goandstudybackend.controller;

import com.goandstudybackend.dto.request.ChangePasswordRequest;
import com.goandstudybackend.dto.request.CreateStaffRequest;
import com.goandstudybackend.dto.request.GenerateReportRequest;
import com.goandstudybackend.dto.request.MembershipUpgradeRequest;
import com.goandstudybackend.dto.request.ResetPasswordRequest;
import com.goandstudybackend.dto.request.ReviewRequest;
import com.goandstudybackend.dto.request.UpdateSystemConfigRequest;
import com.goandstudybackend.dto.request.WaiveFineRequest;
import com.goandstudybackend.dto.response.ApiResponse;
import com.goandstudybackend.entity.Author;
import com.goandstudybackend.entity.BookCategory;
import com.goandstudybackend.entity.Loan;
import com.goandstudybackend.repository.AuthorRepository;
import com.goandstudybackend.repository.BookCategoryRepository;
import com.goandstudybackend.repository.BookRepository;
import com.goandstudybackend.service.AdminService;
import com.goandstudybackend.service.AuthorService;
import com.goandstudybackend.service.BookRequestService;
import com.goandstudybackend.service.BookService;
import com.goandstudybackend.service.FineService;
import com.goandstudybackend.service.LoanService;
import com.goandstudybackend.service.NotificationService;
import com.goandstudybackend.service.ReportService;
import com.goandstudybackend.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.PageRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final BookService bookService;
    private final LoanService loanService;
    private final FineService fineService;
    private final ReportService reportService;
    private final SystemConfigService systemConfigService;
    private final BookRequestService bookRequestService;
    private final AuthorService authorService;
    private final AuthorRepository authorRepository;
    private final BookCategoryRepository bookCategoryRepository;
    private final BookRepository bookRepository;
    private final NotificationService notificationService;

    @GetMapping("/authors")
    @Operation(summary = "List all authors with stats")
    public ResponseEntity<Map<String, Object>> getAuthors() {
        List<Author> authors = authorRepository.findAll();
        long total = authors.size();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("authors", authors);
        response.put("total", total);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/authors/{authorId}")
    @Operation(summary = "Get author detail and books")
    public ResponseEntity<Map<String, Object>> getAuthorDetail(@PathVariable String authorId) {
        Map<String, Object> detail = authorService.getAuthorDetail(authorId);
        return ResponseEntity.ok(detail);
    }

    @PostMapping("/authors/{authorId}/refresh-stats")
    @Operation(summary = "Refresh author stats")
    public ResponseEntity<Map<String, Object>> refreshAuthorStats(@PathVariable String authorId) {
        authorService.updateAuthorStats(authorId);
        return ResponseEntity.ok(Map.of("status", "stats refreshed"));
    }

    @PostMapping("/authors")
    @Operation(summary = "Create new author")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createAuthor(
            @RequestBody Map<String, Object> request
    ) {
        try {
            String name = (String) request.get("name");
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.failure("Author name is required", null));
            }
            String bio = (String) request.get("bio");
            String nationality = (String) request.get("nationality");
            
            // Safely handle genres - could be List or String
            List<String> genres = new ArrayList<>();
            Object genresObj = request.get("genres");
            if (genresObj instanceof List) {
                genres = ((List<?>) genresObj).stream()
                        .map(Object::toString)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
            } else if (genresObj instanceof String) {
                String genresStr = (String) genresObj;
                if (!genresStr.isEmpty()) {
                    genres = Arrays.stream(genresStr.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());
                }
            }
            
            Author author = authorService.createAuthor(name.trim(), bio, nationality, genres);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Author created", Map.of(
                            "id", author.getId(),
                            "name", author.getName(),
                            "nationality", author.getNationality(),
                            "genres", author.getGenres()
                    )));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("Failed to create author: " + e.getMessage(), null));
        }
    }

    @PutMapping("/authors/{authorId}")
    @Operation(summary = "Update author")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateAuthor(
            @PathVariable String authorId,
            @RequestBody Map<String, Object> request
    ) {
        String name = (String) request.get("name");
        String bio = (String) request.get("bio");
        String nationality = (String) request.get("nationality");
        @SuppressWarnings("unchecked")
        List<String> genres = (List<String>) request.get("genres");
        
        Author author = authorService.updateAuthor(authorId, name, bio, nationality, genres);
        return ResponseEntity.ok(ApiResponse.success("Author updated", Map.of(
                "id", author.getId(),
                "name", author.getName(),
                "nationality", author.getNationality(),
                "genres", author.getGenres()
        )));
    }

    @DeleteMapping("/authors/{authorId}")
    @Operation(summary = "Delete author")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteAuthor(
            @PathVariable String authorId
    ) {
        authorService.deleteAuthor(authorId);
        return ResponseEntity.ok(ApiResponse.success("Author deleted", Map.of("authorId", authorId)));
    }

    @GetMapping("/staff")
    @Operation(summary = "List staff")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStaff(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success("Staff fetched", adminService.getAllStaff(isActive, page, limit)));
    }

    @PostMapping("/staff")
    @Operation(summary = "Create staff")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createStaff(
            Authentication authentication,
            @Valid @RequestBody CreateStaffRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Staff created", adminService.createStaff(request, authentication.getName())));
    }

    @PatchMapping("/staff/{staffId}/toggle")
    @Operation(summary = "Toggle staff active status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleStaff(Authentication authentication, @PathVariable String staffId) {
        boolean active = adminService.getStaffById(staffId).isActive();
        Map<String, Object> result = active
                ? adminService.deactivateStaff(staffId, authentication.getName())
                : adminService.reactivateStaff(staffId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Staff status updated", result));
    }

    @DeleteMapping("/staff/{staffId}")
    @Operation(summary = "Deactivate staff")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deactivateStaff(Authentication authentication, @PathVariable String staffId) {
        return ResponseEntity.ok(ApiResponse.success("Staff deactivated", adminService.deactivateStaff(staffId, authentication.getName())));
    }

    @GetMapping("/members")
    @Operation(summary = "List members")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMembers(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "1") int page
    ) {
        return ResponseEntity.ok(ApiResponse.success("Members fetched", adminService.getAllMembers(isActive, sortBy, page)));
    }

    @GetMapping("/members/{memberId}")
    @Operation(summary = "Get member details")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMember(Authentication authentication, @PathVariable String memberId) {
        return ResponseEntity.ok(ApiResponse.success("Member fetched", adminService.getMemberWithAnalytics(memberId, authentication.getName())));
    }

    @PatchMapping("/members/{memberId}/deactivate")
    @Operation(summary = "Deactivate member")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deactivateMember(Authentication authentication, @PathVariable String memberId) {
        return ResponseEntity.ok(ApiResponse.success("Member deactivated", adminService.deactivateMember(memberId, authentication.getName())));
    }

    @PatchMapping("/members/{memberId}/reactivate")
    @Operation(summary = "Reactivate member")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reactivateMember(Authentication authentication, @PathVariable String memberId) {
        return ResponseEntity.ok(ApiResponse.success("Member reactivated", adminService.reactivateMember(memberId, authentication.getName())));
    }

    @DeleteMapping("/members/{memberId}")
    @Operation(summary = "Delete member")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteMember(Authentication authentication, @PathVariable String memberId) {
        return ResponseEntity.ok(ApiResponse.success("Member deleted", adminService.deleteMember(memberId, authentication.getName())));
    }

    @GetMapping("/books")
    @Operation(summary = "List books for admin")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBooks(
            @RequestParam(required = false) Boolean isDeleted,
            @RequestParam(required = false) String genre,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "0") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Books fetched", bookService.getAllBooksIncludingDeleted(isDeleted, genre, page, size)));
    }

    @GetMapping("/books/low-stock")
    @Operation(summary = "List low-stock books")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLowStockBooks() {
        Map<String, Object> books = bookService.getAllBooksIncludingDeleted(false, null, 1);
        Object rawBooks = books.get("books");
        List<?> lowStock = rawBooks instanceof List<?> list
                ? list.stream().filter(item -> {
                    if (!(item instanceof com.goandstudybackend.entity.Book book)) {
                        return false;
                    }
                    return book.getAvailableCopies() <= 2;
                }).toList()
                : List.of();
        return ResponseEntity.ok(ApiResponse.success("Low-stock books fetched", Map.of("books", lowStock, "total", lowStock.size())));
    }

    @DeleteMapping("/books/{bookId}")
    @Operation(summary = "Soft-delete book")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteBook(
            Authentication authentication,
            @PathVariable String bookId,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        String reason = body == null ? null : String.valueOf(body.getOrDefault("reason", ""));
        return ResponseEntity.ok(ApiResponse.success("Book deleted", bookService.softDeleteBook(bookId, authentication.getName(), reason)));
    }

    @GetMapping("/loans")
    @Operation(summary = "List loans")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLoans() {
        return ResponseEntity.ok(ApiResponse.success("Loans fetched", loanService.getLoans()));
    }

    @PostMapping("/loans/detect-overdues")
    @Operation(summary = "Manually trigger overdue detection and fine record creation")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detectOverdues() {
        var overdueLoans = loanService.detectOverdues();
        return ResponseEntity.ok(ApiResponse.success("Overdue detection completed", Map.of(
            "overdueLoansFound", overdueLoans.size(),
            "loans", overdueLoans.stream().map(Loan::getId).toList()
        )));
    }

    @GetMapping("/fines")
    @Operation(summary = "List fines")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFines(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page
    ) {
        return ResponseEntity.ok(ApiResponse.success("Fines fetched", fineService.getAllFines(status, page)));
    }

    @PostMapping("/fines/{fineId}/waive")
    @Operation(summary = "Waive fine")
    public ResponseEntity<ApiResponse<Map<String, Object>>> waiveFine(
            Authentication authentication,
            @PathVariable String fineId,
            @Valid @RequestBody WaiveFineRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Fine waived", fineService.waiveFine(fineId, request.getReason(), authentication.getName())));
    }

    @PostMapping("/fines/{fineId}/pay")
    @Operation(summary = "Mark fine as paid/collect payment")
    public ResponseEntity<ApiResponse<Map<String, Object>>> payFine(
            Authentication authentication,
            @PathVariable String fineId
    ) {
        return ResponseEntity.ok(ApiResponse.success("Fine marked as paid", fineService.collectFine(fineId, authentication.getName())));
    }

    @GetMapping("/config")
    @Operation(summary = "Get system config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConfig() {
        return ResponseEntity.ok(ApiResponse.success("Config fetched", systemConfigService.getMergedConfig()));
    }

    @PutMapping("/config")
    @Operation(summary = "Update system config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateConfig(
            Authentication authentication,
            @Valid @RequestBody UpdateSystemConfigRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Config updated", systemConfigService.updateSystemConfig(request, authentication.getName())));
    }

    @GetMapping("/reports")
    @Operation(summary = "List reports")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReports(
            @RequestParam(required = false) String reportType,
            @RequestParam(defaultValue = "1") int page
    ) {
        return ResponseEntity.ok(ApiResponse.success("Reports fetched", reportService.getAllReports(reportType, page)));
    }

    @PostMapping("/reports")
    @Operation(summary = "Generate report")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateReport(
            Authentication authentication,
            @Valid @RequestBody GenerateReportRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Report generated", reportService.generateReport(request, authentication.getName())));
    }

    @GetMapping("/analytics/dashboard")
    @Operation(summary = "Get admin analytics dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalyticsDashboard() {
        return ResponseEntity.ok(ApiResponse.success("Analytics dashboard fetched", reportService.getAdminDashboard()));
    }

    @GetMapping("/analytics/genres")
    @Operation(summary = "Get genre analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGenreAnalytics() {
        return ResponseEntity.ok(ApiResponse.success("Genre analytics fetched", reportService.getGenreAnalytics()));
    }

    @GetMapping("/analytics/ai-usage")
    @Operation(summary = "Get AI usage analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAiUsageStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate
    ) {
        return ResponseEntity.ok(ApiResponse.success("AI usage fetched", reportService.getAiUsageStats(fromDate, toDate)));
    }

    @GetMapping("/reports/export")
    @Operation(summary = "Export report as CSV/TSV/PDF")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam String reportType,
            @RequestParam String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate
    ) {
        LocalDateTime from = fromDate != null ? fromDate : LocalDateTime.now().minusMonths(1);
        LocalDateTime to = toDate != null ? toDate : LocalDateTime.now();

        byte[] data = switch (format.toLowerCase()) {
            case "csv" -> reportService.exportReportAsCsv(reportType, from, to);
            case "tsv" -> reportService.exportReportAsTsv(reportType, from, to);
            case "pdf" -> reportService.exportReportAsPdf(reportType, from, to);
            default -> reportService.exportReportAsCsv(reportType, from, to);
        };

        String contentType = switch (format.toLowerCase()) {
            case "csv" -> "text/csv";
            case "tsv" -> "text/tab-separated-values";
            case "pdf" -> "application/pdf";
            default -> "text/csv";
        };

        String filename = reportType + "_report_" + LocalDateTime.now().toLocalDate() + "." + format.toLowerCase();

        return ResponseEntity.ok()
                .header("Content-Type", contentType)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(data);
    }

    // Category Management Endpoints

    @GetMapping("/categories")
    @Operation(summary = "Get all book categories with book counts")
    public ResponseEntity<ApiResponse<List<BookCategory>>> getAllCategories() {
        List<BookCategory> categories = bookService.getBookCategories();
        return ResponseEntity.ok(ApiResponse.success("Categories fetched", categories));
    }

    @PostMapping("/categories")
    @Operation(summary = "Create new category")
    public ResponseEntity<ApiResponse<BookCategory>> createCategory(@RequestBody Map<String, Object> request) {
        String id = (String) request.get("id");
        String name = (String) request.get("name");
        String description = (String) request.get("description");

        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Category name is required", null));
        }

        // Generate ID if not provided
        if (id == null || id.trim().isEmpty()) {
            id = "CAT" + System.currentTimeMillis();
        }

        // Check if category already exists
        if (bookCategoryRepository.existsById(id)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Category with this ID already exists", null));
        }

        BookCategory category = BookCategory.builder()
                .id(id)
                .name(name)
                .description(description)
                .slug(name.toLowerCase().replaceAll("[^a-z0-9]", "-"))
                .bookCount(0)
                .build();

        BookCategory saved = bookCategoryRepository.save(category);
        return ResponseEntity.ok(ApiResponse.success("Category created", saved));
    }

    @PutMapping("/categories/{categoryId}")
    @Operation(summary = "Update category")
    public ResponseEntity<ApiResponse<BookCategory>> updateCategory(
            @PathVariable String categoryId,
            @RequestBody Map<String, Object> request
    ) {
        BookCategory category = bookCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        String name = (String) request.get("name");
        String description = (String) request.get("description");

        if (name != null && !name.trim().isEmpty()) {
            category.setName(name);
            category.setSlug(name.toLowerCase().replaceAll("[^a-z0-9]", "-"));
        }
        if (description != null) {
            category.setDescription(description);
        }

        BookCategory updated = bookCategoryRepository.save(category);
        return ResponseEntity.ok(ApiResponse.success("Category updated", updated));
    }

    @DeleteMapping("/categories/{categoryId}")
    @Operation(summary = "Delete category")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable String categoryId) {
        BookCategory category = bookCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Check if category has books
        long bookCount = bookRepository.countByCategoryIdsContainingAndIsDeletedFalse(categoryId);
        if (bookCount > 0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Cannot delete category with " + bookCount + " books. Reassign books first.", null));
        }

        bookCategoryRepository.delete(category);
        return ResponseEntity.ok(ApiResponse.success("Category deleted", null));
    }

    @PostMapping("/notifications/broadcast")
    @Operation(summary = "Broadcast notification to all users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> broadcastNotification(
            Authentication authentication,
            @RequestBody Map<String, String> request
    ) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Message is required", null));
        }

        try {
            notificationService.broadcastNotification(message.trim(), authentication.getName());
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message", "Broadcast notification sent successfully");
            response.put("recipientCount", "All active users");
            return ResponseEntity.ok(ApiResponse.success("Broadcast sent", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("Failed to send broadcast: " + e.getMessage(), null));
        }
    }

}
