package com.opticsappbg.opticsapp.delivery.service;

import com.opticsappbg.opticsapp.delivery.model.Delivery;
import com.opticsappbg.opticsapp.delivery.model.DeliveryItem;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelDeliveryExportService {

    private final DeliveryService deliveryService;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Transactional(readOnly = true)
    public byte[] exportDelivery(Long deliveryId) throws IOException {

        Delivery delivery = deliveryService.getDeliveryById(deliveryId);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Доставка");

            // Стилове
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(boldFont);
            headerStyle.setFillForegroundColor(
                    IndexedColors.GREY_25_PERCENT.getIndex()
            );
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle boldStyle = workbook.createCellStyle();
            boldStyle.setFont(boldFont);

            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setDataFormat(
                    workbook.createDataFormat().getFormat("#,##0.00")
            );

            // Заглавие
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("ДАННИ ЗА ДОСТАВКА");
            titleCell.setCellStyle(boldStyle);

            // Информация за доставката
            addInfoRow(sheet, 2, "Доставчик",
                    delivery.getSupplier().getCompanyName(), boldStyle);

            addInfoRow(sheet, 3, "Фактура №",
                    delivery.getInvoiceNumber(), boldStyle);

            addInfoRow(sheet, 4, "Тип документ",
                    delivery.getDocumentType() != null
                            ? delivery.getDocumentType().getDisplayName()
                            : "", boldStyle);

            addInfoRow(sheet, 5, "Дата на фактура",
                    delivery.getInvoiceDate() != null
                            ? delivery.getInvoiceDate().format(DATE_FORMAT)
                            : "", boldStyle);

            addInfoRow(sheet, 6, "Бележка",
                    delivery.getNote(), boldStyle);

            addInfoRow(sheet, 7, "Сума преди отстъпка",
                    delivery.getTotalBeforeDiscount(), boldStyle, moneyStyle);

            addInfoRow(sheet, 8, "Отстъпка (%)",
                    delivery.getDiscountPercentage(), boldStyle, moneyStyle);

            addInfoRow(sheet, 9, "Крайна сума",
                    delivery.getTotalAmount(), boldStyle, moneyStyle);

            addInfoRow(sheet, 10, "Обновяване на доставните цени",
                    delivery.isUpdateProductPurchasePrices() ? "Да" : "Не",
                    boldStyle);

            // Заглавия на таблицата
            int headerRowIndex = 12;

            String[] headers = {
                    "№",
                    "Категория",
                    "Баркод",
                    "Марка",
                    "Модел",
                    "Размер/Диаметър",
                    "SPH",
                    "CYL",
                    "ADD",
                    "PRISM",
                    "Количество",
                    "Оригинална доставна цена",
                    "Доставна цена след отстъпка",
                    "Оригинална стойност",
                    "Стойност след отстъпка"
            };

            Row headerRow = sheet.createRow(headerRowIndex);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Артикули
            int rowIndex = headerRowIndex + 1;
            int itemNumber = 1;

            for (DeliveryItem item : delivery.getItems()) {

                var product = item.getProduct();

                Row row = sheet.createRow(rowIndex++);

                BigDecimal originalPrice =
                        item.getOriginalPurchasePriceAtDelivery();

                BigDecimal discountedPrice =
                        item.getPurchasePriceAtDelivery();

                BigDecimal quantity =
                        BigDecimal.valueOf(item.getQuantity());

                BigDecimal originalTotal =
                        originalPrice.multiply(quantity);

                BigDecimal discountedTotal =
                        discountedPrice.multiply(quantity);

                int col = 0;

                setNumber(row, col++, itemNumber++);
                setText(row, col++, product.getCategory() != null
                        ? product.getCategory().getDisplayName() : "");
                setText(row, col++, product.getBarcode());
                setText(row, col++, product.getBrand());
                setText(row, col++, product.getModel());
                setText(row, col++, product.getSizeOrDiameter());

                setDiopter(row, col++, product.getSph(), moneyStyle);
                setDiopter(row, col++, product.getCyl(), moneyStyle);
                setDiopter(row, col++, product.getAddPower(), moneyStyle);
                setDiopter(row, col++, product.getPrism(), moneyStyle);

                setNumber(row, col++, item.getQuantity());

                setDecimal(row, col++, originalPrice, moneyStyle);
                setDecimal(row, col++, discountedPrice, moneyStyle);
                setDecimal(row, col++, originalTotal, moneyStyle);
                setDecimal(row, col, discountedTotal, moneyStyle);
            }

            // Автоматичен филтър върху таблицата
            if (!delivery.getItems().isEmpty()) {
                sheet.setAutoFilter(
                        new org.apache.poi.ss.util.CellRangeAddress(
                                headerRowIndex,
                                rowIndex - 1,
                                0,
                                headers.length - 1
                        )
                );
            }

            // Автоматична ширина на колоните
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i, false);
            }

            workbook.write(outputStream);

            return outputStream.toByteArray();
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportDeliveries(List<Delivery> deliveries) throws IOException {

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Доставки");

            // Стилове
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(boldFont);
            headerStyle.setFillForegroundColor(
                    IndexedColors.GREY_25_PERCENT.getIndex()
            );
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setDataFormat(
                    workbook.createDataFormat().getFormat("#,##0.00")
            );

            // Заглавия
            String[] headers = {
                    "№",
                    "Доставчик",
                    "Фактура №",
                    "Тип документ",
                    "Дата на фактура",
                    "Категория",
                    "Баркод",
                    "Марка",
                    "Модел",
                    "Размер/Диаметър",
                    "SPH",
                    "CYL",
                    "ADD",
                    "PRISM",
                    "Количество",
                    "Оригинална доставна цена",
                    "Доставна цена след отстъпка",
                    "Оригинална стойност",
                    "Стойност след отстъпка"
            };

            int headerRowIndex = 0;
            Row headerRow = sheet.createRow(headerRowIndex);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Данни
            int rowIndex = 1;
            int rowNumber = 1;

            for (Delivery delivery : deliveries) {

                String supplierName = delivery.getSupplier() != null
                        ? delivery.getSupplier().getCompanyName()
                        : "";

                String invoiceNumber = delivery.getInvoiceNumber();

                String documentType = delivery.getDocumentType() != null
                        ? delivery.getDocumentType().getDisplayName()
                        : "";

                String invoiceDate = delivery.getInvoiceDate() != null
                        ? delivery.getInvoiceDate().format(DATE_FORMAT)
                        : "";

                // Доставка без артикули
                if (delivery.getItems() == null || delivery.getItems().isEmpty()) {

                    Row row = sheet.createRow(rowIndex++);

                    setNumber(row, 0, rowNumber++);
                    setText(row, 1, supplierName);
                    setText(row, 2, invoiceNumber);
                    setText(row, 3, documentType);
                    setText(row, 4, invoiceDate);

                    continue;
                }

                // Ред за всеки артикул от доставката
                for (DeliveryItem item : delivery.getItems()) {

                    var product = item.getProduct();

                    Row row = sheet.createRow(rowIndex++);

                    BigDecimal originalPrice =
                            item.getOriginalPurchasePriceAtDelivery();

                    BigDecimal discountedPrice =
                            item.getPurchasePriceAtDelivery();

                    BigDecimal quantity =
                            BigDecimal.valueOf(item.getQuantity());

                    BigDecimal originalTotal = originalPrice != null
                            ? originalPrice.multiply(quantity)
                            : null;

                    BigDecimal discountedTotal = discountedPrice != null
                            ? discountedPrice.multiply(quantity)
                            : null;

                    int col = 0;

                    setNumber(row, col++, rowNumber++);
                    setText(row, col++, supplierName);
                    setText(row, col++, invoiceNumber);
                    setText(row, col++, documentType);
                    setText(row, col++, invoiceDate);

                    setText(row, col++, product != null
                            && product.getCategory() != null
                            ? product.getCategory().getDisplayName() : "");

                    setText(row, col++, product != null
                            ? product.getBarcode() : "");

                    setText(row, col++, product != null
                            ? product.getBrand() : "");

                    setText(row, col++, product != null
                            ? product.getModel() : "");

                    setText(row, col++, product != null
                            ? product.getSizeOrDiameter() : "");

                    setDiopter(row, col++, product != null
                            ? product.getSph() : null, moneyStyle);

                    setDiopter(row, col++, product != null
                            ? product.getCyl() : null, moneyStyle);

                    setDiopter(row, col++, product != null
                            ? product.getAddPower() : null, moneyStyle);

                    setDiopter(row, col++, product != null
                            ? product.getPrism() : null, moneyStyle);

                    setNumber(row, col++, item.getQuantity());

                    setDecimal(row, col++, originalPrice, moneyStyle);
                    setDecimal(row, col++, discountedPrice, moneyStyle);
                    setDecimal(row, col++, originalTotal, moneyStyle);
                    setDecimal(row, col, discountedTotal, moneyStyle);
                }
            }

            // Автоматичен филтър
            if (rowIndex > 1) {
                sheet.setAutoFilter(
                        new org.apache.poi.ss.util.CellRangeAddress(
                                headerRowIndex,
                                rowIndex - 1,
                                0,
                                headers.length - 1
                        )
                );
            }

            // Автоматична ширина на колоните
            // Автоматична ширина на колоните
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i, false);
            }

            workbook.write(outputStream);

            return outputStream.toByteArray();
        }
    }

    private void addInfoRow(
            Sheet sheet,
            int rowIndex,
            String label,
            String value,
            CellStyle labelStyle) {

        Row row = sheet.createRow(rowIndex);

        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);

        row.createCell(1).setCellValue(value != null ? value : "");
    }

    private void addInfoRow(
            Sheet sheet,
            int rowIndex,
            String label,
            BigDecimal value,
            CellStyle labelStyle,
            CellStyle valueStyle) {

        Row row = sheet.createRow(rowIndex);

        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);

        Cell valueCell = row.createCell(1);

        if (value != null) {
            valueCell.setCellValue(value.doubleValue());
            valueCell.setCellStyle(valueStyle);
        }
    }

    private void setText(Row row, int column, String value) {
        row.createCell(column).setCellValue(value != null ? value : "");
    }

    private void setNumber(Row row, int column, Number value) {
        row.createCell(column).setCellValue(value.doubleValue());
    }

    private void setDecimal(
            Row row,
            int column,
            BigDecimal value,
            CellStyle style) {

        Cell cell = row.createCell(column);

        if (value != null) {
            cell.setCellValue(value.doubleValue());
            cell.setCellStyle(style);
        }
    }

    private void setDiopter(
            Row row,
            int column,
            BigDecimal value,
            CellStyle style) {

        if (value != null && value.compareTo(BigDecimal.ZERO) == 0) {
            setDecimal(row, column, null, style);
        } else {
            setDecimal(row, column, value, style);
        }
    }
}
