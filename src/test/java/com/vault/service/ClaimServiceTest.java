package com.vault.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vault.entity.ClaimStatus;
import org.junit.jupiter.api.Test;

class ClaimServiceTest {
    @Test
    void acceptsOnlyTheRequiredForwardTransitionsAndRejectionRules() {
        assertTrue(ClaimService.canTransition(ClaimStatus.RAISED, ClaimStatus.RECEIVED));
        assertTrue(ClaimService.canTransition(ClaimStatus.RECEIVED, ClaimStatus.IN_REPAIR));
        assertTrue(ClaimService.canTransition(ClaimStatus.IN_REPAIR, ClaimStatus.REPAIRED));
        assertTrue(ClaimService.canTransition(ClaimStatus.REPAIRED, ClaimStatus.RETURNED));
        assertTrue(ClaimService.canTransition(ClaimStatus.RAISED, ClaimStatus.REJECTED));
        assertTrue(ClaimService.canTransition(ClaimStatus.RECEIVED, ClaimStatus.REJECTED));
    }

    @Test
    void rejectsBackwardSkippedAndTerminalTransitions() {
        for (ClaimStatus oldStatus : ClaimStatus.values()) {
            for (ClaimStatus newStatus : ClaimStatus.values()) {
                boolean allowed = (oldStatus == ClaimStatus.RAISED && (newStatus == ClaimStatus.RECEIVED || newStatus == ClaimStatus.REJECTED))
                        || (oldStatus == ClaimStatus.RECEIVED && (newStatus == ClaimStatus.IN_REPAIR || newStatus == ClaimStatus.REJECTED))
                        || (oldStatus == ClaimStatus.IN_REPAIR && newStatus == ClaimStatus.REPAIRED)
                        || (oldStatus == ClaimStatus.REPAIRED && newStatus == ClaimStatus.RETURNED);
                if (!allowed) assertFalse(ClaimService.canTransition(oldStatus, newStatus), oldStatus + " -> " + newStatus);
            }
        }
        assertFalse(ClaimService.canTransition(null, ClaimStatus.RAISED));
        assertFalse(ClaimService.canTransition(ClaimStatus.RAISED, null));
    }
}
