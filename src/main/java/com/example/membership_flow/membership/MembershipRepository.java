package com.example.membership_flow.membership;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends MongoRepository<Membership, String> {
    List<Membership> findByMemberId(String memberId);
    List<Membership> findByStatus(Membership.MembershipStatus status);
    Optional<Membership> findByGatewaySubscriptionId(String gatewaySubscriptionId);
}
