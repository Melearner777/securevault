package com.jishan.securevault.controller;

import com.jishan.securevault.entity.FileMetadata;
import com.jishan.securevault.security.JwtUtil;
import com.jishan.securevault.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/files")
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private JwtUtil jwtUtil;

    // POST /files/upload
    // Header: Authorization: Bearer <token>
    // Body: multipart file
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String username = jwtUtil.extractUsername(token);
            String accessLevel = jwtUtil.extractAccessLevel(token);
            String result = fileService.uploadFile(file, username, accessLevel);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Upload failed: " + e.getMessage());
        }
    }

    // GET /files/download/{id}
    // Header: Authorization: Bearer <token>
    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String accessLevel = jwtUtil.extractAccessLevel(token);
            byte[] fileBytes = fileService.downloadFile(id, accessLevel);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"file_" + id + "\"")
                    .body(fileBytes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // GET /files/list
    // Header: Authorization: Bearer <token>
    // Returns list of files the user can access at their current level
    @GetMapping("/list")
    public ResponseEntity<List<FileMetadata>> listFiles(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.extractUsername(token);
        String accessLevel = jwtUtil.extractAccessLevel(token);
        List<FileMetadata> files = fileService.listFiles(username, accessLevel);
        return ResponseEntity.ok(files);
    }
}
