package com.opticsappbg.opticsapp.product.service;

import com.opticsappbg.opticsapp.product.model.Product;
import com.opticsappbg.opticsapp.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelProductExportService {

    private final ProductRepository productRepository;

    private static final String[] HEADERS = {
            "Категория",
            "Бранд",
            "Модел",
            "Баркод",
            "Наличност",
            "Размер",
            "SPH",
            "CYL",
            "ADD",
            "Призма",
            "Доставна цена",
            "Продажна цена",
            "Бележка"
    };

    @Transactional(readOnly = true)
    public byte[] exportExcel() throws IOException {
        List<Product> products = productRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Продукти");

            // Стил за заглавията
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(
                    IndexedColors.GREY_25_PERCENT.getIndex()
            );
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Данни за продуктите
            int rowIndex = 1;

            for (Product product : products) {
                Row row = sheet.createRow(rowIndex++);

                setText(row, 0, product.getCategory() != null
                        ? product.getCategory().getDisplayName() : null);
                setText(row, 1, product.getBrand());
                setText(row, 2, product.getModel());
                setText(row, 3, product.getBarcode());

                Cell quantityCell = row.createCell(4);
                if (product.getQuantity() != null) {
                    quantityCell.setCellValue(product.getQuantity());
                }

                setText(row, 5, product.getSizeOrDiameter());
                setNumber(row, 6, product.getSph());
                setNumber(row, 7, product.getCyl());
                setNumber(row, 8, product.getAddPower());
                setNumber(row, 9, product.getPrism());
                setNumber(row, 10, product.getPurchasePrice());
                setNumber(row, 11, product.getRetailPrice());
                setText(row, 12, product.getNote());
            }

            // Автоматична ширина на колоните
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);

                // Ограничаваме прекалено широките колони
                if (sheet.getColumnWidth(i) > 15000) {
                    sheet.setColumnWidth(i, 15000);
                }
            }

            // Заглавията остават видими при скролиране
            sheet.createFreezePane(0, 1);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void setText(Row row, int columnIndex, String value) {
        Cell cell = row.createCell(columnIndex);

        if (value != null) {
            cell.setCellValue(value);
        }
    }

    private void setNumber(Row row, int columnIndex, BigDecimal value) {
        Cell cell = row.createCell(columnIndex);

        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
    }
}