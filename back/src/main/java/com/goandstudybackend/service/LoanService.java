package com.goandstudybackend.service;

import com.goandstudybackend.dto.request.IssueLoanRequest;
import com.goandstudybackend.dto.request.ReadingProgressRequest;
import com.goandstudybackend.dto.request.ReturnLoanRequest;
import com.goandstudybackend.entity.Author;
import com.goandstudybackend.entity.Book;
import com.goandstudybackend.entity.BookCategory;
import com.goandstudybackend.entity.FineRecord;
import com.goandstudybackend.entity.Loan;
import com.goandstudybackend.entity.LoanEvent;
import com.goandstudybackend.entity.Member;
import com.goandstudybackend.enumeration.LoanEventType;
import com.goandstudybackend.enumeration.LoanStatus;
import com.goandstudybackend.exception.ResourceNotFoundException;
import com.goandstudybackend.repository.AuthorRepository;
import com.goandstudybackend.repository.BookCategoryRepository;
import com.goandstudybackend.repository.BookRepository;
import com.goandstudybackend.repository.FineRecordRepository;
import com.goandstudybackend.repository.LoanEventRepository;
import com.goandstudybackend.repository.LoanRepository;
import com.goandstudybackend.repository.MemberRepository;
import com.goandstudybackend.service.MemberAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class LoanService {
    private final MemberRepository memberRepository;
    private static final List<String> ACTIVE_STATUSES = List.of("ISSUED", "RENEWED", "OVERDUE", "ACTIVE");

    private boolean isActiveLoanStatus(String status) {
        return status != null && ACTIVE_STATUSES.contains(status.trim().toUpperCase());
    }

    private String normalizeIsbn(String value) {
        return value == null ? "" : value.replaceAll("[-\\s]", "").toLowerCase();
    }

    private String resolveBookIsbn(Loan loan) {
        String bookIsbn = loan.getBookIsbn();
        System.err.println("DEBUG resolveBookIsbn: Loan ID=" + loan.getId() + ", bookIsbn from loan=" + bookIsbn);
        if (bookIsbn != null && !bookIsbn.trim().isEmpty()) {
            System.err.println("DEBUG resolveBookIsbn: Returning bookIsbn from loan: " + bookIsbn);
            return bookIsbn;
        }
        try {
            Book book = bookRepository.findById(loan.getBookId()).orElse(null);
            String isbnFromBook = book == null ? null : book.getIsbn();
            System.err.println("DEBUG resolveBookIsbn: ISBN from book lookup=" + isbnFromBook);
            return isbnFromBook;
        } catch (Exception e) {
            System.err.println("Error resolving ISBN for loan " + loan.getId() + ": " + e.getMessage());
            return null;
        }
    }

    // Generate next loan ID in format LN000001, LN000002, etc.
    private String generateNextLoanId() {
        try {
            // Use MongoTemplate to only fetch IDs, avoiding boolean conversion issues
            Query query = new Query();
            query.fields().include("_id");
            List<Document> loanDocs = mongoTemplate.find(query, Document.class, "loans");
            int maxNum = 0;
            for (Document doc : loanDocs) {
                String id = doc.getString("_id");
                if (id != null && id.startsWith("LN")) {
                    try {
                        String numPart = id.substring(2); // Remove "LN" prefix
                        int num = Integer.parseInt(numPart);
                        if (num > maxNum) {
                            maxNum = num;
                        }
                    } catch (NumberFormatException e) {
                        // Skip non-numeric IDs
                    }
                }
            }
            return String.format("LN%06d", maxNum + 1);
        } catch (Exception e) {
            System.err.println("Error generating loan ID: " + e.getMessage());
            // Fallback: use timestamp-based ID
            return "LN" + System.currentTimeMillis();
        }
    }

    // Add staff methods
    public Map<String, Object> issueBook(IssueLoanRequest request, String staffId) {
        try {
            System.out.println("DEBUG: issueBook called with memberId=" + request.getMemberId() + ", bookId=" + request.getBookId() + ", staffId=" + staffId);
            Book book = bookRepository.findById(request.getBookId()).orElseThrow(() -> new ResourceNotFoundException("Book not found: " + request.getBookId()));
            System.out.println("DEBUG: Book found: " + book.getTitle() + ", availableCopies=" + book.getAvailableCopies());
            if (book.getAvailableCopies() <= 0) {
                throw new IllegalArgumentException("No available copies for book: " + book.getTitle());
            }

            LocalDateTime now = LocalDateTime.now();
            String loanId = generateNextLoanId();
            System.out.println("DEBUG: Generated loanId=" + loanId);
        Loan loan = Loan.builder()
            .id(loanId)
            .memberId(request.getMemberId())
            .bookId(request.getBookId())
            .bookIsbn(book.getIsbn())
            .bookTitle(book.getTitle())
            .issuedByStaffId(staffId)
            .issuedAt(now)
            .status(LoanStatus.ISSUED.name())
            .notes(request.getNotes())
            .createdAt(now)
            .createdBy(staffId)
            .build();

        // Get member to determine loan duration
        Member member = memberRepository.findById(request.getMemberId())
            .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + request.getMemberId()));

        // Set due date from member's loan duration (from their membership config)
        int loanDurationDays = member.getLoanDurationDays() > 0 ? member.getLoanDurationDays() : 14;
        LocalDateTime dueDate = LocalDateTime.now().plusDays(loanDurationDays);
        loan.setDueDate(dueDate);
        loan.setUpdatedAt(now);
        loan.setUpdatedBy(staffId);

        loan = loanRepository.save(loan);

        member.setActiveLoanCount(member.getActiveLoanCount() + 1);
        member.setTotalLoans(member.getTotalLoans() + 1);
        member.setUpdatedAt(now);
        memberRepository.save(member);
        activityLogService.log("BOOK_ISSUED", staffId, "ROLE_STAFF", "loans", loan.getId(),
                null, Map.of("memberId", member.getId(), "bookId", book.getId(), "bookTitle", book.getTitle()));
        realtimeEventService.broadcastBookIssued(member.getId(), book.getTitle(), member.getActiveLoanCount());

        loanEventRepository.save(LoanEvent.builder()
            .loanId(loan.getId())
            .eventType(LoanEventType.ISSUED)
            .timestamp(now)
            .triggeredBy(staffId)
            .metadata(Map.of("dueDate", dueDate))
            .createdAt(now)
            .build());

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        // Send notification to member about book issue
        if (notificationService != null) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
                String dueDateStr = dueDate.format(formatter);
                notificationService.createNotification(
                        request.getMemberId(),
                        "LOAN",
                        "Book Issued Successfully",
                        "You have successfully borrowed \"" + book.getTitle() + "\". Please return it by " + dueDateStr + " to avoid late fees.",
                        loan.getId(),
                        book.getId(),
                        "app",
                        "Sent",
                        null,
                        true
                );
            } catch (Exception e) {
                System.out.println("Book issue notification failed: " + e.getMessage());
            }
        }

            return mapToLoanResponse(loan);
        } catch (Exception e) {
            System.err.println("ERROR in issueBook: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public Map<String, Object> getActiveLoans(String status, String q) {
        try {
            List<Loan> loans = loanRepository.findAll().stream()
                .filter(l -> isActiveLoanStatus(l.getStatus()))
                .collect(Collectors.toList());
            if (status != null && !status.isEmpty()) {
                loans = loans.stream().filter(l -> l.getStatus() != null && l.getStatus().equalsIgnoreCase(status)).collect(Collectors.toList());
            }
            if (q != null && !q.isEmpty()) {
                String searchQuery = q.toLowerCase();
                String normalizedQuery = searchQuery.replaceAll("[-\\s]", "");
                loans = loans.stream()
                    .filter(l -> {
                        String storedIsbn = resolveBookIsbn(l);
                        return (l.getBookTitle() != null && l.getBookTitle().toLowerCase().contains(searchQuery)) ||
                               (storedIsbn != null && (storedIsbn.toLowerCase().contains(searchQuery) ||
                                                       normalizeIsbn(storedIsbn).contains(normalizedQuery))) ||
                               (l.getMemberId() != null && l.getMemberId().toLowerCase().contains(searchQuery));
                    })
                    .collect(Collectors.toList());
            }
            List<Map<String, Object>> items = loans.stream()
                .map(loan -> {
                    try {
                        return mapToLoanResponse(loan);
                    } catch (Exception e) {
                        System.err.println("Error mapping loan response for loan ID " + loan.getId() + ": " + e.getMessage());
                        // Return a minimal response mapping to avoid complete failure
                        Map<String, Object> minimalMap = new LinkedHashMap<>();
                        minimalMap.put("loanId", loan.getId());
                        minimalMap.put("id", loan.getId());
                        minimalMap.put("memberId", loan.getMemberId());
                        minimalMap.put("bookId", loan.getBookId());
                        minimalMap.put("bookIsbn", loan.getBookIsbn());
                        minimalMap.put("bookTitle", loan.getBookTitle());
                        minimalMap.put("status", loan.getStatus());
                        minimalMap.put("issuedAt", loan.getIssuedAt());
                        minimalMap.put("dueDate", loan.getDueDate());
                        minimalMap.put("returnedAt", loan.getReturnedAt());
                        minimalMap.put("error", "Failed to load complete loan details");
                        return minimalMap;
                    }
                })
                .collect(Collectors.toList());
            return Map.of("loans", items, "total", items.size());
        } catch (Exception e) {
            System.err.println("Error in getActiveLoans: " + e.getMessage());
            e.printStackTrace();
            return Map.of("loans", List.of(), "total", 0, "error", "Failed to load active loans");
        }
    }

    public List<Map<String, Object>> getActiveLoansByIsbn(String isbn) {
        try {
            System.err.println("DEBUG getActiveLoansByIsbn: Input ISBN=" + isbn);
            String normalizedIsbn = normalizeIsbn(isbn);
            System.err.println("DEBUG getActiveLoansByIsbn: Normalized ISBN=" + normalizedIsbn);

            // First try direct MongoDB query for bookIsbn field
            List<Loan> loansByBookIsbn = loanRepository.findByBookIsbn(normalizedIsbn);
            System.err.println("DEBUG: Found " + loansByBookIsbn.size() + " loans with bookIsbn=" + normalizedIsbn);
            
            // Filter for active status
            List<Loan> activeLoans = loansByBookIsbn.stream()
                .filter(l -> isActiveLoanStatus(l.getStatus()))
                .collect(Collectors.toList());
            System.err.println("DEBUG: Found " + activeLoans.size() + " active loans with bookIsbn=" + normalizedIsbn);

            // If found, return them
            if (!activeLoans.isEmpty()) {
                return activeLoans.stream()
                    .map(l -> {
                        try {
                            return mapToLoanResponse(l);
                        } catch (Exception e) {
                            System.err.println("ERROR mapping loan " + l.getId() + ": " + e.getMessage());
                            Map<String, Object> minimalMap = new LinkedHashMap<>();
                            minimalMap.put("loanId", l.getId());
                            minimalMap.put("id", l.getId());
                            minimalMap.put("memberId", l.getMemberId());
                            minimalMap.put("bookId", l.getBookId());
                            minimalMap.put("bookIsbn", l.getBookIsbn());
                            minimalMap.put("bookTitle", l.getBookTitle());
                            minimalMap.put("status", l.getStatus());
                            minimalMap.put("issuedAt", l.getIssuedAt());
                            minimalMap.put("dueDate", l.getDueDate());
                            minimalMap.put("returnedAt", l.getReturnedAt());
                            minimalMap.put("overdueDays", l.getOverdueDays());
                            minimalMap.put("fineAmount", l.getFineAmount());
                            minimalMap.put("finePaid", l.getFinePaid());
                            minimalMap.put("isCurrentlyReading", l.getIsCurrentlyReading());
                            minimalMap.put("author", "Unknown");
                            minimalMap.put("genre", "");
                            minimalMap.put("memberName", "Unknown");
                            minimalMap.put("bookCoverImageUrl", "");
                            minimalMap.put("error", "Failed to load complete loan details");
                            return minimalMap;
                        }
                    })
                    .collect(Collectors.toList());
            }

            // Fallback to original logic if direct query fails
            List<Loan> allLoans = loanRepository.findAll();
            System.err.println("DEBUG getActiveLoansByIsbn: Total loans=" + allLoans.size());

            List<Loan> loans = allLoans.stream()
                .filter(l -> isActiveLoanStatus(l.getStatus()))
                .filter(l -> {
                    String resolvedIsbn = resolveBookIsbn(l);
                    String normalizedResolvedIsbn = normalizeIsbn(resolvedIsbn);
                    boolean matches = normalizedIsbn.equals(normalizedResolvedIsbn);
                    System.err.println("DEBUG: Loan " + l.getId() + " - resolvedIsbn=" + resolvedIsbn + ", normalized=" + normalizedResolvedIsbn + ", matches=" + matches);
                    if (matches) {
                        System.err.println("DEBUG: Found matching loan " + l.getId() + " with ISBN=" + resolvedIsbn);
                    }
                    return matches;
                })
                .collect(Collectors.toList());

            System.err.println("DEBUG getActiveLoansByIsbn: Matching active loans=" + loans.size());

            return loans.stream()
                .map(l -> {
                    try {
                        return mapToLoanResponse(l);
                    } catch (Exception e) {
                        System.err.println("ERROR mapping loan " + l.getId() + ": " + e.getMessage());
                        Map<String, Object> minimalMap = new LinkedHashMap<>();
                        minimalMap.put("loanId", l.getId());
                        minimalMap.put("id", l.getId());
                        minimalMap.put("memberId", l.getMemberId());
                        minimalMap.put("bookId", l.getBookId());
                        minimalMap.put("bookIsbn", l.getBookIsbn());
                        minimalMap.put("bookTitle", l.getBookTitle());
                        minimalMap.put("status", l.getStatus());
                        minimalMap.put("issuedAt", l.getIssuedAt());
                        minimalMap.put("dueDate", l.getDueDate());
                        minimalMap.put("returnedAt", l.getReturnedAt());
                        minimalMap.put("overdueDays", l.getOverdueDays());
                        minimalMap.put("fineAmount", l.getFineAmount());
                        minimalMap.put("finePaid", l.getFinePaid());
                        minimalMap.put("isCurrentlyReading", l.getIsCurrentlyReading());
                        minimalMap.put("author", "Unknown");
                        minimalMap.put("genre", "");
                        minimalMap.put("memberName", "Unknown");
                        minimalMap.put("bookCoverImageUrl", "");
                        minimalMap.put("error", "Failed to load complete loan details");
                        return minimalMap;
                    }
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("ERROR in getActiveLoansByIsbn: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

public List<Map<String, Object>> getActiveLoansByIsbnAndMember(String isbn, String memberEmail) {
        try {
            String email = memberEmail == null ? "" : memberEmail.trim();
            Member member = memberRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + memberEmail));
            String normalizedIsbn = normalizeIsbn(isbn);
            if (normalizedIsbn.isBlank()) {
                return new ArrayList<>();
            }

            return loanRepository.findAll().stream()
                    .filter(l -> isActiveLoanStatus(l.getStatus()) || isActiveLoanStatus(l.getLoanStatus()))
                    .filter(l -> normalizedIsbn.equals(normalizeIsbn(resolveBookIsbn(l))))
                    .filter(l -> member.getId().equals(l.getMemberId()))
                    .map(this::safeLoanResponse)
                    .collect(Collectors.toList());
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            System.err.println("ERROR in getActiveLoansByIsbnAndMember: " + exception.getMessage());
            exception.printStackTrace();
            throw new IllegalStateException("Could not verify member loan. Please retry or contact admin.", exception);
        }
    }

    private Map<String, Object> safeLoanResponse(Loan loan) {
        try {
            return mapToLoanResponse(loan);
        } catch (Exception e) {
            System.err.println("ERROR mapping loan " + loan.getId() + ": " + e.getMessage());
            Map<String, Object> minimalMap = new LinkedHashMap<>();
            minimalMap.put("loanId", loan.getId());
            minimalMap.put("id", loan.getId());
            minimalMap.put("memberId", loan.getMemberId());
            minimalMap.put("bookId", loan.getBookId());
            minimalMap.put("bookIsbn", resolveBookIsbn(loan));
            minimalMap.put("bookTitle", loan.getBookTitle());
            minimalMap.put("status", loan.getStatus());
            minimalMap.put("issuedAt", loan.getIssuedAt());
            minimalMap.put("dueDate", loan.getDueDate());
            minimalMap.put("returnedAt", loan.getReturnedAt());
            minimalMap.put("overdueDays", loan.getOverdueDays());
            minimalMap.put("fineAmount", loan.getFineAmount());
            minimalMap.put("finePaid", loan.getFinePaid());
            minimalMap.put("isCurrentlyReading", loan.getIsCurrentlyReading());
            minimalMap.put("author", "Unknown");
            minimalMap.put("genre", "");
            minimalMap.put("memberName", "Unknown");
            minimalMap.put("bookCoverImageUrl", "");
            minimalMap.put("error", "Loaded with partial details");
            return minimalMap;
        }
    }

    public Map<String, Object> returnBook(String loanId, ReturnLoanRequest request, String staffId) {
        // Use MongoTemplate directly to ensure no repository-level ID conversion issues
        org.springframework.data.mongodb.core.query.Query query = new org.springframework.data.mongodb.core.query.Query(
                org.springframework.data.mongodb.core.query.Criteria.where("_id").is(loanId)
        );
        Loan loan = mongoTemplate.findOne(query, Loan.class);
        
        if (loan == null) {
            throw new ResourceNotFoundException("Loan not found with ID: " + loanId);
        }

        String rawStatus = loan.getStatus();
        String normalizedStatus = rawStatus == null ? "" : rawStatus.trim().toUpperCase();
        
        System.err.println("DEBUG: Normalized Status: '" + normalizedStatus + "'");

        if (!List.of("ISSUED", "RENEWED", "OVERDUE").contains(normalizedStatus)) {
            System.err.println("DEBUG: Rejecting return for status: " + normalizedStatus);
            throw new IllegalArgumentException("Cannot return non-active loan (Current status: " + normalizedStatus + ")");
        }

        LocalDateTime now = LocalDateTime.now();
        loan.setReturnedAt(now);
        loan.setReturnedByStaffId(staffId);
        loan.setStatus(LoanStatus.RETURNED.name());
        loan.setNotes(request == null ? null : request.getNotes());
        loan.setIsCurrentlyReading(false);
        loan.setUpdatedAt(now);
        loan.setUpdatedBy(staffId);
        
        applyReturnFine(loan, now);

        loanRepository.save(loan);
        System.out.println("DEBUG: Loan saved successfully.");

        loanEventRepository.save(LoanEvent.builder()
            .loanId(loan.getId())
            .eventType(LoanEventType.RETURNED)
            .timestamp(now)
            .triggeredBy(staffId)
            .metadata(Map.of("fineAmount", loan.getFineAmount()))
            .createdAt(now)
            .build());

        System.err.println("DEBUG: Fetching book with ID: " + loan.getBookId());
        Book book = bookRepository.findById(loan.getBookId()).orElseThrow(() -> {
            System.err.println("DEBUG: Book NOT found for loan bookId: " + loan.getBookId());
            return new ResourceNotFoundException("Book not found for loan: " + loan.getBookId());
        });
        
        System.err.println("DEBUG: Book found: " + book.getTitle() + ". Updating availability...");
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        book.setUpdatedAt(now);
        bookRepository.save(book);
        System.err.println("DEBUG: Book availability updated successfully.");

        memberRepository.findById(loan.getMemberId()).ifPresent(member -> {
            int activeCount = loanRepository.findByMemberIdAndStatusIn(
                    loan.getMemberId(),
                    List.of("ISSUED", "RENEWED", "OVERDUE", "Active", "Issued", "Renewed", "Overdue")
            ).size();
            member.setActiveLoanCount(activeCount);
            member.setUpdatedAt(now);
            memberRepository.save(member);
        });
        activityLogService.log("BOOK_RETURNED", staffId, "ROLE_STAFF", "loans", loan.getId(),
                null, Map.of("memberId", loan.getMemberId(), "bookId", book.getId(), "bookTitle", book.getTitle(), "fineAmount", loan.getFineAmount()));
        realtimeEventService.broadcastBookReturned(loan.getMemberId(), book.getTitle(), loan.getFineAmount());

        // Trigger queue notification if this book is in a member's queue
        if (bookQueueService != null) {
            try {
                bookQueueService.checkAndNotifyNextBook(loan.getMemberId(), loan.getBookId());
            } catch (Exception e) {
                // Don't fail the return if queue notification fails
                System.out.println("Queue notification failed: " + e.getMessage());
            }
        }

        // Send review notification to member
        if (notificationService != null) {
            try {
                notificationService.createNotification(
                        loan.getMemberId(),
                        "REVIEW",
                        "Rate your reading experience",
                        "You recently returned \"" + book.getTitle() + "\". Please rate it out of 5 stars and share your feedback!",
                        loan.getId(),
                        book.getId(),
                        "app",
                        "Sent",
                        null,
                        true
                );
            } catch (Exception e) {
                System.out.println("Review notification failed: " + e.getMessage());
            }
        }

        // activityLogService.logActivity(staffId, "LOAN_RETURNED", loanId, Map.of("memberId", loan.getMemberId()));
        
        // Update member analytics immediately after book return
        try {
            memberAnalyticsService.recomputeForMember(loan.getMemberId());
        } catch (Exception e) {
            System.out.println("Failed to update analytics for member " + loan.getMemberId() + ": " + e.getMessage());
        }

        return mapToLoanResponse(loan);
    }

    public Map<String, Object> renewLoan(String loanId, String staffId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
        if (!List.of("ISSUED", "RENEWED", "Issued", "Renewed").contains(loan.getStatus())) {
            throw new IllegalArgumentException("Cannot renew non-active loan");
        }
        if (loan.getRenewalCount() >= 2) { // stub limit 2
            throw new IllegalArgumentException("Renewal limit exceeded");
        }

        LocalDateTime now = LocalDateTime.now();
        loan.setRenewalCount(loan.getRenewalCount() + 1);
        loan.setDueDate(loan.getDueDate().plusDays(7)); // stub +7 days
        loan.setStatus(LoanStatus.RENEWED.name());
        loan.setUpdatedAt(now);
        loan.setUpdatedBy(staffId);

        loan = loanRepository.save(loan);

        loanEventRepository.save(LoanEvent.builder()
            .loanId(loan.getId())
            .eventType(LoanEventType.RENEWED)
            .timestamp(now)
            .triggeredBy(staffId)
            .metadata(Map.of("renewalCount", loan.getRenewalCount()))
            .createdAt(now)
            .build());

        // activityLogService.logActivity(staffId, "LOAN_RENEWED", loanId, Map.of());

        return mapToLoanResponse(loan);
    }

    private void applyReturnFine(Loan loan, LocalDateTime returnedAt) {
        if (loan.getDueDate() == null) {
            loan.setOverdueDays(0);
            loan.setFineAmount(0.0);
            return;
        }

        int overdueDays = calculateOverdueDays(loan.getDueDate(), returnedAt);
        loan.setOverdueDays(overdueDays);
        loan.setIsOverdue(overdueDays > 0);

        if (overdueDays <= 0) {
            loan.setFineAmount(0.0);
            return;
        }

        double rate = getFineRateForMember(loan.getMemberId());
        double fineAmount = overdueDays * rate;
        loan.setFineAmount(fineAmount);
        upsertFineRecord(loan, overdueDays, rate, fineAmount, returnedAt);
    }

    private int calculateOverdueDays(LocalDateTime dueDate, LocalDateTime asOf) {
        int gracePeriodDays = 0;
        try {
            gracePeriodDays = systemConfigService.getById("loan_settings").getGracePeriodDays();
        } catch (Exception e) {
            gracePeriodDays = 0;
        }
        LocalDateTime fineStart = dueDate.plusDays(Math.max(0, gracePeriodDays));
        long days = ChronoUnit.DAYS.between(fineStart, asOf);
        return (int) Math.max(0, days);
    }

    private double getFineRateForMember(String memberId) {
        try {
            Member member = memberRepository.findById(memberId).orElse(null);
            if (member != null && member.getFineRatePerDay() > 0) {
                return member.getFineRatePerDay();
            }
        } catch (Exception e) {
            System.err.println("Error fetching member fine rate for " + memberId + ": " + e.getMessage());
        }
        try {
            double configuredRate = systemConfigService.getById("loan_settings").getFineRatePerDay();
            return configuredRate > 0 ? configuredRate : 10.0;
        } catch (Exception e) {
            return 10.0;
        }
    }

    private void upsertFineRecord(Loan loan, int overdueDays, double rate, double fineAmount, LocalDateTime createdAt) {
        FineRecord fineRecord = fineRecordRepository.findByLoanId(loan.getId()).orElseGet(() -> FineRecord.builder()
            .id("FINE" + System.currentTimeMillis() + Math.abs(loan.getId().hashCode()))
            .loanId(loan.getId())
            .memberId(loan.getMemberId())
            .bookId(loan.getBookId())
            .bookTitle(loan.getBookTitle())
            .status("Pending")
            .createdAt(createdAt)
            .build());
        if ("Pending".equalsIgnoreCase(fineRecord.getStatus())) {
            fineRecord.setOverdueDays(overdueDays);
            fineRecord.setRatePerDay(rate);
            fineRecord.setTotalAmount(fineAmount);
            fineRecord.setCreatedAt(fineRecord.getCreatedAt() == null ? createdAt : fineRecord.getCreatedAt());
            fineRecordRepository.save(fineRecord);
        }
    }

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final BookCategoryRepository bookCategoryRepository;
    private final ActivityLogService activityLogService;
    private final BookQueueService bookQueueService;
    private final LoanEventRepository loanEventRepository;
    private final NotificationService notificationService;
    private final FineRecordRepository fineRecordRepository;
    private final MemberAnalyticsService memberAnalyticsService;
    private final SystemConfigService systemConfigService;
    private final RealtimeEventService realtimeEventService;
    private final MongoTemplate mongoTemplate;

    public Map<String, Object> getLoans() {
        try {
            // Limit to recent loans to avoid memory issues
            List<Loan> loans = loanRepository.findAll();
            // If there are too many loans, limit to the most recent 1000
            if (loans.size() > 1000) {
                loans = loans.stream()
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .limit(1000)
                    .collect(Collectors.toList());
            }
            
            List<Map<String, Object>> items = loans.stream()
                .map(loan -> {
                    try {
                        return mapToLoanResponse(loan);
                    } catch (Exception e) {
                        System.err.println("Error mapping loan response for loan ID " + loan.getId() + ": " + e.getMessage());
                        // Return a minimal response mapping to avoid complete failure
                        Map<String, Object> minimalMap = new LinkedHashMap<>();
                        minimalMap.put("loanId", loan.getId());
                        minimalMap.put("id", loan.getId());
                        minimalMap.put("memberId", loan.getMemberId());
                        minimalMap.put("bookId", loan.getBookId());
                        minimalMap.put("bookIsbn", loan.getBookIsbn());
                        minimalMap.put("bookTitle", loan.getBookTitle());
                        minimalMap.put("status", loan.getStatus());
                        minimalMap.put("issuedAt", loan.getIssuedAt());
                        minimalMap.put("dueDate", loan.getDueDate());
                        minimalMap.put("returnedAt", loan.getReturnedAt());
                        minimalMap.put("error", "Failed to load complete loan details");
                        return minimalMap;
                    }
                })
                .collect(Collectors.toList());
            return Map.of("loans", items, "total", items.size(), "note", items.size() == 1000 ? "Showing latest 1000 loans" : "All loans loaded");
        } catch (Exception e) {
            System.err.println("Error in getLoans: " + e.getMessage());
            e.printStackTrace();
            return Map.of("loans", List.of(), "total", 0, "error", "Failed to load loans: " + e.getMessage());
        }
    }

    public Map<String, Object> getMyActiveLoans(String memberId, String status) {
        List<String> statuses = (status != null && !status.isEmpty()) ? List.of(status.toUpperCase(), status) : List.of("ISSUED", "RENEWED", "OVERDUE", "Active", "Issued", "Renewed", "Overdue");
        List<Loan> loans = loanRepository.findByMemberIdAndStatusInOrderByDueDateAsc(memberId, statuses);
        List<Map<String, Object>> items = loans.stream().map(this::mapToLoanResponse).collect(Collectors.toList());
        return Map.of("loans", items, "total", items.size());
    }

    public Map<String, Object> getCurrentlyReading(String memberId) {
        List<Loan> reading = loanRepository.findByMemberIdAndStatusIn(memberId, List.of("ISSUED", "RENEWED"))
                .stream().filter(l -> Boolean.TRUE.equals(l.getIsCurrentlyReading())).collect(Collectors.toList());
        List<Map<String, Object>> items = reading.stream().map(this::mapToLoanResponse).collect(Collectors.toList());
        return Map.of("reading", items);
    }

    public Map<String, Object> markAsCurrentlyReading(String memberId, String loanId, ReadingProgressRequest request) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
        if (!loan.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("Loan does not belong to member");
        }
        loan.setIsCurrentlyReading(true);
        if (request.getCurrentChapter() != null) loan.setCurrentChapter(request.getCurrentChapter());
        if (loan.getReadingStartedAt() == null) loan.setReadingStartedAt(LocalDateTime.now());
        loan.setUpdatedAt(LocalDateTime.now());
        loanRepository.save(loan);
        return Map.of("message", "Progress updated", "loanId", loanId);
    }

    public Map<String, Object> getMyReadingHistory(String memberId, int page, String genre) {
        List<Loan> history = new ArrayList<>();
        history.addAll(loanRepository.findByMemberIdAndStatusOrderByReturnedAtDesc(memberId, "RETURNED"));
        history.addAll(loanRepository.findByMemberIdAndStatusOrderByReturnedAtDesc(memberId, "Returned"));
        // Simple filtering for now as genre filter needs category lookup
        List<Map<String, Object>> items = history.stream().map(this::mapToLoanResponse).collect(Collectors.toList());
        return Map.of("history", items, "total", items.size(), "page", page);
    }

    public List<Loan> detectOverdues() {
        LocalDateTime now = LocalDateTime.now();
        List<Loan> overdueLoans = new ArrayList<>();
        List<String> activeStatuses = List.of("ISSUED", "RENEWED");

        // First, process loans that are not yet marked overdue
        for (String status : activeStatuses) {
            List<Loan> loans = loanRepository.findByDueDateBeforeAndStatus(now, status);
            for (Loan loan : loans) {
                if (!Boolean.TRUE.equals(loan.getIsOverdue())) {
                    loan.setIsOverdue(true);
                    loan.setStatus("OVERDUE");
                    // Calculate overdue days and fine amount
                    int overdueDays = calculateOverdueDays(loan.getDueDate(), now);
                    loan.setOverdueDays((int) overdueDays);
                    double rate = getFineRateForMember(loan.getMemberId());
                    double totalFine = overdueDays * rate;
                    loan.setFineAmount(totalFine);
                    loan.setUpdatedAt(now);
                    loanRepository.save(loan);

                    // Create FineRecord for this overdue loan (if not exists)
                    if (!fineRecordRepository.existsByLoanId(loan.getId())) {
                        FineRecord fineRecord = FineRecord.builder()
                            .id("FINE" + System.currentTimeMillis() + Math.abs(loan.getId().hashCode()))
                            .loanId(loan.getId())
                            .memberId(loan.getMemberId())
                            .bookId(loan.getBookId())
                            .bookTitle(loan.getBookTitle())
                            .overdueDays((int) overdueDays)
                            .ratePerDay(rate)
                            .totalAmount(totalFine)
                            .status("Pending")
                            .createdAt(now)
                            .build();
                        fineRecordRepository.save(fineRecord);
                    }

                    overdueLoans.add(loan);
                }
            }
        }

        // Second, ensure all OVERDUE status loans have FineRecords
        List<Loan> existingOverdueLoans = loanRepository.findByStatus("OVERDUE");
        for (Loan loan : existingOverdueLoans) {
            if (!fineRecordRepository.existsByLoanId(loan.getId())) {
                // Calculate/update fine amount
                int overdueDays = calculateOverdueDays(loan.getDueDate(), now);
                double rate = getFineRateForMember(loan.getMemberId());
                double totalFine = overdueDays * rate;

                // Update loan
                loan.setOverdueDays((int) overdueDays);
                loan.setFineAmount(totalFine);
                loan.setUpdatedAt(now);
                loanRepository.save(loan);

                // Create FineRecord
                FineRecord fineRecord = FineRecord.builder()
                    .id("FINE" + System.currentTimeMillis() + Math.abs(loan.getId().hashCode()))
                    .loanId(loan.getId())
                    .memberId(loan.getMemberId())
                    .bookId(loan.getBookId())
                    .bookTitle(loan.getBookTitle())
                    .overdueDays((int) overdueDays)
                    .ratePerDay(rate)
                    .totalAmount(totalFine)
                    .status("Pending")
                    .createdAt(now)
                    .build();
                fineRecordRepository.save(fineRecord);
                overdueLoans.add(loan);
            }
        }

        return overdueLoans;
    }

    private Map<String, Object> mapToLoanResponse(Loan loan) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("loanId", loan.getId());
        map.put("id", loan.getId());
        map.put("memberId", loan.getMemberId());
        map.put("bookId", loan.getBookId());

        // Some legacy loans may have bookIsbn missing. Derive it from bookId if possible
        String bookIsbn = loan.getBookIsbn();
        if (bookIsbn == null || bookIsbn.trim().isEmpty()) {
            try {
                Book bookForIsbn = bookRepository.findById(loan.getBookId()).orElse(null);
                if (bookForIsbn != null) {
                    bookIsbn = bookForIsbn.getIsbn();
                }
            } catch (Exception e) {
                System.err.println("Error deriving bookIsbn for loan " + loan.getId() + ": " + e.getMessage());
            }
        }

        map.put("bookIsbn", bookIsbn);
        map.put("bookTitle", loan.getBookTitle());
        map.put("status", loan.getStatus());
        map.put("issuedAt", loan.getIssuedAt());
        map.put("dueDate", loan.getDueDate());
        map.put("returnedAt", loan.getReturnedAt());
        map.put("overdueDays", loan.getOverdueDays());
        map.put("fineAmount", loan.getFineAmount());
        map.put("finePaid", loan.getFinePaid());
        map.put("isCurrentlyReading", loan.getIsCurrentlyReading());
        map.put("currentChapter", loan.getCurrentChapter());
        
        // Safely fetch related data with exception handling
        try {
            Book book = bookRepository.findById(loan.getBookId()).orElse(null);
            if (book != null) {
                map.put("bookCoverImageUrl", book.getCoverImageUrl() != null ? book.getCoverImageUrl() : "");
                
                // Get author name safely
                String authorName = "Unknown";
                if (book.getAuthorIds() != null && !book.getAuthorIds().isEmpty()) {
                    try {
                        authorName = authorRepository.findById(book.getAuthorIds().get(0))
                                .map(Author::getName)
                                .orElse("Unknown");
                    } catch (Exception e) {
                        System.err.println("Error fetching author for book " + book.getId() + ": " + e.getMessage());
                        authorName = "Unknown";
                    }
                }
                map.put("author", authorName);
                map.put("genre", primaryCategoryName(book));
            } else {
                map.put("bookCoverImageUrl", "");
                map.put("author", "Unknown");
                map.put("genre", "");
            }
        } catch (Exception e) {
            System.err.println("Error fetching book details for loan " + loan.getId() + ": " + e.getMessage());
            map.put("bookCoverImageUrl", "");
            map.put("author", "Unknown");
            map.put("genre", "");
        }
        
        // Get member name safely
        try {
            memberRepository.findById(loan.getMemberId())
                    .ifPresent(member -> map.put("memberName", member.getName()));
        } catch (Exception e) {
            System.err.println("Error fetching member for loan " + loan.getId() + ": " + e.getMessage());
        }
        
        // Calculate days held
        map.put("daysHeld", loan.getIssuedAt() != null && loan.getReturnedAt() != null
                ? ChronoUnit.DAYS.between(loan.getIssuedAt(), loan.getReturnedAt())
                : 0);
        
        // Calculate fine dynamically for ALL unreturned loans that are overdue
        double fineAmount = 0.0;
        int currentOverdueDays = 0;
        
        // Only calculate fine for unreturned loans (status not RETURNED or CANCELLED)
        if (loan.getReturnedAt() == null && 
            !List.of("RETURNED", "CANCELLED", "Returned", "Cancelled").contains(loan.getStatus())) {
            try {
                if (loan.getDueDate() != null) {
                    LocalDateTime now = LocalDateTime.now();
                    
                    // Apply grace period if configured
                    int gracePeriodDays = 0;
                    try {
                        gracePeriodDays = systemConfigService.getById("loan_settings").getGracePeriodDays();
                    } catch (Exception e) {
                        gracePeriodDays = 0;
                    }
                    
                    LocalDateTime fineStartDate = loan.getDueDate().plusDays(Math.max(0, gracePeriodDays));
                    
                    // Only charge fine if past grace period
                    if (now.isAfter(fineStartDate)) {
                        currentOverdueDays = (int) ChronoUnit.DAYS.between(fineStartDate, now);
                        if (currentOverdueDays > 0) {
                            double rate = getFineRateForMember(loan.getMemberId());
                            fineAmount = currentOverdueDays * rate;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error calculating fine for loan " + loan.getId() + ": " + e.getMessage());
            }
        }
        
        // Use calculated fine if it's greater than stored fine (for unreturned books)
        if (loan.getReturnedAt() == null && fineAmount > loan.getFineAmount()) {
            map.put("fineAmount", fineAmount);
            map.put("overdueDays", currentOverdueDays);
        } else {
            map.put("fineAmount", loan.getFineAmount());
        }
        
        return map;
    }

    private String primaryCategoryName(Book book) {
        if (book == null || book.getCategoryIds() == null || book.getCategoryIds().isEmpty()) {
            return "";
        }
        try {
            String firstCategoryId = book.getCategoryIds().get(0);
            if (firstCategoryId == null || firstCategoryId.trim().isEmpty()) {
                return "";
            }
            return bookCategoryRepository.findById(firstCategoryId)
                    .map(BookCategory::getName)
                    .orElse("");
        } catch (Exception e) {
            System.err.println("Error fetching category for book " + book.getId() + ": " + e.getMessage());
            return "";
        }
    }
}
