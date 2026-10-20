package com.vault.controller;

import com.vault.dto.PurchaseImportResult;
import com.vault.dto.PurchaseRequest;
import com.vault.dto.PurchaseResponse;
import com.vault.entity.Purchase;
import com.vault.exception.FileStorageException;
import com.vault.exception.ResourceNotFoundException;
import com.vault.service.BatchJobService;
import com.vault.service.FileStorageService;
import com.vault.service.PurchaseService;
import com.vault.validation.OnCreate;
import com.vault.validation.OnUpdate;
import jakarta.validation.groups.Default;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {
    private final PurchaseService purchaseService;
    private final FileStorageService fileStorageService;
    private final BatchJobService batchJobService;

    public PurchaseController(PurchaseService purchaseService, FileStorageService fileStorageService,
                              BatchJobService batchJobService) {
        this.purchaseService = purchaseService;
        this.fileStorageService = fileStorageService;
        this.batchJobService = batchJobService;
    }

    @GetMapping
    public List<PurchaseResponse> findMine(Principal principal,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        return purchaseService.findMine(principal.getName(), keyword, category, status);
    }

    @GetMapping("/{id}")
    public PurchaseResponse getMine(Principal principal, @PathVariable Long id) {
        return purchaseService.getMine(principal.getName(), id);
    }

    @PostMapping
    public ResponseEntity<PurchaseResponse> create(Principal principal,
            @Validated({Default.class, OnCreate.class}) @RequestBody PurchaseRequest request) {
        return ResponseEntity.status(201).body(purchaseService.create(principal.getName(), request));
    }

    @PutMapping("/{id}")
    public PurchaseResponse update(Principal principal, @PathVariable Long id,
            @Validated({Default.class, OnUpdate.class}) @RequestBody PurchaseRequest request) {
        return purchaseService.update(principal.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Principal principal, @PathVariable Long id) {
        purchaseService.delete(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/bill", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PurchaseResponse uploadBill(Principal principal, @PathVariable Long id, @RequestParam("file") MultipartFile file) {
        purchaseService.ownedPurchase(principal.getName(), id);
        String storedPath = fileStorageService.store(file);
        try { return purchaseService.setBillPath(principal.getName(), id, storedPath); }
        catch (RuntimeException ex) {
            try { Files.deleteIfExists(Path.of(storedPath)); } catch (IOException ignored) { }
            throw ex;
        }
    }

    @GetMapping("/{id}/bill")
    public ResponseEntity<Resource> downloadBill(Principal principal, @PathVariable Long id) {
        Purchase purchase = purchaseService.ownedPurchase(principal.getName(), id);
        if (purchase.getBillFilePath() == null) throw new ResourceNotFoundException("No bill has been uploaded for this purchase");
        Path path = fileStorageService.resolve(purchase.getBillFilePath());
        Resource file = new FileSystemResource(path);
        if (!file.exists() || !file.isReadable()) throw new ResourceNotFoundException("Bill file was not found");
        MediaType mediaType;
        try { mediaType = MediaType.parseMediaType(Files.probeContentType(path)); }
        catch (IOException | IllegalArgumentException ex) { throw new FileStorageException("Could not read bill file type", ex); }
        return ResponseEntity.ok().contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName() + "\"")
                .body(file);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PurchaseImportResult importCsv(Principal principal, @RequestParam("file") MultipartFile file) {
        return batchJobService.runImportJob(principal.getName(), file);
    }
}
