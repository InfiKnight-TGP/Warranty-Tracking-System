package com.vault.messaging;

import com.vault.dto.ReminderMessage;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReminderProducer {
    private static final String QUEUE = "warranty.reminders";
    private final JmsTemplate jmsTemplate;

    public ReminderProducer(JmsTemplate jmsTemplate) { this.jmsTemplate = jmsTemplate; }

    public void send(ReminderMessage message) { jmsTemplate.convertAndSend(QUEUE, message); }
}
