package com.vault.repository;

import com.vault.entity.ClaimUpdate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimUpdateRepository extends JpaRepository<ClaimUpdate, Long> {
    List<ClaimUpdate> findByClaimIdOrderByUpdatedAtAsc(Long claimId);
}
