package com.example.membership_flow.contract;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SubscriptionContractRepository extends MongoRepository<SubscriptionContractRecord, String> {

    Optional<SubscriptionContractRecord> findByContractId(String contractId);
}
