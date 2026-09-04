package com.localsync.service;

import com.localsync.dto.FileInfoDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileManagerService {

    @Value("${localsync.allowed-roots:#{null}}")
    private String allowedRootsConfig;

    private List<Path> getAllowedRoots() {
        List<Path> roots = new ArrayList<>();
        // Automatically allow all active system drives (C:\, D:\, etc.)
        File[] drives = File.listRoots();
        if (drives != null) {
            for (File drive : drives) {
                try {
                    roots.add(drive.toPath().toAbsolutePath().normalize());
                } catch (Exception ignored) {}
            }
        }
        try {
            roots.add(Path.of(System.getProperty("user.home")).toAbsolutePath().normalize());
        } catch (Exception ignored) {}

        if (allowedRootsConfig != null && !allowedRootsConfig.isBlank()) {
            for (String r : allowedRootsConfig.split(",")) {
                try {
                    roots.add(Path.of(r.trim()).toAbsolutePath().normalize());
                } catch (Exception ignored) {}
            }
        }
        return roots;
    }

    /**
     * Validate that the requested path is within allowed roots (sandbox).
     */
    public Path validateAndResolvePath(String rawPath) throws IOException {
        Path requested = Path.of(rawPath).normalize().toAbsolutePath();
        for (Path root : getAllowedRoots()) {
            Path absRoot = root.toAbsolutePath().normalize();
            if (requested.startsWith(absRoot)) {
                return requested;
            }
        }
        throw new SecurityException("Access denied: path outside allowed roots: " + rawPath);
    }

    /**
     * List contents of a directory.
     */
    public List<FileInfoDto> listDirectory(String rawPath) throws IOException {
        Path dir = validateAndResolvePath(rawPath);

        if (!Files.exists(dir)) throw new NoSuchFileException("Path not found: " + rawPath);
        if (!Files.isDirectory(dir)) throw new NotDirectoryException("Not a directory: " + rawPath);

        List<FileInfoDto> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                try {
                    BasicFileAttributes attrs = Files.readAttributes(entry, BasicFileAttributes.class);
                    result.add(FileInfoDto.builder()
                        .name(entry.getFileName().toString())
                        .path(entry.toString())
                        .isDirectory(attrs.isDirectory())
                        .size(attrs.isDirectory() ? -1 : attrs.size())
                        .lastModified(attrs.lastModifiedTime().toMillis())
                        .extension(getExtension(entry.getFileName().toString()))
                        .mimeType(attrs.isDirectory() ? "directory" : guessMimeType(entry.getFileName().toString()))
                        .build());
                } catch (Exception e) {
                    log.debug("Skipping inaccessible file: {}", entry);
                }
            }
        }

        result.sort(Comparator.comparing((FileInfoDto f) -> Boolean.valueOf(f.isDirectory())).reversed()
            .thenComparing(f -> f.getName().toLowerCase()));
        return result;
    }

    /**
     * Get storage roots (drives on Windows, home dirs on Linux/Mac).
     */
    public List<FileInfoDto> getStorageRoots() {
        List<FileInfoDto> roots = new ArrayList<>();
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            // Windows: list available drives
            File[] drives = File.listRoots();
            if (drives != null) {
                for (File drive : drives) {
                    if (drive.exists() && drive.canRead()) {
                        roots.add(FileInfoDto.builder()
                            .name(drive.getAbsolutePath())
                            .path(drive.getAbsolutePath())
                            .isDirectory(true)
                            .size(drive.getTotalSpace())
                            .lastModified(0)
                            .extension("")
                            .mimeType("drive")
                            .build());
                    }
                }
            }
        }

        // Always add user home standard folders
        String home = System.getProperty("user.home");
        List<String[]> stdFolders = List.of(
            new String[]{"Documents", home + "/Documents"},
            new String[]{"Downloads", home + "/Downloads"},
            new String[]{"Pictures", home + "/Pictures"},
            new String[]{"Videos", home + "/Videos"},
            new String[]{"Desktop", home + "/Desktop"},
            new String[]{"Music", home + "/Music"}
        );
        for (String[] folder : stdFolders) {
            Path p = Path.of(folder[1]);
            if (Files.exists(p)) {
                roots.add(0, FileInfoDto.builder()
                    .name(folder[0])
                    .path(folder[1])
                    .isDirectory(true)
                    .size(-1)
                    .lastModified(0)
                    .extension("")
                    .mimeType("folder")
                    .build());
            }
        }

        return roots;
    }

    /**
     * Delete a file or directory.
     */
    public void delete(String rawPath) throws IOException {
        Path target = validateAndResolvePath(rawPath);
        if (Files.isDirectory(target)) {
            FileUtils.deleteDirectory(target.toFile());
        } else {
            Files.delete(target);
        }
        log.info("Deleted: {}", target);
    }

    /**
     * Create a directory.
     */
    public void createDirectory(String rawPath) throws IOException {
        Path dir = validateAndResolvePath(rawPath);
        Files.createDirectories(dir);
    }

    /**
     * Get file info for a single path.
     */
    public FileInfoDto getFileInfo(String rawPath) throws IOException {
        Path p = validateAndResolvePath(rawPath);
        BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
        return FileInfoDto.builder()
            .name(p.getFileName().toString())
            .path(p.toString())
            .isDirectory(attrs.isDirectory())
            .size(attrs.isDirectory() ? -1 : attrs.size())
            .lastModified(attrs.lastModifiedTime().toMillis())
            .extension(getExtension(p.getFileName().toString()))
            .mimeType(guessMimeType(p.getFileName().toString()))
            .build();
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot >= 0) ? filename.substring(dot + 1).toLowerCase() : "";
    }

    private String guessMimeType(String filename) {
        String ext = getExtension(filename).toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "mp4" -> "video/mp4";
            case "mkv" -> "video/x-matroska";
            case "mp3" -> "audio/mpeg";
            case "pdf" -> "application/pdf";
            case "zip" -> "application/zip";
            case "txt" -> "text/plain";
            case "doc", "docx" -> "application/msword";
            case "xls", "xlsx" -> "application/vnd.ms-excel";
            case "apk" -> "application/vnd.android.package-archive";
            default -> "application/octet-stream";
        };
    }
}
