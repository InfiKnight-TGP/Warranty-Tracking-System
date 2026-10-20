package com.vault.scheduler;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WarrantyReminderScheduler {
    private final JobLauncher jobLauncher;
    private final Job warrantyReminderJob;

    public WarrantyReminderScheduler(JobLauncher jobLauncher, @Qualifier("warrantyReminderJob") Job warrantyReminderJob) {
        this.jobLauncher = jobLauncher;
        this.warrantyReminderJob = warrantyReminderJob;
    }

    @Scheduled(cron = "${vault.scheduler.cron}")
    public void sendExpiryReminders() {
        try {
            jobLauncher.run(warrantyReminderJob, new JobParametersBuilder()
                    .addLong("scheduledAt", System.currentTimeMillis()).toJobParameters());
        } catch (Exception ex) {
            throw new IllegalStateException("Could not launch the warranty reminder batch job", ex);
        }
    }
}
