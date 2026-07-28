package com.crm.controller;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.crm.util.FileUploadUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
public class FileController {

    private final FileUploadUtil fileUploadUtil;

    @GetMapping("/api-file/debug")
    public ResponseEntity<?> debug() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "FileController is working");
        response.put("uploadDir", fileUploadUtil.getUploadDir());
        response.put("isLive", fileUploadUtil.isLive());
        response.put("baseUrl", fileUploadUtil.getActiveBaseUrl());
        
        String uploadDir = fileUploadUtil.getUploadDir();
        Path uploadPath = Paths.get(uploadDir);
        response.put("uploadPathExists", Files.exists(uploadPath));
        response.put("uploadPath", uploadPath.toAbsolutePath().toString());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Debug endpoint to list files in a specific directory
     * GET /xformcrm/api-file/debug/list/quotation/UWS_RRW_26-27_210_R2
     */
    @GetMapping("/api/debug/list/**")
    public ResponseEntity<?> debugListFiles(org.springframework.web.context.request.WebRequest request) {
        try {
            String path = request.getDescription(false);
            String fullPath = extractPath(path);
            
            log.info("========================================");
            log.info("DEBUG LIST FILES:");
            log.info("Full path: {}", fullPath);
            
            String uploadDir = fileUploadUtil.getUploadDir();
            if (uploadDir == null || uploadDir.isBlank()) {
                return ResponseEntity.status(500).body("Upload directory not configured");
            }
            
            // Split the path
            String[] parts = fullPath.split("/");
            if (parts.length < 1) {
                return ResponseEntity.badRequest().body("Invalid path");
            }
            
            // Build directory path
            Path dirPath = Paths.get(uploadDir, parts);
            log.info("Directory path: {}", dirPath.toAbsolutePath());
            
            Map<String, Object> response = new HashMap<>();
            response.put("directory", dirPath.toAbsolutePath().toString());
            response.put("exists", Files.exists(dirPath));
            
            if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
                List<Map<String, String>> files = new ArrayList<>();
                try (Stream<Path> paths = Files.list(dirPath)) {
                    paths.forEach(filePath -> {
                        Map<String, String> fileInfo = new HashMap<>();
                        fileInfo.put("name", filePath.getFileName().toString());
                        fileInfo.put("isDirectory", String.valueOf(Files.isDirectory(filePath)));
                        try {
                            fileInfo.put("size", String.valueOf(Files.size(filePath)));
                        } catch (IOException e) {
                            fileInfo.put("size", "unknown");
                        }
                        files.add(fileInfo);
                    });
                }
                response.put("files", files);
                response.put("totalFiles", files.size());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in debug list: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/api-file/list/{subDirectory}/{subSubDirectory}")
    public ResponseEntity<?> listFiles(
            @PathVariable String subDirectory,
            @PathVariable String subSubDirectory) {
        try {
            String uploadDir = fileUploadUtil.getUploadDir();
            if (uploadDir == null || uploadDir.isBlank()) {
                return ResponseEntity.status(500).body("Upload directory not configured");
            }
            
            Path dirPath = Paths.get(uploadDir, subDirectory, subSubDirectory);
            
            if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
                return ResponseEntity.notFound().build();
            }
            
            List<Map<String, String>> fileDetails = new ArrayList<>();
            Files.list(dirPath).forEach(path -> {
                Map<String, String> details = new HashMap<>();
                String fileName = path.getFileName().toString();
                details.put("name", fileName);
                details.put("hasExtension", String.valueOf(fileName.contains(".")));
                try {
                    String contentType = Files.probeContentType(path);
                    details.put("type", contentType != null ? contentType : "unknown");
                    details.put("size", String.valueOf(Files.size(path)));
                } catch (IOException e) {
                    details.put("error", e.getMessage());
                }
                fileDetails.add(details);
            });
            
            Map<String, Object> response = new HashMap<>();
            response.put("directory", dirPath.toAbsolutePath().toString());
            response.put("files", fileDetails);
            response.put("total", fileDetails.size());
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            log.error("Error listing files: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

  @GetMapping("/api/view/**")
public ResponseEntity<Resource> viewFile(org.springframework.web.context.request.WebRequest request) {
    try {
        String path = request.getDescription(false);
        log.info("========================================");
        log.info("VIEWING FILE:");
        log.info("Request description: {}", path);
        
        // Extract the path after /api/view/
        String fullPath = extractPathAfterApiView(path);
        log.info("Extracted full path: {}", fullPath);
        
        if (fullPath == null || fullPath.isEmpty()) {
            log.error("Could not extract file path");
            return ResponseEntity.badRequest().build();
        }
        
        // Handle "files/" prefix if present in the path
        if (fullPath.startsWith("files/")) {
            fullPath = fullPath.substring(6); // Remove "files/"
            log.info("Removed 'files/' prefix, new path: {}", fullPath);
        }
        
        // Split the path
        String[] parts = fullPath.split("/");
        if (parts.length < 2) {
            log.error("Invalid path format");
            return ResponseEntity.badRequest().build();
        }
        
        // First part is "quotation"
        String subDirectory = parts[0];
        
        // Build the directory path (everything except the last part which is filename)
        StringBuilder dirPathBuilder = new StringBuilder();
        for (int i = 1; i < parts.length - 1; i++) {
            if (i > 1) dirPathBuilder.append("/");
            dirPathBuilder.append(parts[i]);
        }
        String directoryPath = dirPathBuilder.toString();
        String fileName = parts[parts.length - 1];
        
        // Decode the filename
        String decodedFileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8.toString());
        String decodedDirectoryPath = URLDecoder.decode(directoryPath, StandardCharsets.UTF_8.toString());

        log.info("SubDirectory: {}", subDirectory);
        log.info("Directory Path: {}", decodedDirectoryPath);
        log.info("FileName: {}", decodedFileName);
        log.info("========================================");

        String uploadDir = fileUploadUtil.getUploadDir();
        if (uploadDir == null || uploadDir.isBlank()) {
            log.error("Upload directory is not configured");
            return ResponseEntity.status(500).build();
        }

        // Convert slashes to underscores for filesystem path
        String fileSystemPath = decodedDirectoryPath.replace("/", "_");
        
        // Build the primary path
        Path primaryPath = Paths.get(uploadDir, subDirectory, fileSystemPath, decodedFileName);
        log.info("Primary file path: {}", primaryPath.toAbsolutePath());
        
        // Check if file exists at primary path
        Path filePath = null;
        if (Files.exists(primaryPath) && !Files.isDirectory(primaryPath)) {
            filePath = primaryPath;
            log.info("✅ Found file at primary path");
        } else {
            // Try alternative paths
            List<Path> possiblePaths = new ArrayList<>();
            
            // Try with the exact path (if there are subdirectories with slashes)
            possiblePaths.add(Paths.get(uploadDir, subDirectory, decodedDirectoryPath, decodedFileName));
            
            // Try without quotation folder
            possiblePaths.add(Paths.get(uploadDir, fileSystemPath, decodedFileName));
            possiblePaths.add(Paths.get(uploadDir, decodedDirectoryPath, decodedFileName));
            
            // Try with the original path (before underscore conversion)
            possiblePaths.add(Paths.get(uploadDir, subDirectory, directoryPath, fileName));
            
            // Try with "files" prefix (legacy)
            possiblePaths.add(Paths.get(uploadDir, "files", subDirectory, fileSystemPath, decodedFileName));
            
            for (Path testPath : possiblePaths) {
                log.info("Checking alternative path: {}", testPath.toAbsolutePath());
                if (Files.exists(testPath) && !Files.isDirectory(testPath)) {
                    filePath = testPath;
                    log.info("✅ Found file at: {}", testPath.toAbsolutePath());
                    break;
                }
            }
        }
        
        if (filePath == null) {
            log.error("❌ File not found for path: {}", fullPath);
            
            // Log the directory contents to help debug
            Path dirPath = Paths.get(uploadDir, subDirectory, fileSystemPath);
            log.info("Checking directory: {}", dirPath.toAbsolutePath());
            if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
                try (Stream<Path> paths = Files.list(dirPath)) {
                    log.info("Files in directory:");
                    paths.forEach(p -> log.info("  - {}", p.getFileName().toString()));
                } catch (IOException e) {
                    log.error("Error listing directory: {}", e.getMessage());
                }
            } else {
                log.error("Directory does not exist: {}", dirPath.toAbsolutePath());
            }
            
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            log.error("❌ Resource not readable");
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        log.info("✅ Serving file: {}", decodedFileName);
        log.info("Content-Type: {}", contentType);
        log.info("File Size: {} bytes", Files.size(filePath));
        log.info("========================================");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + decodedFileName + "\"")
                .body(resource);

    } catch (Exception e) {
        log.error("Error serving file: {}", e.getMessage(), e);
        return ResponseEntity.internalServerError().build();
    }
}

    /**
     * Helper method to extract path after /api/view/
     */
    private String extractPathAfterApiView(String requestPath) {
        String path = requestPath;
        if (path.contains("uri=")) {
            path = path.substring(path.indexOf("uri=") + 4);
        }
        
        // Remove context path if present
        if (path.startsWith("/xformcrm")) {
            path = path.substring(9);
        }
        
        String pattern = "/api/view/";
        int index = path.indexOf(pattern);
        if (index != -1) {
            String extractedPath = path.substring(index + pattern.length());
            if (extractedPath.contains("?")) {
                extractedPath = extractedPath.substring(0, extractedPath.indexOf("?"));
            }
            return extractedPath;
        }
        
        pattern = "api/view/";
        index = path.indexOf(pattern);
        if (index != -1) {
            String extractedPath = path.substring(index + pattern.length());
            if (extractedPath.contains("?")) {
                extractedPath = extractedPath.substring(0, extractedPath.indexOf("?"));
            }
            return extractedPath;
        }
        
        return null;
    }

    private String extractPath(String requestPath) {
        String path = requestPath;
        if (path.contains("uri=")) {
            path = path.substring(path.indexOf("uri=") + 4);
        }
        if (path.startsWith("/xformcrm")) {
            path = path.substring(9);
        }
        if (path.contains("/api-file/debug/list/")) {
            path = path.substring(path.indexOf("/api-file/debug/list/") + 22);
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    @GetMapping("/api/download/**")
    public ResponseEntity<Resource> downloadFile(org.springframework.web.context.request.WebRequest request) {
        try {
            String path = request.getDescription(false);
            log.info("========================================");
            log.info("DOWNLOADING FILE:");
            log.info("Request description: {}", path);
            
            String fullPath = extractPathAfterApiView(path.replace("/download/", "/view/"));
            log.info("Extracted full path: {}", fullPath);
            
            if (fullPath == null || fullPath.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            String[] parts = fullPath.split("/");
            if (parts.length < 2) {
                return ResponseEntity.badRequest().build();
            }
            
            String subDirectory = parts[0];
            StringBuilder dirPathBuilder = new StringBuilder();
            for (int i = 1; i < parts.length - 1; i++) {
                if (i > 1) dirPathBuilder.append("/");
                dirPathBuilder.append(parts[i]);
            }
            String directoryPath = dirPathBuilder.toString();
            String fileName = parts[parts.length - 1];
            
            String decodedFileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8.toString());
            String decodedDirectoryPath = URLDecoder.decode(directoryPath, StandardCharsets.UTF_8.toString());
            
            String uploadDir = fileUploadUtil.getUploadDir();
            if (uploadDir == null || uploadDir.isBlank()) {
                return ResponseEntity.status(500).build();
            }
            
            String fileSystemPath = decodedDirectoryPath.replace("/", "_");
            Path filePath = Paths.get(uploadDir, subDirectory, fileSystemPath, decodedFileName);
            
            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            
            log.info("✅ Downloading file: {}", decodedFileName);
            log.info("========================================");
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + decodedFileName + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Error downloading file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}