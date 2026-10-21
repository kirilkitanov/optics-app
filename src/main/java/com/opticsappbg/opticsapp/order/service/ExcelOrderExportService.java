
package com.opticsappbg.opticsapp.order.service;

import com.opticsappbg.opticsapp.order.model.Order;
import com.opticsappbg.opticsapp.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelOrderExportService {

    private final OrderRepository orderRepository;

    private static final String[] HEADERS = {
            "Дата",
            "Номер на поръчка",
            "Клиент",
            "Телефон",
            "Сума (EUR)",
            "Статус"
    };

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");


    @Transactional(readOnly = true)
    public byte[] exportExcel(
            LocalDate startDate,
            LocalDate endDate) throws IOException {

        List<Order> orders;

        if (startDate == null && endDate == null) {
            orders = orderRepository.findAllByOrderByCreatedOnDesc();
        } else if (startDate == null) {
            orders = orderRepository
                    .findByCreatedOnLessThanOrderByCreatedOnDesc(
                            endDate.plusDays(1).atStartOfDay()
                    );
        } else if (endDate == null) {
            orders = orderRepository
                    .findByCreatedOnGreaterThanEqualOrderByCreatedOnDesc(
                            startDate.atStartOfDay()
                    );
        } else {
            orders = orderRepository
                    .findByCreatedOnBetweenOrderByCreatedOnDesc(
                            startDate.atStartOfDay(),
                            endDate.plusDays(1)
                                    .atStartOfDay()
                                    .minusNanos(1)
                    );
        }

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream =
                     new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Поръчки");

            // Стил за заглавията
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(
                    IndexedColors.GREY_25_PERCENT.getIndex()
            );
            headerStyle.setFillPattern(
                    FillPatternType.SOLID_FOREGROUND
            );

            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Данни за поръчките
            int rowIndex = 1;

            for (Order order : orders) {
                Row row = sheet.createRow(rowIndex++);

                String date = order.getCreatedOn() != null
                        ? order.getCreatedOn().format(DATE_FORMATTER)
                        : null;

                String customerName = order.getCustomer() != null
                        ? order.getCustomer().getFullName()
                        : null;

                String phone = order.getCustomer() != null
                        ? order.getCustomer().getPhone()
                        : null;

                setText(row, 0, date);
                setText(row, 1, order.getOrderNumber());
                setText(row, 2, customerName);
                setText(row, 3, phone);
                setNumber(row, 4, order.getTotal());
                setText(row, 5, order.getStatus() != null
                        ? order.getStatus().getDisplayName()
                        : null);
            }

            // Автоматична ширина на колоните
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);

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

    private void setText(
            Row row,
            int columnIndex,
            String value) {

        Cell cell = row.createCell(columnIndex);

        if (value != null) {
            cell.setCellValue(value);
        }
    }

    private void setNumber(
            Row row,
            int columnIndex,
            BigDecimal value) {

        Cell cell = row.createCell(columnIndex);

        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
    }
}
