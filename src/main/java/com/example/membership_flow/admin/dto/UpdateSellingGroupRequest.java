package com.example.membership_flow.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdateSellingGroupRequest(
        @JsonProperty("group_id") String groupId,
        String name
) {}
