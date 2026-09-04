package com.localsync.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DuplicateGroupDto {
    private String hash;
    private List<String> files;
    private int fileCount;
    private long sizePerFile;
    private long wastedBytes;
}
