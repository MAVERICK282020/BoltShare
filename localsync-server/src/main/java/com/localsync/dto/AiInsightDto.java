package com.localsync.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiInsightDto {
    private String type;        // "WARNING", "INFO", "TIP"
    private String title;
    private String description;
    private String actionLabel;
    private String actionType;
}
