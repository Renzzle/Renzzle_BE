package com.renzzle.backend.domain.payment.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// Stored by name in a plain VARCHAR. @Enumerated would give a MySQL enum or a CHECK constraint on new tables,
// and either needs a column change for every new status
@Converter
public class InAppPurchaseStatusConverter implements AttributeConverter<InAppPurchaseStatus, String> {

    @Override
    public String convertToDatabaseColumn(InAppPurchaseStatus status) {
        return status == null ? null : status.name();
    }

    @Override
    public InAppPurchaseStatus convertToEntityAttribute(String column) {
        return column == null ? null : InAppPurchaseStatus.valueOf(column);
    }
}
