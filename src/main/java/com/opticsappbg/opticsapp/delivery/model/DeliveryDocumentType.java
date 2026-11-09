package com.opticsappbg.opticsapp.delivery.model;

public enum DeliveryDocumentType {

    INVOICE("Фактура"),
    CREDIT_NOTE("Кредитно известие");

    private final String displayName;

    DeliveryDocumentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}