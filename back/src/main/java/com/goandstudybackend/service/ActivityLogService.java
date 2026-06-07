package com.goandstudybackend.service;

import com.goandstudybackend.entity.ActivityLog;
import com.goandstudybackend.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public ActivityLog log(String eventType, String actorId, String actorRole, String targetCollection, String targetId) {
        return log(eventType, actorId, actorRole, targetCollection, targetId, null, null);
    }

    public ActivityLog log(String eventType,
                           String actorId,
                           String actorRole,
                           String targetCollection,
                           String targetId,
                           Map<String, Object> changes,
                           Map<String, Object> metadata) {
        ActivityLog activityLog = ActivityLog.builder()
                .eventType(eventType)
                .actorId(actorId)
                .actorRole(actorRole)
                .targetCollection(targetCollection)
                .targetId(targetId)
                .changes(changes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(changes))
                .metadata(metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata))
                .createdAt(LocalDateTime.now())
                .build();
        return activityLogRepository.save(activityLog);
    }
}
