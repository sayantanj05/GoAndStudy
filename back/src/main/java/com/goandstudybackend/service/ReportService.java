package com.goandstudybackend.service;

import com.goandstudybackend.dto.request.GenerateReportRequest;
import com.goandstudybackend.entity.AiLog;
import com.goandstudybackend.entity.FineRecord;
import com.goandstudybackend.entity.GenreAnalytics;
import com.goandstudybackend.entity.Loan;
import com.goandstudybackend.entity.Member;
import com.goandstudybackend.entity.MemberAnalytics;
import com.goandstudybackend.entity.Report;
import com.goandstudybackend.repository.ActivityLogRepository;
import com.goandstudybackend.repository.AiLogRepository;
import com.goandstudybackend.repository.BookRepository;
import com.goandstudybackend.repository.FineRecordRepository;
import com.goandstudybackend.repository.GenreAnalyticsRepository;
import com.goandstudybackend.repository.LoanRepository;
import com.goandstudybackend.repository.MemberAnalyticsRepository;
import com.goandstudybackend.repository.MemberRepository;
import com.goandstudybackend.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final LoanRepository loanRepository;
    private final FineRecordRepository fineRecordRepository;
    private final GenreAnalyticsRepository genreAnalyticsRepository;
    private final MemberAnalyticsRepository memberAnalyticsRepository;
    private final AiLogRepository aiLogRepository;
    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ActivityLogService activityLogService;

    public Map<String, Object> generateReport(GenerateReportRequest request, String adminId) {
        Map<String, Object> data = switch (request.getReportType()) {
            case "MONTHLY_LOANS" -> monthlyLoans(request.getPeriodStart(), request.getPeriodEnd());
            case "OVERDUE_SUMMARY" -> overdueSummary(request.getPeriodStart(), request.getPeriodEnd());
            case "GENRE_ANALYTICS" -> Map.of("genres", genreAnalyticsRepository.findAllByOrderByLoansLast30DaysDesc());
            case "MEMBER_ACTIVITY" -> Map.of("members", memberAnalyticsRepository.findAll());
            case "FINE_COLLECTION" -> fineCollection(request.getPeriodStart(), request.getPeriodEnd());
            case "AI_USAGE" -> aiUsage(request.getPeriodStart(), request.getPeriodEnd());
            case "STAFF_PERFORMANCE" -> staffPerformance(request.getPeriodStart(), request.getPeriodEnd());
            default -> throw new IllegalArgumentException("Unsupported report type");
        };

        Map<String, Object> summary = summarise(data);
        Report report = reportRepository.save(Report.builder()
                .reportType(request.getReportType())
                .title(request.getReportType() + " Report")
                .periodStart(request.getPeriodStart())
                .periodEnd(request.getPeriodEnd())
                .generatedByAdminId(adminId)
                .data(data)
                .summary(summary)
                .fileSize(data.toString().length())
                .createdAt(LocalDateTime.now())
                .build());

        activityLogService.log("REPORT_GENERATED", adminId, "ROLE_ADMIN", "reports", report.getId());
        return Map.of(
                "reportId", report.getId(),
                "title", report.getTitle(),
                "reportType", report.getReportType(),
                "createdAt", report.getCreatedAt()
        );
    }

    public Map<String, Object> getAllReports(String reportType, int page) {
        List<Report> reports = (reportType == null || reportType.isBlank())
                ? reportRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(page - 1, 0), 20)).getContent()
                : reportRepository.findByReportTypeOrderByCreatedAtDesc(reportType, PageRequest.of(Math.max(page - 1, 0), 20)).getContent();
        long total = reportType == null || reportType.isBlank() ? reportRepository.count() : reports.size();
        return Map.of("reports", reports, "total", total);
    }

    public Report getReportById(String reportId) {
        return reportRepository.findById(reportId).orElseThrow(() -> new IllegalArgumentException("Report not found"));
    }

    public Map<String, Object> getAdminDashboard() {
        try {
            // Simple synchronous approach to avoid timeout issues
            long totalMembers = memberRepository.countByIsActive(true);
            long totalBooks = bookRepository.countByIsDeletedFalse();
            List<Loan> allLoans = loanRepository.findAll();
            long activeLoans = allLoans.stream()
                    .filter(loan -> isActiveDashboardStatus(loan.getStatus()) || isActiveDashboardStatus(loan.getLoanStatus()))
                    .count();
            long overdueLoans = allLoans.stream()
                    .filter(loan -> "OVERDUE".equalsIgnoreCase(String.valueOf(loan.getStatus()))
                            || "OVERDUE".equalsIgnoreCase(String.valueOf(loan.getLoanStatus())))
                    .count();

            Map<String, Long> membershipStats = getMembershipBreakdownCounts();
            Map<String, Object> aiUsage = aiUsage(null, null);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("totalMembers", totalMembers);
            response.put("totalBooks", totalBooks);
            response.put("activeLoans", activeLoans);
            response.put("overdueLoans", overdueLoans);
            
            // Add optional data with try-catch to prevent failures
            try {
                response.put("loansThisWeek", loansThisWeek());
            } catch (Exception e) {
                response.put("loansThisWeek", List.of());
            }
            
            try {
                response.put("registrationsThisWeek", registrationsThisWeek());
            } catch (Exception e) {
                response.put("registrationsThisWeek", List.of());
            }
            
            response.put("membershipBreakdown", List.of(
                    Map.of("type", "BASIC", "count", membershipStats.getOrDefault("BASIC", 0L)),
                    Map.of("type", "STANDARD", "count", membershipStats.getOrDefault("STANDARD", 0L)),
                    Map.of("type", "PREMIUM", "count", membershipStats.getOrDefault("PREMIUM", 0L)),
                    Map.of("type", "STUDENT", "count", membershipStats.getOrDefault("STUDENT", 0L))
            ));
            
            try {
                response.put("topGenres", genreAnalyticsRepository.findTop5ByOrderByLoansLast7DaysDesc().stream()
                        .map(item -> Map.<String, Object>of(
                                "genreName", item.getGenreName(),
                                "loansLast7Days", item.getLoansLast7Days(),
                                "trendScore", item.getTrendScore()
                        )).toList());
            } catch (Exception e) {
                response.put("topGenres", List.of());
            }
            
            try {
                response.put("fineCollectionTrend", fineCollectionTrend());
            } catch (Exception e) {
                response.put("fineCollectionTrend", Map.of());
            }
            
            response.put("aiUsage", aiUsage);
            
            try {
                response.put("recentActivity", activityLogRepository.findTop10ByOrderByCreatedAtDesc().stream()
                        .map(log -> Map.<String, Object>of(
                                "eventType", log.getEventType(),
                                "actorId", log.getActorId(),
                                "actorRole", log.getActorRole(),
                                "targetId", log.getTargetId(),
                                "createdAt", log.getCreatedAt()
                        )).toList());
            } catch (Exception e) {
                response.put("recentActivity", List.of());
            }
            
            try {
                response.put("overdueTable", overdueSummary(null, null).get("loans"));
            } catch (Exception e) {
                response.put("overdueTable", List.of());
            }
            
            return response;
        } catch (Exception e) {
            // Return minimal dashboard data if there's an error
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("totalMembers", 0L);
            errorResponse.put("totalBooks", 0L);
            errorResponse.put("activeLoans", 0L);
            errorResponse.put("overdueLoans", 0L);
            errorResponse.put("loansThisWeek", List.of());
            errorResponse.put("registrationsThisWeek", List.of());
            errorResponse.put("membershipBreakdown", List.of());
            errorResponse.put("topGenres", List.of());
            errorResponse.put("fineCollectionTrend", Map.of());
            errorResponse.put("aiUsage", Map.of());
            errorResponse.put("recentActivity", List.of());
            errorResponse.put("overdueTable", List.of());
            return errorResponse;
        }
    }

    public Map<String, Object> getGenreAnalytics() {
        return Map.of("genres", genreAnalyticsRepository.findAllByOrderByLoansLast30DaysDesc());
    }

    private boolean isActiveDashboardStatus(String status) {
        if (status == null) return false;
        return List.of("ISSUED", "RENEWED", "OVERDUE", "ACTIVE").contains(status.trim().toUpperCase());
    }

    public Map<String, Object> getAiUsageStats(LocalDateTime fromDate, LocalDateTime toDate) {
        return aiUsage(fromDate, toDate);
    }

    public byte[] exportReportAsCsv(String reportType, LocalDateTime from, LocalDateTime to) {
        Map<String, Object> data = getReportData(reportType, from, to);
        return convertToCsv(data);
    }

    public byte[] exportReportAsTsv(String reportType, LocalDateTime from, LocalDateTime to) {
        Map<String, Object> data = getReportData(reportType, from, to);
        return convertToTsv(data);
    }

    public byte[] exportReportAsPdf(String reportType, LocalDateTime from, LocalDateTime to) {
        Map<String, Object> data = getReportData(reportType, from, to);
        return convertToPdf(data, reportType);
    }

    private Map<String, Object> getReportData(String reportType, LocalDateTime from, LocalDateTime to) {
        return switch (reportType) {
            case "inventory" -> getAdminDashboard();
            case "circulation" -> monthlyLoans(from, to);
            case "financial" -> fineCollection(from, to);
            case "users" -> Map.of("members", memberAnalyticsRepository.findAll());
            default -> Map.of("data", List.of());
        };
    }

    private byte[] convertToCsv(Map<String, Object> data) {
        StringBuilder csv = new StringBuilder();
        csv.append("Report Generated,").append(LocalDateTime.now()).append("\n\n");

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            csv.append("Section,").append(entry.getKey()).append("\n");
            Object value = entry.getValue();
            if (value instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map) {
                // Write headers
                Map<?, ?> firstItem = (Map<?, ?>) list.get(0);
                csv.append(String.join(",", firstItem.keySet().stream().map(Object::toString).toList())).append("\n");
                // Write rows
                for (Object item : list) {
                    Map<?, ?> map = (Map<?, ?>) item;
                    csv.append(map.values().stream().map(v -> v == null ? "" : v.toString().replace(",", ";")).collect(java.util.stream.Collectors.joining(","))).append("\n");
                }
            } else {
                csv.append("Value,").append(value).append("\n");
            }
            csv.append("\n");
        }
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private byte[] convertToTsv(Map<String, Object> data) {
        StringBuilder tsv = new StringBuilder();
        tsv.append("Report Generated\t").append(LocalDateTime.now()).append("\n\n");

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            tsv.append("Section\t").append(entry.getKey()).append("\n");
            Object value = entry.getValue();
            if (value instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map) {
                Map<?, ?> firstItem = (Map<?, ?>) list.get(0);
                tsv.append(String.join("\t", firstItem.keySet().stream().map(Object::toString).toList())).append("\n");
                for (Object item : list) {
                    Map<?, ?> map = (Map<?, ?>) item;
                    tsv.append(map.values().stream().map(v -> v == null ? "" : v.toString()).collect(java.util.stream.Collectors.joining("\t"))).append("\n");
                }
            } else {
                tsv.append("Value\t").append(value).append("\n");
            }
            tsv.append("\n");
        }
        return tsv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private byte[] convertToPdf(Map<String, Object> data, String reportType) {
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
            com.lowagie.text.Document document = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfWriter.getInstance(document, baos);
            document.open();

            // Title
            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD);
            document.add(new com.lowagie.text.Paragraph(reportType.toUpperCase() + " REPORT", titleFont));
            document.add(new com.lowagie.text.Paragraph("Generated: " + LocalDateTime.now()));
            document.add(new com.lowagie.text.Paragraph("\n"));

            // Content
            com.lowagie.text.Font headerFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font normalFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10);

            for (Map.Entry<String, Object> entry : data.entrySet()) {
                document.add(new com.lowagie.text.Paragraph(entry.getKey(), headerFont));
                Object value = entry.getValue();

                if (value instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map) {
                    // Create table for list data
                    Map<?, ?> firstItem = (Map<?, ?>) list.get(0);
                    int cols = firstItem.size();
                    com.lowagie.text.Table table = new com.lowagie.text.Table(cols);
                    table.setWidth(100);

                    // Headers
                    for (Object key : firstItem.keySet()) {
                        table.addCell(new com.lowagie.text.Phrase(key.toString(), headerFont));
                    }

                    // Data rows
                    for (Object item : list) {
                        Map<?, ?> map = (Map<?, ?>) item;
                        for (Object val : map.values()) {
                            table.addCell(new com.lowagie.text.Phrase(val == null ? "" : val.toString(), normalFont));
                        }
                    }
                    document.add(table);
                } else if (value instanceof Map<?, ?> map) {
                    for (Map.Entry<?, ?> subEntry : map.entrySet()) {
                        document.add(new com.lowagie.text.Paragraph(subEntry.getKey() + ": " + subEntry.getValue(), normalFont));
                    }
                } else {
                    document.add(new com.lowagie.text.Paragraph(value == null ? "" : value.toString(), normalFont));
                }
                document.add(new com.lowagie.text.Paragraph("\n"));
            }

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private Map<String, Object> monthlyLoans(LocalDateTime from, LocalDateTime to) {
        List<Loan> loans = filterByDate(loanRepository.findAll(), from, to, Loan::getIssuedAt);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Loan loan : loans) {
            String month = loan.getIssuedAt() == null ? "" : loan.getIssuedAt().getMonth().name();
            counts.put(month, counts.getOrDefault(month, 0L) + 1);
        }
        return Map.of("monthlyLoans", counts);
    }

    private Map<String, Object> overdueSummary(LocalDateTime from, LocalDateTime to) {
        List<Loan> loans = new ArrayList<>();
        loans.addAll(loanRepository.findByStatus("OVERDUE"));
        loans.addAll(loanRepository.findByStatus("Overdue"));
        loans = filterByDate(loans, from, to, Loan::getDueDate);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Loan loan : loans) {
            Member member = memberRepository.findById(loan.getMemberId()).orElse(null);
            items.add(Map.of(
                    "loanId", loan.getId(),
                    "memberId", loan.getMemberId(),
                    "memberName", member == null ? "" : member.getName(),
                    "bookTitle", loan.getBookTitle(),
                    "dueDate", loan.getDueDate(),
                    "overdueDays", loan.getOverdueDays(),
                    "fineAmount", loan.getFineAmount()
            ));
        }
        return Map.of("loans", items, "count", items.size());
    }

    private Map<String, Object> fineCollection(LocalDateTime from, LocalDateTime to) {
        List<FineRecord> fines = filterByDate(fineRecordRepository.findAll(), from, to, FineRecord::getCreatedAt);
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (FineRecord fine : fines) {
            byStatus.put(fine.getStatus(), byStatus.getOrDefault(fine.getStatus(), 0L) + 1);
        }
        return Map.of("fines", fines, "byStatus", byStatus);
    }

    private Map<String, Object> aiUsage(LocalDateTime from, LocalDateTime to) {
        List<AiLog> logs = from == null && to == null ? aiLogRepository.findAll() : aiLogRepository.findByCreatedAtBetween(from, to);
        int totalCalls = logs.size();
        double avgTokens = logs.stream().mapToInt(AiLog::getTokensUsed).average().orElse(0.0);
        double avgLatency = logs.stream().mapToInt(AiLog::getLatencyMs).average().orElse(0.0);
        int totalTokens = logs.stream().mapToInt(AiLog::getTokensUsed).sum();
        double conversionRate = totalCalls == 0 ? 0.0 : (double) logs.stream().filter(AiLog::isWasActedOn).count() / totalCalls;

        Map<LocalDate, Long> callsByDay = new LinkedHashMap<>();
        for (AiLog log : logs) {
            LocalDate date = log.getCreatedAt() == null ? LocalDate.now() : log.getCreatedAt().toLocalDate();
            callsByDay.put(date, callsByDay.getOrDefault(date, 0L) + 1);
        }

        return Map.of(
                "totalCalls", totalCalls,
                "avgTokensPerCall", avgTokens,
                "avgLatencyMs", avgLatency,
                "totalTokensUsed", totalTokens,
                "conversionRate", conversionRate,
                "callsByDay", callsByDay.entrySet().stream().map(entry -> Map.of("date", entry.getKey(), "count", entry.getValue())).toList()
        );
    }

    private Map<String, Object> staffPerformance(LocalDateTime from, LocalDateTime to) {
        List<Loan> loans = filterByDate(loanRepository.findAll(), from, to, Loan::getIssuedAt);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Loan loan : loans) {
            String staffId = loan.getIssuedByStaffId() == null ? "" : loan.getIssuedByStaffId();
            counts.put(staffId, counts.getOrDefault(staffId, 0L) + 1);
        }
        return Map.of("staffPerformance", counts);
    }

    private List<Map<String, Object>> loansThisWeek() {
        List<Map<String, Object>> response = new ArrayList<>();
        LocalDate today = LocalDate.now();
        List<Loan> allLoans = loanRepository.findAll();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long loansCount = allLoans.stream()
                    .filter(loan -> loan.getIssuedAt() != null && loan.getIssuedAt().toLocalDate().isEqual(date))
                    .count();
            long returnsCount = allLoans.stream()
                    .filter(loan -> loan.getReturnedAt() != null && loan.getReturnedAt().toLocalDate().isEqual(date))
                    .count();
            response.add(Map.of(
                "day", date.getDayOfWeek().name().substring(0, 3),
                "loans", loansCount,
                "returns", returnsCount
            ));
        }
        return response;
    }

    private List<Map<String, Object>> registrationsThisWeek() {
        List<Map<String, Object>> response = new ArrayList<>();
        LocalDate today = LocalDate.now();
        List<Member> members = memberRepository.findAll();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long count = members.stream()
                    .filter(member -> member.getTimeCreated() != null && member.getTimeCreated().toLocalDate().isEqual(date))
                    .count();
            response.add(Map.of("day", date.getDayOfWeek().name().substring(0, 3), "count", count));
        }
        return response;
    }

    private List<Map<String, Object>> fineCollectionTrend() {
        WeekFields weekFields = WeekFields.of(Locale.ENGLISH);
        Map<String, Double> totals = new LinkedHashMap<>();
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        for (FineRecord fine : fineRecordRepository.findAll()) {
            if (!"Paid".equalsIgnoreCase(fine.getStatus()) || fine.getCollectedAt() == null || fine.getCollectedAt().isBefore(threshold)) {
                continue;
            }
            LocalDate collectedDate = fine.getCollectedAt().toLocalDate();
            String week = collectedDate.getYear() + "-W" + collectedDate.get(weekFields.weekOfWeekBasedYear());
            totals.put(week, totals.getOrDefault(week, 0.0) + fine.getTotalAmount());
        }
        return totals.entrySet().stream().map(entry -> Map.<String, Object>of("week", entry.getKey(), "totalCollected", entry.getValue())).toList();
    }

    private Map<String, Long> getMembershipBreakdownCounts() {
        Map<String, Long> stats = new LinkedHashMap<>();
        // Use simple counts instead of loading all members
        try {
            long totalMembers = memberRepository.count();
            // For now, return simple breakdown - can be enhanced later with proper aggregation
            stats.put("BASIC", totalMembers);
            stats.put("STANDARD", 0L);
            stats.put("PREMIUM", 0L);
            stats.put("STUDENT", 0L);
        } catch (Exception e) {
            // Return empty stats if there's an error
            stats.put("BASIC", 0L);
            stats.put("STANDARD", 0L);
            stats.put("PREMIUM", 0L);
            stats.put("STUDENT", 0L);
        }
        return stats;
    }

    private Map<String, Object> summarise(Map<String, Object> data) {
        return Map.of("keys", data.keySet(), "generatedAt", LocalDateTime.now());
    }

    private <T> List<T> filterByDate(List<T> items, LocalDateTime from, LocalDateTime to, java.util.function.Function<T, LocalDateTime> extractor) {
        return items.stream().filter(item -> {
            LocalDateTime value = extractor.apply(item);
            if (value == null) {
                return false;
            }
            boolean matchesFrom = from == null || !value.isBefore(from);
            boolean matchesTo = to == null || !value.isAfter(to);
            return matchesFrom && matchesTo;
        }).toList();
    }
}
