package com.vault.dto;

import com.vault.entity.ClaimStatus;
import java.io.Serializable;

public record ClaimUpdateMessage(Long claimId, ClaimStatus newStatus, String note) implements Serializable {
    private static final long serialVersionUID = 1L;
}
