package com.example.membership_flow.common;

import com.example.membership_flow.admin.ShopifyUserErrorException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ShopifyUserErrorException.class)
    ProblemDetail handleShopifyUserError(ShopifyUserErrorException ex) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        detail.setTitle("Shopify API Error");
        detail.setProperty("errors", ex.getErrors());
        return detail;
    }

    @ExceptionHandler(RestClientResponseException.class)
    ProblemDetail handleShopifyHttpError(RestClientResponseException ex) {
        var detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.valueOf(ex.getStatusCode().value()),
                "Shopify API request failed: " + ex.getMessage()
        );
        detail.setTitle("Shopify HTTP Error");
        return detail;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleGeneral(Exception ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }
}
