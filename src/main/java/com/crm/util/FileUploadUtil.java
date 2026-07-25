package com.crm.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Slf4j
@Component
public class FileUploadUtil {

    @Value("${app.upload.dir:files/}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080/xformcrm}")
    private String baseUrl;

    @Value("${app.base-url-local:http://localhost:8080/xformcrm}")
    private String baseUrlLocal;

    @Value("${app.is-live:false}")
    private boolean isLive;

    /**
     * Get the upload directory path with null safety
     */
    public String getUploadDir() {
        if (uploadDir == null || uploadDir.trim().isEmpty()) {
            log.warn("Upload directory is null or empty, using default: files/");
            return "files/";
        }
        return uploadDir;
    }

    /**
     * Upload a file and return only the generated filename.
     */
    public String upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            log.warn("File is null or empty");
            return null;
        }

        log.info("Uploading file: {}, size: {} bytes",
                file.getOriginalFilename(), file.getSize());

        Path uploadPath = Paths.get(getUploadDir()).toAbsolutePath();

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Created upload directory: {}", uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String filename = UUID.randomUUID() + extension;

        Path filePath = uploadPath.resolve(filename);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("File uploaded successfully: {}", filePath);
        log.info("Filename saved to database: {}", filename);

        return filename;
    }

    /**
     * Delete a file by filename.
     */
    public void delete(String filename) {
        if (filename == null || filename.isBlank()) {
            log.warn("Filename is null or blank");
            return;
        }

        try {
            Path filePath = Paths.get(getUploadDir()).resolve(filename);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted file: {}", filePath);
            } else {
                log.warn("File not found: {}", filename);
            }

        } catch (IOException e) {
            log.error("Failed to delete file", e);
        }
    }

    /**
     * Get file path with null safety
     */
    public Path getFilePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            log.warn("Relative path is null or blank");
            return null;
        }

        try {
            String uploadDirPath = getUploadDir();
            if (uploadDirPath == null || uploadDirPath.isBlank()) {
                log.error("Upload directory is not configured");
                return null;
            }

            Path filePath = Paths.get(uploadDirPath).resolve(relativePath);

            if (Files.exists(filePath) && Files.isReadable(filePath)) {
                return filePath;
            }

            log.warn("File not found or not readable: {}", filePath);
            return null;

        } catch (Exception e) {
            log.error("Error getting file path: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if file exists.
     */
    public boolean fileExists(String filename) {
        if (filename == null || filename.isBlank()) {
            return false;
        }

        try {
            String uploadDirPath = getUploadDir();
            if (uploadDirPath == null || uploadDirPath.isBlank()) {
                return false;
            }

            Path filePath = Paths.get(uploadDirPath).resolve(filename);
            return Files.exists(filePath) && Files.isReadable(filePath);

        } catch (Exception e) {
            log.error("Error checking file existence: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Upload file to subdirectory and return full URL.
     */
/**
 * Upload file to subdirectory and return full URL.
 */
public String uploadFile(MultipartFile file, String subDirectory) throws IOException {
    if (file == null || file.isEmpty()) {
        return null;
    }

    String originalFileName = file.getOriginalFilename();
    String extension = "";

    // Get the file extension from the original filename
    if (originalFileName != null && originalFileName.contains(".")) {
        extension = originalFileName.substring(originalFileName.lastIndexOf("."));
    }

    // Generate unique filename WITH extension
    String uniqueFileName = UUID.randomUUID() + extension;

    String uploadDirPath = getUploadDir();
    if (uploadDirPath == null || uploadDirPath.isBlank()) {
        throw new IOException("Upload directory is not configured");
    }

    Path uploadPath = Paths.get(uploadDirPath, subDirectory);

    if (!Files.exists(uploadPath)) {
        Files.createDirectories(uploadPath);
        log.info("Created directory: {}", uploadPath);
    }

    Path filePath = uploadPath.resolve(uniqueFileName);
    Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

    String activeBaseUrl = isLive ? baseUrl : baseUrlLocal;
    String baseUrlToUse = (activeBaseUrl != null && !activeBaseUrl.isBlank()) 
            ? activeBaseUrl 
            : "http://localhost:8080/xformcrm";

    log.info("Environment : {}", isLive ? "LIVE" : "LOCAL");
    log.info("Base URL    : {}", baseUrlToUse);
    log.info("Uploaded to : {}", filePath);

    return baseUrlToUse + "/api/files/" + subDirectory + "/" + uniqueFileName;
}

    /**
     * Delete uploaded file using URL.
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            log.warn("File URL is null or blank");
            return;
        }

        try {
            String relativePath = fileUrl;

            if (relativePath.contains("/api/files/")) {
                relativePath = relativePath.substring(
                        relativePath.indexOf("/api/files/") + "/api/files/".length());
            }

            String uploadDirPath = getUploadDir();
            if (uploadDirPath == null || uploadDirPath.isBlank()) {
                log.error("Upload directory is not configured");
                return;
            }

            Path path = Paths.get(uploadDirPath).resolve(relativePath);

            if (Files.exists(path)) {
                Files.delete(path);
                log.info("Deleted file: {}", path);
            } else {
                log.warn("File not found: {}", path);
            }

        } catch (IOException e) {
            log.error("Error deleting file", e);
        }
    }

    /**
     * Current active base URL.
     */
    public String getActiveBaseUrl() {
        String base = isLive ? baseUrl : baseUrlLocal;
        return (base != null && !base.isBlank()) ? base : "http://localhost:8080/xformcrm";
    }

    /**
     * Whether application is running in live mode.
     */
    public boolean isLive() {
        return isLive;
    }
}