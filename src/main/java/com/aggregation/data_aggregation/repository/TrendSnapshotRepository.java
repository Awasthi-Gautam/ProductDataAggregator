package com.aggregation.data_aggregation.repository;

import com.aggregation.data_aggregation.model.entity.TrendSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TrendSnapshotRepository extends JpaRepository<TrendSnapshot, String> {

    /**
     * Returns the most recent snapshot for a product recorded BEFORE the given time.
     * Used to find the previous cycle's score: call with before = now - 30 minutes
     * so we don't accidentally pick up the snapshot just written in this cycle.
     */
    Optional<TrendSnapshot> findTopByProductIdAndRecordedAtBeforeOrderByRecordedAtDesc(
        String productId, LocalDateTime before);

    /** Prunes old snapshots — called at end of every scoring cycle with a 48h cutoff. */
    @Modifying
    @Transactional
    @Query("DELETE FROM TrendSnapshot t WHERE t.recordedAt < :cutoff")
    void deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
