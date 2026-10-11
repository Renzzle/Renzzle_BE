package com.renzzle.backend.domain.payment.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// Google Play Developer API purchases.voidedpurchases.list response
@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleVoidedPurchasePage(
        // Google leaves this out when nothing was voided
        List<GoogleVoidedPurchase> voidedPurchases,
        TokenPagination tokenPagination
) {

    public List<GoogleVoidedPurchase> voidedPurchasesOrEmpty() {
        return voidedPurchases == null ? List.of() : voidedPurchases;
    }

    public String nextPageToken() {
        return tokenPagination == null ? null : tokenPagination.nextPageToken();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TokenPagination(
            String nextPageToken
    ) {
    }
}
