package com.vault.repository;

import com.vault.entity.Claim;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.vault.entity.ClaimStatus;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
    List<Claim> findByRaisedById(Long userId);
    List<Claim> findByServiceCentreId(Long serviceCentreId);
    long countByStatusNotIn(List<ClaimStatus> statuses);
    long countByRaisedByIdAndStatusNotIn(Long userId, List<ClaimStatus> statuses);
    @Query(value = "SELECT status, COUNT(*) AS cnt FROM claims GROUP BY status", nativeQuery = true)
    List<Object[]> countClaimsPerStatusNative();
}
