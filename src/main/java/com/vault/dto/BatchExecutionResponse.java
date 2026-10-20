package com.vault.dto;

import java.time.LocalDateTime;

public record BatchExecutionResponse(Long executionId, String jobName, String status, String exitCode,
                                     long readCount, long writeCount, long skipCount,
                                     LocalDateTime startTime, LocalDateTime endTime) { }
