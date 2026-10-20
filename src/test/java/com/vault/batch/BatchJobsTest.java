package com.vault.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vault.dto.PurchaseImportResult;
import com.vault.repository.PurchaseRepository;
import com.vault.service.BatchJobService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:warranty-vault-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "vault.scheduler.cron=0 0 0 1 1 *"
})
class BatchJobsTest {
    @Autowired private BatchJobService batchJobService;
    @Autowired private PurchaseRepository purchaseRepository;

    @Test
    void reminderBatchMarksSeededExpiringPurchasesAndCsvImportReportsSkippedRows() {
        var reminderExecution = batchJobService.runReminderJobForAdmin("admin@vault.com");
        assertEquals("COMPLETED", reminderExecution.status());
        assertTrue(purchaseRepository.findFirstBySerialNumberIgnoreCase("LG-3003").orElseThrow().isReminder30Sent());
        assertTrue(purchaseRepository.findFirstBySerialNumberIgnoreCase("SM-4004").orElseThrow().isReminder30Sent());
        assertTrue(purchaseRepository.findFirstBySerialNumberIgnoreCase("SM-4004").orElseThrow().isReminder7Sent());

        String today = LocalDate.now().toString();
        String csv = "productName,brand,category,serialNumber,storeName,purchaseDate,price,warrantyMonths,supportContact\n"
                + "Batch Valid,Acme,Test,BATCH-OK,Demo Store," + today + ",15.00,12,Helpdesk\n"
                + "Batch Invalid,Acme,Test,BATCH-BAD,Demo Store," + today + ",15.00,0,Helpdesk\n";
        MockMultipartFile file = new MockMultipartFile("file", "purchases.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        PurchaseImportResult result = batchJobService.runImportJob("user@vault.com", file);

        assertEquals("COMPLETED", result.status());
        assertEquals(1, result.writeCount());
        assertEquals(1, result.skipCount());
        assertTrue(purchaseRepository.findFirstBySerialNumberIgnoreCase("BATCH-OK").isPresent());
    }
}
