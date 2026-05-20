package com.example.membership_flow.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PauseContractRequest(
        @JsonProperty("contract_id") String contractId
) {}
