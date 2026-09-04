package com.localsync.service;

import com.localsync.dto.AiInsightDto;
import com.localsync.dto.DuplicateGroupDto;
import com.localsync.dto.StorageReportDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AiEngineService {

    private static final long MB = 1024 * 1024L;
    private static final long UNUSED_FILE_SIZE_THRESHOLD = 100 * MB; // Only flag files > 100MB

    /**
     * Analyze storage usage in a directory tree.
     */
    public StorageReportDto analyzeStorage(String rootPath) throws IOException {
        Path root = Path.of(rootPath);
        if (!Files.exists(root)) throw new NoSuchFileException("Path not found: " + rootPath);

        Map<String, Long> categorySizes = new LinkedHashMap<>();
        categorySizes.put("Photos", 0L);
        categorySizes.put("Videos", 0L);
        categorySizes.put("Documents", 0L);
        categorySizes.put("Audio", 0L);
        categorySizes.put("Archives", 0L);
        categorySizes.put("Others", 0L);

        long[] totalSize = {0};
        long[] fileCount = {0};

        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isRegularFile()) {
                        long size = attrs.size();
                        totalSize[0] += size;
                        fileCount[0]++;
                        String category = categorize(file.getFileName().toString());
                        categorySizes.merge(category, size, Long::sum);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE; // Skip inaccessible files
                }
            });
        } catch (SecurityException e) {
            log.warn("Access denied walking tree: {}", rootPath);
        }

        return StorageReportDto.builder()
            .rootPath(rootPath)
            .totalSizeBytes(totalSize[0])
            .fileCount(fileCount[0])
            .categorySizes(categorySizes)
            .build();
    }

    /**
     * Find duplicate files using SHA-256 hashing.
     */
    public List<DuplicateGroupDto> findDuplicates(String rootPath) throws IOException {
        Path root = Path.of(rootPath);
        Map<String, List<String>> hashToFiles = new HashMap<>();

        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (attrs.isRegularFile() && attrs.size() > 0) {
                    try {
                        String hash = sha256(file);
                        hashToFiles.computeIfAbsent(hash, k -> new ArrayList<>()).add(file.toString());
                    } catch (Exception e) {
                        log.debug("Could not hash file: {}", file);
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });

        return hashToFiles.entrySet().stream()
            .filter(e -> e.getValue().size() > 1)
            .map(e -> {
                try {
                    long size = Files.size(Path.of(e.getValue().get(0)));
                    return DuplicateGroupDto.builder()
                        .hash(e.getKey())
                        .files(e.getValue())
                        .fileCount(e.getValue().size())
                        .sizePerFile(size)
                        .wastedBytes(size * (e.getValue().size() - 1))
                        .build();
                } catch (IOException ex) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .sorted(Comparator.comparingLong(DuplicateGroupDto::getWastedBytes).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Find files not accessed recently.
     */
    public List<Map<String, Object>> findUnusedFiles(String rootPath, int days) throws IOException {
        Path root = Path.of(rootPath);
        Instant cutoff = Instant.now().minusSeconds((long) days * 86400);
        List<Map<String, Object>> result = new ArrayList<>();

        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (attrs.isRegularFile()
                    && attrs.size() >= UNUSED_FILE_SIZE_THRESHOLD
                    && attrs.lastModifiedTime().toInstant().isBefore(cutoff)) {
                    result.add(Map.of(
                        "path", file.toString(),
                        "name", file.getFileName().toString(),
                        "sizeBytes", attrs.size(),
                        "lastModified", LocalDateTime.ofInstant(
                            attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault()).toString()
                    ));
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });

        result.sort(Comparator.comparingLong(m -> -((long) m.get("sizeBytes"))));
        return result;
    }

    /**
     * Generate AI insights and recommendations.
     */
    public List<AiInsightDto> generateInsights(String rootPath) throws IOException {
        List<AiInsightDto> insights = new ArrayList<>();
        StorageReportDto report = analyzeStorage(rootPath);
        List<DuplicateGroupDto> duplicates = findDuplicates(rootPath);
        List<Map<String, Object>> unused = findUnusedFiles(rootPath, 30);

        // Duplicate insight
        if (!duplicates.isEmpty()) {
            long totalWasted = duplicates.stream().mapToLong(DuplicateGroupDto::getWastedBytes).sum();
            insights.add(AiInsightDto.builder()
                .type("WARNING")
                .title(String.format("%.1f GB duplicate files detected", totalWasted / (1024.0 * 1024 * 1024)))
                .description(duplicates.size() + " groups of duplicate files found. Removing duplicates can free up space.")
                .actionLabel("View Duplicates")
                .actionType("show_duplicates")
                .build());
        }

        // Unused large files
        if (!unused.isEmpty()) {
            long unusedSize = unused.stream().mapToLong(m -> (long) m.get("sizeBytes")).sum();
            insights.add(AiInsightDto.builder()
                .type("INFO")
                .title(String.format("%.1f GB in files unused for 30+ days", unusedSize / (1024.0 * 1024 * 1024)))
                .description(unused.size() + " large files haven't been modified in 30+ days. Consider archiving them.")
                .actionLabel("View Files")
                .actionType("show_unused")
                .build());
        }

        // Video files recommendation
        Long videoSize = report.getCategorySizes().get("Videos");
        if (videoSize != null && videoSize > 5L * 1024 * 1024 * 1024) {
            insights.add(AiInsightDto.builder()
                .type("TIP")
                .title(String.format("%.1f GB of videos found", videoSize / (1024.0 * 1024 * 1024)))
                .description("Consider moving large video files to your laptop for backup and to free phone storage.")
                .actionLabel("Browse Videos")
                .actionType("browse_videos")
                .build());
        }

        return insights;
    }

    // ========== Private Helpers ==========

    private String sha256(Path file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream is = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private String categorize(String filename) {
        String ext = getExtension(filename).toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg", "png", "gif", "webp", "heic", "bmp", "raw" -> "Photos";
            case "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm" -> "Videos";
            case "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv" -> "Documents";
            case "mp3", "flac", "wav", "aac", "ogg", "m4a" -> "Audio";
            case "zip", "rar", "7z", "tar", "gz", "bz2" -> "Archives";
            default -> "Others";
        };
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot >= 0) ? filename.substring(dot + 1) : "";
    }
}
