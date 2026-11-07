package com.opticsappbg.opticsapp.web.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DeliveryImportPreview {

    private String fileName;

    private List<DeliveryImportPreviewItem> items = new ArrayList<>();

    private List<String> errors = new ArrayList<>();

    public boolean hasErrors() {
        return !errors.isEmpty()
                || items.stream().anyMatch(item ->
                item.getErrorMessage() != null
                        && !item.getErrorMessage().isBlank());
    }

    public boolean isValid() {
        return !items.isEmpty() && !hasErrors();
    }
}