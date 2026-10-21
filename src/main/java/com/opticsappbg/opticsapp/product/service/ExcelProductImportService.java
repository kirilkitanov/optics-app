package com.opticsappbg.opticsapp.product.service;

import com.opticsappbg.opticsapp.product.model.Product;
import com.opticsappbg.opticsapp.product.model.ProductCategory;
import com.opticsappbg.opticsapp.product.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExcelProductImportService {

    private final ProductRepository productRepository;
    private final ProductService productService;

    @Transactional
    public ImportResult importExcel(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Моля, избери Excel файл.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null ||
                !filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException(
                    "Поддържат се Excel файлове с разширение .xlsx."
            );
        }

        int added = 0;
        int updated = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        Set<String> seenRows = new HashSet<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
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

            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper()
                    .createFormulaEvaluator();

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);

                if (row == null || isEmptyRow(row, formatter, evaluator)) {
                    continue;
                }

                String duplicateKey = null;
                boolean keyRegistered = false;
                try {
                    ProductCategory category = parseCategory(
                            getText(row, headers, formatter, evaluator,
                                    "категория", "category")
                    );

                    String brand = getText(
                            row, headers, formatter, evaluator, "бранд", "brand"
                    );

                    String model = getText(
                            row, headers, formatter, evaluator, "модел", "model"
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

                    BigDecimal sph = getDecimal(
                            row, headers, formatter, evaluator,
                            "sph", "сфера", "сферa"
                    );

                    BigDecimal cyl = getDecimal(
                            row, headers, formatter, evaluator,
                            "cyl", "цилиндър", "цилиндърдиоптър"
                    );

                    BigDecimal addPower = getDecimal(
                            row, headers, formatter, evaluator,
                            "add", "addpower", "ад", "добавка"
                    );

                    BigDecimal prism = getDecimal(
                            row, headers, formatter, evaluator,
                            "призма", "prism"
                    );

                    BigDecimal purchasePrice = getDecimal(
                            row, headers, formatter, evaluator,
                            "доставнацена", "ценаназакупуване",
                            "purchaseprice", "costprice"
                    );

                    BigDecimal retailPrice = getDecimal(
                            row, headers, formatter, evaluator,
                            "продажнацена", "ценанадребно",
                            "retailprice", "saleprice"
                    );

                    if (purchasePrice != null &&
                            purchasePrice.compareTo(BigDecimal.ZERO) < 0) {
                        throw new IllegalArgumentException(
                                "Доставната цена не може да бъде отрицателна."
                        );
                    }

                    if (retailPrice != null &&
                            retailPrice.compareTo(BigDecimal.ZERO) < 0) {
                        throw new IllegalArgumentException(
                                "Продажната цена не може да бъде отрицателна."
                        );
                    }

                    String note = getText(
                            row, headers, formatter, evaluator,
                            "бележка", "забележка", "note"
                    );

                    String normalizedBarcode = normalize(barcode);

                    if (normalizedBarcode != null) {
                        // При попълнен баркод проверяваме само него.
                        duplicateKey = "BARCODE:" + normalizedBarcode;
                    } else {
                        // Без баркод сравняваме всички основни характеристики.
                        duplicateKey = "CHAR:"
                                + category.name() + "|"
                                + normalizeKey(brand) + "|"
                                + normalizeKey(model) + "|"
                                + normalizeKey(size) + "|"
                                + normalizeNumber(sph) + "|"
                                + normalizeNumber(cyl) + "|"
                                + normalizeNumber(addPower) + "|"
                                + normalizeNumber(prism);
                    }

                    if (!seenRows.add(duplicateKey)) {
                        throw new IllegalArgumentException(
                                "Дублиран продукт в Excel файла. " +
                                        "Първо провери предишния ред със същия баркод " +
                                        "или същите характеристики."
                        );
                    }

                    keyRegistered = true;

                    Product existing = findExisting(
                            category, brand, model, barcode, size,
                            zeroIfNull(sph), zeroIfNull(cyl),
                            zeroIfNull(addPower), zeroIfNull(prism)
                    );

                    if (existing == null && retailPrice == null) {
                        throw new IllegalArgumentException(
                                "Новият продукт няма продажна цена."
                        );
                    }

                    if (existing == null) {
                        Product product = new Product();

                        product.setCategory(category);
                        product.setBrand(brand);
                        product.setModel(model);
                        product.setBarcode(normalize(barcode));
                        product.setSizeOrDiameter(normalize(size));
                        product.setSph(zeroIfNull(sph));
                        product.setCyl(zeroIfNull(cyl));
                        product.setAddPower(zeroIfNull(addPower));
                        product.setPrism(zeroIfNull(prism));
                        product.setQuantity(0);
                        product.setPurchasePrice(purchasePrice);
                        product.setRetailPrice(retailPrice);
                        product.setNote(note);
                        product.setCreatedOn(LocalDateTime.now());
                        product.setActive(true);

                        productRepository.save(product);
                        added++;
                    } else {
                        updateIfPresent(existing, category, brand, model,
                                barcode, size, sph, cyl, addPower, prism,
                                retailPrice, note);

                        if (purchasePrice != null) {
                            productService.updatePurchasePrice(
                                    existing.getId(), purchasePrice
                            );
                        }

                        productRepository.save(existing);
                        updated++;
                    }

                } catch (IllegalArgumentException ex) {
                    if (keyRegistered) {
                        seenRows.remove(duplicateKey);
                    }

                    skipped++;
                    errors.add("Ред " + (rowIndex + 1) + ": " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            throw ex;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Възникна неочаквана грешка при импортирането.",
                    ex
            );
        }

        return new ImportResult(added, updated, skipped, errors);
    }

    private Product findExisting(
            ProductCategory category,
            String brand,
            String model,
            String barcode,
            String size,
            BigDecimal sph,
            BigDecimal cyl,
            BigDecimal addPower,
            BigDecimal prism) {

        String normalizedBarcode = normalize(barcode);

        // 1. Ако има баркод, търсим само по него.
        if (normalizedBarcode != null) {
            Optional<Product> byBarcode =
                    productRepository.findByBarcode(normalizedBarcode);

            if (byBarcode.isEmpty()) {
                return null;
            }

            Product product = byBarcode.get();

            boolean sameCharacteristics =
                    product.getCategory() == category
                            && Objects.equals(
                            normalize(product.getBrand()),
                            normalize(brand))
                            && Objects.equals(
                            normalize(product.getModel()),
                            normalize(model))
                            && Objects.equals(
                            normalize(product.getSizeOrDiameter()),
                            normalize(size))
                            && product.getSph().compareTo(sph) == 0
                            && product.getCyl().compareTo(cyl) == 0
                            && product.getAddPower().compareTo(addPower) == 0
                            && product.getPrism().compareTo(prism) == 0;

            if (!sameCharacteristics) {
                throw new IllegalArgumentException(
                        "Баркодът вече съществува, но характеристиките "
                                + "на продукта са различни. "
                                + "Необходимо е да се провери редът."
                );
            }

            return product;
        }

        // 2. Ако няма баркод, търсим по характеристики.
        List<Product> matches = new ArrayList<>(
                productRepository.findProductsByCharacteristics(
                        category,
                        brand,
                        model,
                        normalize(size),
                        sph,
                        cyl,
                        addPower,
                        prism
                )
        );

        // Важно: изключваме всички продукти, които имат баркод.
        matches.removeIf(product ->
                normalize(product.getBarcode()) != null
        );

        // 3. Повече от един продукт без баркод със същите
        // характеристики означава, че има нееднозначност.
        if (matches.size() > 1) {
            throw new IllegalArgumentException(
                    "Намерени са няколко продукта без баркод "
                            + "със същите характеристики. "
                            + "Необходимо е ръчно уточняване."
            );
        }

        // 4. Ако има точно един продукт без баркод, обновяваме го.
        if (matches.size() == 1) {
            return matches.get(0);
        }

        // 5. Няма съвпадение без баркод — създаваме нов продукт.
        return null;
    }
    private void updateIfPresent(
            Product product,
            ProductCategory category,
            String brand,
            String model,
            String barcode,
            String size,
            BigDecimal sph,
            BigDecimal cyl,
            BigDecimal addPower,
            BigDecimal prism,
            BigDecimal retailPrice,
            String note) {

        // Тези полета са задължителни в Excel реда.
        product.setCategory(category);
        product.setBrand(brand);
        product.setModel(model);

        if (normalize(barcode) != null) {
            product.setBarcode(normalize(barcode));
        }
        if (normalize(size) != null) {
            product.setSizeOrDiameter(normalize(size));
        }
        if (sph != null) {
            product.setSph(sph);
        }
        if (cyl != null) {
            product.setCyl(cyl);
        }
        if (addPower != null) {
            product.setAddPower(addPower);
        }
        if (prism != null) {
            product.setPrism(prism);
        }
        if (retailPrice != null) {
            product.setRetailPrice(retailPrice);
        }
        if (normalize(note) != null) {
            product.setNote(note.trim());
        }

        product.setUpdatedOn(LocalDateTime.now());
    }

    private Map<String, Integer> readHeaders(Row row) {
        Map<String, Integer> headers = new HashMap<>();
        DataFormatter formatter = new DataFormatter();

        for (Cell cell : row) {
            String name = normalizeHeader(formatter.formatCellValue(cell));
            if (!name.isEmpty()) {
                headers.put(name, cell.getColumnIndex());
            }
        }

        return headers;
    }

    private void requireHeader(Map<String, Integer> headers, String... aliases) {
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

        String value = getText(row, headers, formatter, evaluator, aliases);
        if (value == null) {
            return null;
        }

        value = value.replace("\u00A0", "").replace(" ", "");

        // Поддържаме десетична запетая, например 1,25.
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

    private ProductCategory parseCategory(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Липсва категория.");
        }

        String normalized = normalizeHeader(value);

        for (ProductCategory category : ProductCategory.values()) {
            if (normalizeHeader(category.name()).equals(normalized) ||
                    normalizeHeader(category.getDisplayName()).equals(normalized)) {
                return category;
            }
        }

        throw new IllegalArgumentException(
                "Непозната категория: " + value
        );
    }

    private Integer findColumn(
            Map<String, Integer> headers, String... aliases) {

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
            if (!formatter.formatCellValue(cell, evaluator).trim().isEmpty()) {
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

    public static class ImportResult {
        private final int added;
        private final int updated;
        private final int skipped;
        private final List<String> errors;

        public ImportResult(
                int added, int updated, int skipped, List<String> errors) {
            this.added = added;
            this.updated = updated;
            this.skipped = skipped;
            this.errors = List.copyOf(errors);
        }

        public int getAdded() {
            return added;
        }

        public int getUpdated() {
            return updated;
        }

        public int getSkipped() {
            return skipped;
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    private String normalizeKey(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeNumber(BigDecimal value) {
        return value == null
                ? "0"
                : value.stripTrailingZeros().toPlainString();
    }
}