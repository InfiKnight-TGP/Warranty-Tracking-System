package com.vault.messaging;

import com.vault.dto.ClaimUpdateMessage;
import com.vault.entity.ClaimStatus;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class ClaimUpdateProducer {
    private static final String QUEUE = "claim.updates";
    private final JmsTemplate jmsTemplate;

    public ClaimUpdateProducer(JmsTemplate jmsTemplate) { this.jmsTemplate = jmsTemplate; }

    public void send(Long claimId, ClaimStatus newStatus, String note) {
        jmsTemplate.convertAndSend(QUEUE, new ClaimUpdateMessage(claimId, newStatus, note));
    }
}
