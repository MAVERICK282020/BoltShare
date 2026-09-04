package com.localsync.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class StorageReportDto {
    private String rootPath;
    private long totalSizeBytes;
    private long fileCount;
    private Map<String, Long> categorySizes;
}
