package com.jishan.securevault.service;

import com.jishan.securevault.entity.FileMetadata;
import com.jishan.securevault.repository.FileMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FileService {

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private EncryptionService encryptionService;

    // Storage paths
    private static final String NORMAL_PATH  = "src/main/resources/storage/normal/";
    private static final String SECURE_PATH  = "src/main/resources/storage/secure/";

    // Upload file based on access level
    public String uploadFile(MultipartFile file, String username, String accessLevel) throws Exception {

        String targetDir = accessLevel.equals("LEVEL2") ? SECURE_PATH : NORMAL_PATH;
        boolean isSecure  = accessLevel.equals("LEVEL2");

        // Create directories if not exist
        new File(targetDir).mkdirs();

        byte[] fileBytes = file.getBytes();

        // Encrypt if secure level
        if (isSecure) {
            fileBytes = encryptionService.encrypt(fileBytes);
        }

        // Save file to disk
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(targetDir + fileName);
        Files.write(filePath, fileBytes);

        // Save metadata to database
        FileMetadata metadata = new FileMetadata();
        metadata.setFileName(fileName);
        metadata.setOriginalName(file.getOriginalFilename());
        metadata.setOwnerUsername(username);
        metadata.setAccessLevel(accessLevel);
        metadata.setEncrypted(isSecure);
        metadata.setUploadTime(LocalDateTime.now());
        fileMetadataRepository.save(metadata);

        return "File uploaded successfully: " + fileName;
    }

    // Download file by ID
    public byte[] downloadFile(Long fileId, String accessLevel) throws Exception {

        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        // Block access if level is insufficient
        if (metadata.getAccessLevel().equals("LEVEL2") && !accessLevel.equals("LEVEL2")) {
            throw new RuntimeException("Access denied: insufficient security level");
        }

        String targetDir = metadata.getAccessLevel().equals("LEVEL2") ? SECURE_PATH : NORMAL_PATH;
        Path filePath = Paths.get(targetDir + metadata.getFileName());
        byte[] fileBytes = Files.readAllBytes(filePath);

        // Decrypt if file is encrypted
        if (metadata.isEncrypted()) {
            fileBytes = encryptionService.decrypt(fileBytes);
        }

        return fileBytes;
    }

    // List files by access level
    public List<FileMetadata> listFiles(String username, String accessLevel) {
        return fileMetadataRepository.findByOwnerUsernameAndAccessLevel(username, accessLevel);
    }
}
