package com.example.membership_flow.billing;

import com.example.membership_flow.admin.AdminService;
import com.example.membership_flow.admin.dto.BillingAttemptRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class BillingScheduler {

    private static final int MAX_DUNNING_ATTEMPTS = 3;

    private final AdminService adminService;
    private final BillingAttemptRepository billingAttemptRepository;

    /**
     * Daily at midnight: find ACTIVE contracts due today and fire billing attempts.
     * Each contract gets at most one PENDING or SUCCESS record per scheduledDate,
     * so re-runs are safe.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void processDailyBilling() {
        var today = LocalDate.now();
        log.info("Daily billing job started for {}", today);

        adminService.listSubscriptionContracts()
                .contracts().stream()
                .filter(c -> "ACTIVE".equals(c.status()))
                .filter(c -> isDueToday(c.nextBillingDate(), today))
                .filter(c -> !alreadyProcessed(c.id(), today))
                .forEach(c -> executeAttempt(c.id(), today, 1));
    }

    /**
     * Daily at 6am: retry FAILED attempts that haven't hit the dunning limit yet.
     * Shopify webhook sets nextRetryAt = now + 1 day on failure.
     */
    @Scheduled(cron = "0 0 6 * * *")
    public void processDunning() {
        log.info("Dunning retry job started");

        billingAttemptRepository
                .findByStatusAndAttemptCountLessThanAndNextRetryAtBefore(
                        BillingAttemptRecord.Status.FAILED, MAX_DUNNING_ATTEMPTS, Instant.now())
                .forEach(r -> executeAttempt(r.getContractId(), r.getScheduledDate(), r.getAttemptCount() + 1));
    }

    void executeAttempt(String contractId, LocalDate scheduledDate, int attemptCount) {
        var record = BillingAttemptRecord.builder()
                .contractId(contractId)
                .status(BillingAttemptRecord.Status.PENDING)
                .attemptCount(attemptCount)
                .scheduledDate(scheduledDate)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        billingAttemptRepository.save(record);

        try {
            var response = adminService.createBillingAttempt(new BillingAttemptRequest(contractId, attemptCount));
            record.setAttemptId(response.attemptId());
            record.setUpdatedAt(Instant.now());
            billingAttemptRepository.save(record);
            log.info("Billing attempt queued: {} for contract {} (attempt {})",
                    response.attemptId(), contractId, attemptCount);
        } catch (Exception ex) {
            // @Retryable already exhausted its retries on transient API errors by this point
            log.warn("Billing attempt API call failed for contract {} (attempt {}): {}",
                    contractId, attemptCount, ex.getMessage());
            record.setStatus(BillingAttemptRecord.Status.FAILED);
            record.setErrorMessage(ex.getMessage());
            record.setNextRetryAt(Instant.now().plus(1, ChronoUnit.DAYS));
            record.setUpdatedAt(Instant.now());
            billingAttemptRepository.save(record);
        }
    }

    private boolean isDueToday(String nextBillingDate, LocalDate today) {
        if (nextBillingDate == null) return false;
        try {
            return !LocalDate.parse(nextBillingDate.substring(0, 10)).isAfter(today);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean alreadyProcessed(String contractId, LocalDate today) {
        return billingAttemptRepository.existsByContractIdAndScheduledDateAndStatusIn(
                contractId, today,
                List.of(BillingAttemptRecord.Status.PENDING, BillingAttemptRecord.Status.SUCCESS));
    }
}
