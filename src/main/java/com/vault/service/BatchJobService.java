package com.vault.service;

import com.vault.dto.BatchExecutionResponse;
import com.vault.dto.PurchaseImportResult;
import com.vault.exception.FileStorageException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BatchJobService {
    private final JobLauncher jobLauncher;
    private final JobExplorer jobExplorer;
    private final Job warrantyReminderJob;
    private final Job purchaseExportJob;
    private final Job purchaseImportJob;
    private final AdminService adminService;
    private final Path exportDirectory;

    public BatchJobService(JobLauncher jobLauncher, JobExplorer jobExplorer,
                           @Qualifier("warrantyReminderJob") Job warrantyReminderJob,
                           @Qualifier("purchaseExportJob") Job purchaseExportJob,
                           @Qualifier("purchaseImportJob") Job purchaseImportJob,
                           AdminService adminService,
                           @Value("${vault.export-dir:./exports}") String exportDirectory) {
        this.jobLauncher = jobLauncher;
        this.jobExplorer = jobExplorer;
        this.warrantyReminderJob = warrantyReminderJob;
        this.purchaseExportJob = purchaseExportJob;
        this.purchaseImportJob = purchaseImportJob;
        this.adminService = adminService;
        this.exportDirectory = Path.of(exportDirectory).toAbsolutePath().normalize();
    }

    public BatchExecutionResponse runReminderJobForAdmin(String email) {
        adminService.requireAdmin(email);
        return run(warrantyReminderJob, new JobParametersBuilder().addString("nonce", UUID.randomUUID().toString()).toJobParameters());
    }

    public Path runExportJobForAdmin(String email) {
        adminService.requireAdmin(email);
        try {
            Files.createDirectories(exportDirectory);
            Path output = exportDirectory.resolve("purchases-" + System.currentTimeMillis() + ".csv").normalize();
            if (!output.startsWith(exportDirectory)) throw new IllegalArgumentException("Invalid export location");
            run(purchaseExportJob, new JobParametersBuilder().addString("nonce", UUID.randomUUID().toString())
                    .addString("outputPath", output.toString()).toJobParameters());
            return output;
        } catch (Exception ex) {
            throw new FileStorageException("Could not export purchases to CSV", ex);
        }
    }

    @CacheEvict(value = "dashboardStats", allEntries = true)
    public PurchaseImportResult runImportJob(String userEmail, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("A CSV file is required for import");
        Path input = null;
        try {
            Files.createDirectories(exportDirectory);
            input = Files.createTempFile(exportDirectory, "purchases-import-", ".csv");
            file.transferTo(input);
            JobExecution execution = runExecution(purchaseImportJob, new JobParametersBuilder()
                    .addString("nonce", UUID.randomUUID().toString())
                    .addString("userEmail", userEmail)
                    .addString("inputFile", input.toAbsolutePath().toString()).toJobParameters());
            BatchExecutionResponse response = toResponse(execution);
            return new PurchaseImportResult(response.executionId(), response.status(), response.readCount(),
                    response.writeCount(), response.skipCount());
        } catch (Exception ex) {
            throw new FileStorageException("Could not import purchases CSV", ex);
        } finally {
            if (input != null) {
                try { Files.deleteIfExists(input); } catch (IOException ignored) { }
            }
        }
    }

    public List<BatchExecutionResponse> listExecutionsForAdmin(String email) {
        adminService.requireAdmin(email);
        List<BatchExecutionResponse> results = new ArrayList<>();
        for (String jobName : jobExplorer.getJobNames()) {
            for (JobInstance instance : jobExplorer.getJobInstances(jobName, 0, 20)) {
                for (JobExecution execution : jobExplorer.getJobExecutions(instance)) results.add(toResponse(execution));
            }
        }
        results.sort(Comparator.comparing(BatchExecutionResponse::executionId).reversed());
        return results;
    }

    private BatchExecutionResponse run(Job job, JobParameters parameters) {
        try { return toResponse(runExecution(job, parameters)); }
        catch (Exception ex) { throw new IllegalStateException("Could not run batch job " + job.getName(), ex); }
    }

    private JobExecution runExecution(Job job, JobParameters parameters) throws Exception {
        return jobLauncher.run(job, parameters);
    }

    public BatchExecutionResponse toResponse(JobExecution execution) {
        long read = 0, written = 0, skipped = 0;
        for (StepExecution step : execution.getStepExecutions()) {
            read += step.getReadCount();
            written += step.getWriteCount();
            skipped += step.getSkipCount();
        }
        return new BatchExecutionResponse(execution.getId(), execution.getJobInstance().getJobName(),
                execution.getStatus().name(), execution.getExitStatus().getExitCode(), read, written, skipped,
                execution.getStartTime(), execution.getEndTime());
    }
}
