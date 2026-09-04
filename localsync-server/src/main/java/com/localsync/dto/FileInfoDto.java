package com.localsync.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfoDto {
    private String name;
    private String path;

    @JsonProperty("isDirectory")
    private boolean isDirectory;

    @JsonProperty("directory")
    public boolean getDirectory() {
        return isDirectory;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean directory) {
        this.isDirectory = directory;
    }

    private long size;
    private long lastModified;
    private String extension;
    private String mimeType;
}
