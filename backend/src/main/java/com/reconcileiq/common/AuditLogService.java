package com.reconcileiq.common;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void log(String actorEmail, String action, String resourceType, String resourceId, Map<String, Object> details) {
        String detailsJson;
        try {
            detailsJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(details);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            detailsJson = "{}";
        }
        AuditLog log = new AuditLog(actorEmail, action, resourceType, resourceId, detailsJson);
        auditLogRepository.save(log);
    }

    @Transactional
    public void log(String actorEmail, String action, String resourceType, String resourceId) {
        log(actorEmail, action, resourceType, resourceId, Map.of());
    }
}