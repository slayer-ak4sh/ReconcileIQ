package com.reconcileiq.ingestion;

public record SourceDTO(Long id, String name, String code, String partnerType) {
    public static SourceDTO fromEntity(Source s) {
        return new SourceDTO(s.getId(), s.getName(), s.getCode(), s.getPartnerType());
    }
}
