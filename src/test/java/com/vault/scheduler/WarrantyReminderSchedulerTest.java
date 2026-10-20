package com.vault.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

@ExtendWith(MockitoExtension.class)
class WarrantyReminderSchedulerTest {
    @Mock private JobLauncher jobLauncher;
    @Mock private Job warrantyReminderJob;

    @Test
    void launchesReminderJobWithFreshParameters() throws Exception {
        new WarrantyReminderScheduler(jobLauncher, warrantyReminderJob).sendExpiryReminders();
        verify(jobLauncher).run(same(warrantyReminderJob), any(JobParameters.class));
    }
}
