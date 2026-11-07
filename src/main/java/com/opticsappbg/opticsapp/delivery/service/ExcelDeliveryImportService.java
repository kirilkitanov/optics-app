
package com.opticsappbg.opticsapp.delivery.service;

import com.opticsappbg.opticsapp.product.model.ProductCategory;
import com.opticsappbg.opticsapp.product.service.ProductService;
import com.opticsappbg.opticsapp.web.dto.DeliveryImportPreview;
import com.opticsappbg.opticsapp.web.dto.DeliveryImportPreviewItem;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExcelDeliveryImportService {

    private final ProductService productService;

    @Transactional(readOnly = true)
    public DeliveryImportPreview previewExcel(MultipartFile file)
            throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Моля, избери Excel файл."
            );
        }

        String filename = file.getOriginalFilename();

        if (filename == null ||
                !filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException(
                    "Поддържат се Excel файлове с разширение .xlsx."
            );
        }

        DeliveryImportPreview preview = new DeliveryImportPreview();
        preview.setFileName(filename);

        try (Workbook workbook =
                     WorkbookFactory.create(file.getInputStream())) {

            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException(
                        "Excel файлът няма работен лист."
                );
            }

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                throw new IllegalArgumentException(
                        "Не е намерен ред със заглавия на колоните."
                );
            }

            Map<String, Integer> headers = readHeaders(headerRow);

            requireHeader(headers, "категория", "category");
            requireHeader(headers, "бранд", "brand");
            requireHeader(headers, "модел", "model");
            requireHeader(headers, "количество", "quantity", "qty");
            requireHeader(
                    headers,
                    "доставнацена",
                    "ценаназакупуване",
                    "purchaseprice",
                    "costprice"
            );

            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper()
                    .createFormulaEvaluator();

            // Следим баркодовете на новите продукти.
            Map<String, String> barcodeByNewProductKey = new HashMap<>();
            Map<String, String> newProductKeyByBarcode = new HashMap<>();

            for (int rowIndex = 1;
                 rowIndex <= sheet.getLastRowNum();
                 rowIndex++) {

                Row row = sheet.getRow(rowIndex);

                if (row == null ||
                        isEmptyRow(row, formatter, evaluator)) {
                    continue;
                }

                DeliveryImportPreviewItem item =
                        new DeliveryImportPreviewItem();

                item.setExcelRowNumber(rowIndex + 1);

                try {
                    parseRow(
                            row,
                            headers,
                            formatter,
                            evaluator,
                            item
                    );

                    // Разпознаваме продукта, без да записваме промени.
                    productService.matchProductForDeliveryImport(item);

                    if (item.getErrorMessage() == null) {
                        validateNewProductBarcodes(
                                item,
                                barcodeByNewProductKey,
                                newProductKeyByBarcode
                        );
                    }

                } catch (IllegalArgumentException ex) {
                    item.setErrorMessage(ex.getMessage());
                }

                preview.getItems().add(item);
            }

            if (preview.getItems().isEmpty()) {
                preview.getErrors().add(
                        "В Excel файла няма намерени продуктови редове."
                );
            }

        } catch (IOException ex) {
            throw ex;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Възникна неочаквана грешка при прочитането на Excel файла.",
                    ex
            );
        }

        return preview;
    }

    private void parseRow(
            Row row,
            Map<String, Integer> headers,
            DataFormatter formatter,
            FormulaEvaluator evaluator,
            DeliveryImportPreviewItem item) {

        ProductCategory category = parseCategory(
                getText(
                        row, headers, formatter, evaluator,
                        "категория", "category"
                )
        );

        String brand = getText(
                row, headers, formatter, evaluator,
                "бранд", "brand"
        );

        String model = getText(
                row, headers, formatter, evaluator,
                "модел", "model"
        );

        if (brand == null || model == null) {
            throw new IllegalArgumentException(
                    "Липсва бранд или модел."
            );
        }

        String barcode = getText(
                row, headers, formatter, evaluator,
                "баркод", "barcode", "ean"
        );

        String size = getText(
                row, headers, formatter, evaluator,
                "размер", "диаметър", "размердиаметър",
                "sizeordiameter", "diameter", "size"
        );

        BigDecimal sph = zeroIfNull(
                getDecimal(
                        row, headers, formatter, evaluator,
                        "sph", "сфера"
                )
        );

        BigDecimal cyl = zeroIfNull(
                getDecimal(
                        row, headers, formatter, evaluator,
                        "cyl", "цилиндър"
                )
        );

        BigDecimal addPower = zeroIfNull(
                getDecimal(
                        row, headers, formatter, evaluator,
                        "add", "addpower", "ад", "добавка"
                )
        );

        BigDecimal prism = zeroIfNull(
                getDecimal(
                        row, headers, formatter, evaluator,
                        "призма", "prism"
                )
        );

        Integer quantity = getInteger(
                row, headers, formatter, evaluator,
                "количество", "quantity", "qty"
        );

        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException(
                    "Количеството трябва да бъде цяло число, по-голямо от нула."
            );
        }

        BigDecimal purchasePrice = getDecimal(
                row, headers, formatter, evaluator,
                "доставнацена",
                "ценаназакупуване",
                "purchaseprice",
                "costprice"
        );

        if (purchasePrice == null) {
            throw new IllegalArgumentException(
                    "Липсва доставна цена."
            );
        }

        if (purchasePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Доставната цена не може да бъде отрицателна."
            );
        }

        item.setCategory(category);
        item.setBrand(brand.trim());
        item.setModel(model.trim());
        item.setBarcode(normalize(barcode));
        item.setSizeOrDiameter(normalize(size));
        item.setSph(sph);
        item.setCyl(cyl);
        item.setAddPower(addPower);
        item.setPrism(prism);
        item.setQuantity(quantity);
        item.setPurchasePriceAtDelivery(purchasePrice);
    }

    private void validateNewProductBarcodes(
            DeliveryImportPreviewItem item,
            Map<String, String> barcodeByNewProductKey,
            Map<String, String> newProductKeyByBarcode) {

        if (!"NEW".equals(item.getProductMatchType())) {
            return;
        }

        String key = item.getNewProductKey();
        String barcode = normalize(item.getBarcode());

        String existingBarcode = barcodeByNewProductKey.get(key);

        if (existingBarcode != null
                && barcode != null
                && !existingBarcode.equals(barcode)) {

            item.setErrorMessage(
                    "Редът описва същия нов продукт като друг ред, "
                            + "но има различен баркод."
            );
            return;
        }

        if (barcode != null) {
            String existingKey = newProductKeyByBarcode.get(barcode);

            if (existingKey != null && !existingKey.equals(key)) {
                item.setErrorMessage(
                        "Този баркод е посочен и за друг нов продукт "
                                + "в Excel файла."
                );
                return;
            }

            barcodeByNewProductKey.putIfAbsent(key, barcode);
            newProductKeyByBarcode.putIfAbsent(barcode, key);
        }
    }

    private Map<String, Integer> readHeaders(Row row) {

        Map<String, Integer> headers = new HashMap<>();
        DataFormatter formatter = new DataFormatter();

        for (Cell cell : row) {
            String name = normalizeHeader(
                    formatter.formatCellValue(cell)
            );

            if (!name.isEmpty()) {
                headers.put(name, cell.getColumnIndex());
            }
        }

        return headers;
    }

    private void requireHeader(
            Map<String, Integer> headers,
            String... aliases) {

        if (findColumn(headers, aliases) == null) {
            throw new IllegalArgumentException(
                    "Липсва задължителна колона: " + aliases[0]
            );
        }
    }

    private String getText(
            Row row,
            Map<String, Integer> headers,
            DataFormatter formatter,
            FormulaEvaluator evaluator,
            String... aliases) {

        Integer column = findColumn(headers, aliases);

        if (column == null) {
            return null;
        }

        Cell cell = row.getCell(column);

        if (cell == null) {
            return null;
        }

        String value = formatter.formatCellValue(cell, evaluator).trim();

        return value.isEmpty() ? null : value;
    }

    private BigDecimal getDecimal(
            Row row,
            Map<String, Integer> headers,
            DataFormatter formatter,
            FormulaEvaluator evaluator,
            String... aliases) {

        String value = getText(
                row, headers, formatter, evaluator, aliases
        );

        if (value == null) {
            return null;
        }

        value = value.replace("\u00A0", "").replace(" ", "");

        if (value.contains(",") && value.contains(".")) {
            if (value.lastIndexOf(',') > value.lastIndexOf('.')) {
                value = value.replace(".", "").replace(',', '.');
            } else {
                value = value.replace(",", "");
            }
        } else {
            value = value.replace(',', '.');
        }

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "Невалидна числова стойност: " + value
            );
        }
    }

    private Integer getInteger(
            Row row,
            Map<String, Integer> headers,
            DataFormatter formatter,
            FormulaEvaluator evaluator,
            String... aliases) {

        BigDecimal value = getDecimal(
                row, headers, formatter, evaluator, aliases
        );

        if (value == null) {
            return null;
        }

        try {
            return value.intValueExact();
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(
                    "Количеството трябва да бъде цяло число."
            );
        }
    }

    private ProductCategory parseCategory(String value) {

        if (value == null) {
            throw new IllegalArgumentException(
                    "Липсва категория."
            );
        }

        String normalized = normalizeHeader(value);

        for (ProductCategory category : ProductCategory.values()) {
            if (normalizeHeader(category.name()).equals(normalized)
                    || normalizeHeader(category.getDisplayName())
                    .equals(normalized)) {
                return category;
            }
        }

        throw new IllegalArgumentException(
                "Непозната категория: " + value
        );
    }

    private Integer findColumn(
            Map<String, Integer> headers,
            String... aliases) {

        for (String alias : aliases) {
            Integer column = headers.get(normalizeHeader(alias));

            if (column != null) {
                return column;
            }
        }

        return null;
    }

    private boolean isEmptyRow(
            Row row,
            DataFormatter formatter,
            FormulaEvaluator evaluator) {

        for (Cell cell : row) {
            if (!formatter.formatCellValue(cell, evaluator)
                    .trim().isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private String normalizeHeader(String value) {

        if (value == null) {
            return "";
        }

        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s_./-]+", "")
                .replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private String normalize(String value) {

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
