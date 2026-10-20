package com.vault.service;

import com.vault.exception.FileStorageException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {
    private final Path uploadDirectory;

    public FileStorageService(@Value("${vault.upload-dir}") String uploadDirectory) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new FileStorageException("An invoice file is required");
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = extension(originalName);
        String contentType = file.getContentType();
        boolean validType = (".pdf".equals(extension) && "application/pdf".equalsIgnoreCase(contentType))
                || ((".jpg".equals(extension) || ".jpeg".equals(extension)) && "image/jpeg".equalsIgnoreCase(contentType))
                || (".png".equals(extension) && "image/png".equalsIgnoreCase(contentType));
        if (!validType) throw new FileStorageException("Only PDF, JPG, and PNG files are allowed");
        if (file.getSize() > 5L * 1024 * 1024) throw new FileStorageException("Invoice file must be 5 MB or smaller");

        String storedName = UUID.randomUUID() + extension;
        Path target = uploadDirectory.resolve(storedName).normalize();
        if (!target.startsWith(uploadDirectory)) throw new FileStorageException("Invalid invoice file name");
        try {
            Files.createDirectories(uploadDirectory);
            file.transferTo(target);
            return target.toString();
        } catch (IOException ex) {
            throw new FileStorageException("Could not save invoice file", ex);
        }
    }

    public Path resolve(String storedPath) {
        Path file = Path.of(storedPath).toAbsolutePath().normalize();
        if (!file.startsWith(uploadDirectory)) throw new FileStorageException("Invoice file path is invalid");
        return file;
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot).toLowerCase(Locale.ROOT);
    }
}
