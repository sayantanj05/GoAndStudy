package com.goandstudybackend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingProgressRequest {

    @NotNull
    private Boolean isCurrentlyReading;

    @Size(max = 100)
    private String currentChapter;
    
    // Manual getters and setters to fix compilation issues
    public String getCurrentChapter() {
        return currentChapter;
    }
    
    public void setCurrentChapter(String currentChapter) {
        this.currentChapter = currentChapter;
    }
}
