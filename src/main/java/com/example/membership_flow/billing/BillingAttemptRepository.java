package com.example.membership_flow.billing;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BillingAttemptRepository extends MongoRepository<BillingAttemptRecord, String> {

    Optional<BillingAttemptRecord> findByAttemptId(String attemptId);

    boolean existsByContractIdAndScheduledDateAndStatusIn(
            String contractId, LocalDate scheduledDate, Collection<BillingAttemptRecord.Status> statuses);

    // Dunning: FAILED records not yet at max attempts and past their retry time
    List<BillingAttemptRecord> findByStatusAndAttemptCountLessThanAndNextRetryAtBefore(
            BillingAttemptRecord.Status status, int maxAttempts, Instant now);

    List<BillingAttemptRecord> findByContractIdOrderByCreatedAtDesc(String contractId);

    List<BillingAttemptRecord> findAllByOrderByCreatedAtDesc();

    List<BillingAttemptRecord> findByStatusOrderByCreatedAtDesc(BillingAttemptRecord.Status status);

    List<BillingAttemptRecord> findByContractIdAndStatusOrderByCreatedAtDesc(
            String contractId, BillingAttemptRecord.Status status);
}
