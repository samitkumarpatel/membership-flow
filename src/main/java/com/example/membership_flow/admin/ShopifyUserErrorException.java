package com.example.membership_flow.admin;

import com.example.membership_flow.shopify.graphql.UserError;

import java.util.List;
import java.util.stream.Collectors;

public class ShopifyUserErrorException extends RuntimeException {

    private final List<UserError> errors;

    public ShopifyUserErrorException(List<UserError> errors) {
        super(errors.stream().map(UserError::message).collect(Collectors.joining(", ")));
        this.errors = errors;
    }

    public List<UserError> getErrors() {
        return errors;
    }
}
