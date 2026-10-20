package com.vault.dto;

public record PurchaseImportResult(Long executionId, String status, long readCount, long writeCount, long skipCount) { }
