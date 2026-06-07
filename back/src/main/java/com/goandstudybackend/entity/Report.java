package com.goandstudybackend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reports")
public class Report {

    @Id
    private String id;
    private String reportType;
    private String title;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private String generatedByAdminId;
    @Builder.Default
    private Map<String, Object> data = new LinkedHashMap<>();
    @Builder.Default
    private Map<String, Object> summary = new LinkedHashMap<>();
    @Builder.Default
    private String format = "json";
    private int fileSize;
    @Indexed
    private LocalDateTime createdAt;
    
    // Manual builder pattern to fix compilation issues
    public static ReportBuilder builder() {
        return new ReportBuilder();
    }
    
    public static class ReportBuilder {
        private String id;
        private String reportType;
        private String title;
        private LocalDateTime periodStart;
        private LocalDateTime periodEnd;
        private String generatedByAdminId;
        private Map<String, Object> data = new LinkedHashMap<>();
        private Map<String, Object> summary = new LinkedHashMap<>();
        private String format = "json";
        private int fileSize;
        private LocalDateTime createdAt;
        
        public ReportBuilder id(String id) {
            this.id = id;
            return this;
        }
        
        public ReportBuilder reportType(String reportType) {
            this.reportType = reportType;
            return this;
        }
        
        public ReportBuilder title(String title) {
            this.title = title;
            return this;
        }
        
        public ReportBuilder periodStart(LocalDateTime periodStart) {
            this.periodStart = periodStart;
            return this;
        }
        
        public ReportBuilder periodEnd(LocalDateTime periodEnd) {
            this.periodEnd = periodEnd;
            return this;
        }
        
        public ReportBuilder generatedByAdminId(String generatedByAdminId) {
            this.generatedByAdminId = generatedByAdminId;
            return this;
        }
        
        public ReportBuilder data(Map<String, Object> data) {
            this.data = data;
            return this;
        }
        
        public ReportBuilder summary(Map<String, Object> summary) {
            this.summary = summary;
            return this;
        }
        
        public ReportBuilder format(String format) {
            this.format = format;
            return this;
        }
        
        public ReportBuilder fileSize(int fileSize) {
            this.fileSize = fileSize;
            return this;
        }
        
        public ReportBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public Report build() {
            Report report = new Report();
            report.id = this.id;
            report.reportType = this.reportType;
            report.title = this.title;
            report.periodStart = this.periodStart;
            report.periodEnd = this.periodEnd;
            report.generatedByAdminId = this.generatedByAdminId;
            report.data = this.data;
            report.summary = this.summary;
            report.format = this.format;
            report.fileSize = this.fileSize;
            report.createdAt = this.createdAt;
            return report;
        }
    }
    
    // Manual getters to fix compilation issues
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getReportType() {
        return reportType;
    }
    
    public void setReportType(String reportType) {
        this.reportType = reportType;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
