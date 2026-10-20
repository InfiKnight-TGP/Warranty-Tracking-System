package com.vault.config;

import com.vault.dto.PurchaseRequest;
import com.vault.dto.ReminderMessage;
import com.vault.entity.NotificationType;
import com.vault.entity.Purchase;
import com.vault.entity.User;
import com.vault.exception.ResourceNotFoundException;
import com.vault.repository.PurchaseRepository;
import com.vault.repository.UserRepository;
import com.vault.service.PurchaseService;
import com.vault.validation.OnCreate;
import jakarta.persistence.EntityManagerFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.groups.Default;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchConfig {
    private static final Logger logger = LoggerFactory.getLogger(BatchConfig.class);
    private static final int CHUNK_SIZE = 10;

    public record ReminderOutcome(Purchase purchase, List<ReminderMessage> messages) { }

    @Bean
    public ExecutionContextPromotionListener candidatePromotionListener() {
        ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
        listener.setKeys(new String[]{"candidateCount"});
        return listener;
    }

    @Bean
    public Tasklet countReminderCandidatesTasklet(PurchaseRepository repository) {
        return (contribution, chunkContext) -> {
            LocalDate today = LocalDate.now();
            long count30 = repository.findByWarrantyExpiryBetweenAndReminder30SentFalse(today, today.plusDays(30)).size();
            long count7 = repository.findByWarrantyExpiryBetweenAndReminder7SentFalse(today, today.plusDays(7)).size();
            long total = count30 + count7;
            chunkContext.getStepContext().getStepExecution().getExecutionContext().putLong("candidateCount", total);
            logger.info("Found {} expiry reminder candidates ({} 30-day and {} 7-day)", total, count30, count7);
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step countReminderCandidatesStep(JobRepository jobs, PlatformTransactionManager transactions,
                                            Tasklet countReminderCandidatesTasklet,
                                            ExecutionContextPromotionListener candidatePromotionListener) {
        return new StepBuilder("countReminderCandidatesStep", jobs)
                .tasklet(countReminderCandidatesTasklet, transactions).listener(candidatePromotionListener).build();
    }

    @Bean
    @StepScope
    public JpaCursorItemReader<Purchase> reminderCandidateReader(EntityManagerFactory entityManagerFactory) {
        LocalDate today = LocalDate.now();
        // A cursor keeps one stable result set while the job flips the reminder flags used by this WHERE clause.
        return new JpaCursorItemReaderBuilder<Purchase>()
                .name("reminderCandidateReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT p FROM Purchase p JOIN FETCH p.owner WHERE "
                        + "(p.warrantyExpiry BETWEEN :today AND :limit30 AND p.reminder30Sent = false) "
                        + "OR (p.warrantyExpiry BETWEEN :today AND :limit7 AND p.reminder7Sent = false) ORDER BY p.id")
                .parameterValues(java.util.Map.of("today", today, "limit30", today.plusDays(30), "limit7", today.plusDays(7)))
                .build();
    }

    @Bean
    public ItemProcessor<Purchase, ReminderOutcome> reminderCandidateProcessor() {
        return purchase -> {
            LocalDate today = LocalDate.now();
            List<ReminderMessage> messages = new ArrayList<>();
            if (!purchase.isReminder30Sent() && inWindow(purchase, today, 30)) {
                messages.add(new ReminderMessage(purchase.getId(), purchase.getOwner().getId(), NotificationType.EXPIRY_30));
                purchase.setReminder30Sent(true);
            }
            if (!purchase.isReminder7Sent() && inWindow(purchase, today, 7)) {
                messages.add(new ReminderMessage(purchase.getId(), purchase.getOwner().getId(), NotificationType.EXPIRY_7));
                purchase.setReminder7Sent(true);
            }
            return messages.isEmpty() ? null : new ReminderOutcome(purchase, messages);
        };
    }

    private boolean inWindow(Purchase purchase, LocalDate today, int days) {
        return !purchase.getWarrantyExpiry().isBefore(today)
                && !purchase.getWarrantyExpiry().isAfter(today.plusDays(days));
    }

    @Bean
    public JpaItemWriter<Purchase> batchPurchaseWriter(EntityManagerFactory entityManagerFactory) {
        JpaItemWriter<Purchase> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }

    @Bean
    public ItemWriter<ReminderOutcome> reminderWriter(JmsTemplate jmsTemplate, JpaItemWriter<Purchase> batchPurchaseWriter) {
        return chunk -> {
            List<Purchase> changedPurchases = new ArrayList<>();
            for (ReminderOutcome outcome : chunk) {
                for (ReminderMessage message : outcome.messages()) {
                    // Use Boot's configured JmsTemplate so the listener receives its normal Serializable object message.
                    jmsTemplate.convertAndSend("warranty.reminders", message);
                }
                changedPurchases.add(outcome.purchase());
            }
            if (!changedPurchases.isEmpty()) batchPurchaseWriter.write(new org.springframework.batch.item.Chunk<>(changedPurchases));
        };
    }

    @Bean
    public Step processRemindersStep(JobRepository jobs, PlatformTransactionManager transactions,
                                     JpaCursorItemReader<Purchase> reminderCandidateReader,
                                     ItemProcessor<Purchase, ReminderOutcome> reminderCandidateProcessor,
                                     ItemWriter<ReminderOutcome> reminderWriter) {
        return new StepBuilder("processRemindersStep", jobs)
                .<Purchase, ReminderOutcome>chunk(CHUNK_SIZE, transactions)
                .reader(reminderCandidateReader).processor(reminderCandidateProcessor).writer(reminderWriter).build();
    }

    @Bean
    public Tasklet logReminderSummaryTasklet() {
        return (contribution, chunkContext) -> {
            long count = chunkContext.getStepContext().getStepExecution().getJobExecution()
                    .getExecutionContext().getLong("candidateCount", 0L);
            logger.info("Warranty reminder batch finished; candidate reminders={}", count);
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step logReminderSummaryStep(JobRepository jobs, PlatformTransactionManager transactions,
                                      Tasklet logReminderSummaryTasklet) {
        return new StepBuilder("logReminderSummaryStep", jobs).tasklet(logReminderSummaryTasklet, transactions).build();
    }

    @Bean
    public Job warrantyReminderJob(JobRepository jobs, Step countReminderCandidatesStep,
                                   Step processRemindersStep, Step logReminderSummaryStep) {
        return new JobBuilder("warrantyReminderJob", jobs).start(countReminderCandidatesStep)
                .next(processRemindersStep).next(logReminderSummaryStep).build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Purchase> purchaseExportReader(EntityManagerFactory entityManagerFactory) {
        return new JpaPagingItemReaderBuilder<Purchase>().name("purchaseExportReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT p FROM Purchase p JOIN FETCH p.owner ORDER BY p.id")
                .pageSize(50).build();
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<Purchase> purchaseExportWriter(@Value("#{jobParameters['outputPath']}") String outputPath) {
        Path output = Path.of(outputPath).toAbsolutePath().normalize();
        try { Files.createDirectories(output.getParent()); }
        catch (Exception ex) { throw new IllegalStateException("Could not create export directory", ex); }
        return new FlatFileItemWriterBuilder<Purchase>().name("purchaseExportWriter")
                .resource(new FileSystemResource(output))
                .headerCallback(writer -> writer.write("id,productName,brand,category,serialNumber,storeName,purchaseDate,price,warrantyMonths,warrantyExpiry,status,ownerEmail"))
                .lineAggregator(p -> String.join(",", csv(p.getId()), csv(p.getProductName()), csv(p.getBrand()), csv(p.getCategory()),
                        csv(p.getSerialNumber()), csv(p.getStoreName()), csv(p.getPurchaseDate()), csv(p.getPrice()),
                        csv(p.getWarrantyMonths()), csv(p.getWarrantyExpiry()), csv(PurchaseService.status(p)),
                        csv(p.getOwner() == null ? null : p.getOwner().getEmail())))
                .build();
    }

    @Bean
    public Step purchaseExportStep(JobRepository jobs, PlatformTransactionManager transactions,
                                   JpaPagingItemReader<Purchase> purchaseExportReader,
                                   FlatFileItemWriter<Purchase> purchaseExportWriter) {
        return new StepBuilder("purchaseExportStep", jobs).<Purchase, Purchase>chunk(50, transactions)
                .reader(purchaseExportReader).writer(purchaseExportWriter).build();
    }

    @Bean
    public Job purchaseExportJob(JobRepository jobs, Step purchaseExportStep) {
        return new JobBuilder("purchaseExportJob", jobs).start(purchaseExportStep).build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<PurchaseRequest> purchaseImportReader(@Value("#{jobParameters['inputFile']}") String inputFile) {
        return new FlatFileItemReaderBuilder<PurchaseRequest>().name("purchaseImportReader")
                .resource(new FileSystemResource(inputFile)).linesToSkip(1).delimited()
                .names("productName", "brand", "category", "serialNumber", "storeName", "purchaseDate", "price", "warrantyMonths", "supportContact")
                .fieldSetMapper(fields -> new PurchaseRequest(
                        clean(fields.readString("productName")), clean(fields.readString("brand")),
                        clean(fields.readString("category")), clean(fields.readString("serialNumber")),
                        clean(fields.readString("storeName")), LocalDate.parse(fields.readString("purchaseDate").trim()),
                        new BigDecimal(fields.readString("price").trim()), Integer.parseInt(fields.readString("warrantyMonths").trim()),
                        clean(fields.readString("supportContact"))))
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<PurchaseRequest, Purchase> purchaseImportProcessor(Validator validator, UserRepository userRepository,
            @Value("#{jobParameters['userEmail']}") String userEmail) {
        return item -> {
            Set<ConstraintViolation<PurchaseRequest>> errors = validator.validate(item, Default.class, OnCreate.class);
            if (!errors.isEmpty()) throw new ConstraintViolationException("Invalid purchase row", errors);
            User owner = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("User " + userEmail + " was not found"));
            return new Purchase(item.productName(), item.brand(), item.category(), item.serialNumber(), item.storeName(),
                    item.purchaseDate(), item.price(), item.warrantyMonths(), item.supportContact(), owner);
        };
    }

    @Bean
    public SkipListener<PurchaseRequest, Purchase> purchaseImportSkipListener() {
        return new SkipListener<>() {
            @Override public void onSkipInRead(Throwable error) { logger.warn("Skipped malformed CSV row: {}", error.getMessage()); }
            @Override public void onSkipInProcess(PurchaseRequest row, Throwable error) {
                logger.warn("Skipped invalid CSV row for {}: {}", row.productName(), error.getMessage());
            }
            @Override public void onSkipInWrite(Purchase row, Throwable error) {
                logger.warn("Skipped database write for {}: {}", row.getProductName(), error.getMessage());
            }
        };
    }

    @Bean
    public Step purchaseImportStep(JobRepository jobs, PlatformTransactionManager transactions,
                                   FlatFileItemReader<PurchaseRequest> purchaseImportReader,
                                   ItemProcessor<PurchaseRequest, Purchase> purchaseImportProcessor,
                                   JpaItemWriter<Purchase> batchPurchaseWriter,
                                   SkipListener<PurchaseRequest, Purchase> purchaseImportSkipListener) {
        return new StepBuilder("purchaseImportStep", jobs).<PurchaseRequest, Purchase>chunk(CHUNK_SIZE, transactions)
                .reader(purchaseImportReader).processor(purchaseImportProcessor).writer(batchPurchaseWriter)
                .faultTolerant().skipLimit(10).skip(ConstraintViolationException.class)
                .skip(FlatFileParseException.class).skip(DateTimeParseException.class).skip(IllegalArgumentException.class)
                .listener(purchaseImportSkipListener).build();
    }

    @Bean
    public Job purchaseImportJob(JobRepository jobs, Step purchaseImportStep) {
        return new JobBuilder("purchaseImportJob", jobs).start(purchaseImportStep).build();
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private String csv(Object value) {
        if (value == null) return "";
        String text = String.valueOf(value).replace("\"", "\"\"");
        return text.contains(",") || text.contains("\"") || text.contains("\n")
                ? "\"" + text + "\"" : text;
    }
}
