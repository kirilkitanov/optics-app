package com.opticsappbg.opticsapp.order.service;

import com.opticsappbg.opticsapp.order.model.Order;
import com.opticsappbg.opticsapp.order.model.OrderItem;
import com.opticsappbg.opticsapp.product.model.Product;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelOrderExportService {

    private static final String[] DETAIL_HEADERS = {
            "Номер на поръчка",
            "Дата",
            "Клиент",
            "Телефон",
            "Категория",
            "Баркод",
            "Марка",
            "Модел",
            "Размер/диаметър",
            "SPH",
            "CYL",
            "ADD",
            "PRISM",
            "Око",
            "Ос",
            "База",
            "Количество",
            "Единична продажна цена (EUR)",
            "Стойност на реда (EUR)",
            "Отстъпка (%)",
            "Единична цена след отстъпка (EUR)",
            "Стойност след отстъпка (EUR)",
            "Статус"
    };

    private static final String[] SUMMARY_HEADERS = {
            "Номер на поръчка",
            "Дата",
            "Клиент",
            "Телефон",
            "Сума преди отстъпка (EUR)",
            "Отстъпка (%)",
            "Крайна сума (EUR)",
            "Статус"
    };

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @Transactional(readOnly = true)
    public byte[] exportExcel(List<Order> orders) throws IOException {

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            CellStyle headerStyle = createHeaderStyle(workbook);

            DataFormat dataFormat = workbook.createDataFormat();

            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));

            CellStyle percentageStyle = workbook.createCellStyle();
            percentageStyle.setDataFormat(dataFormat.getFormat("0.##\\%"));

            createDetailSheet(
                    workbook,
                    orders,
                    headerStyle,
                    moneyStyle,
                    percentageStyle
            );

            createSummarySheet(
                    workbook,
                    orders,
                    headerStyle,
                    moneyStyle,
                    percentageStyle
            );

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void createDetailSheet(
            Workbook workbook,
            List<Order> orders,
            CellStyle headerStyle,
            CellStyle moneyStyle,
            CellStyle percentageStyle) {

        Sheet sheet = workbook.createSheet("Подробни поръчки");

        createHeader(sheet, DETAIL_HEADERS, headerStyle);

        int rowIndex = 1;

        for (Order order : orders) {

            if (order.getItems() == null || order.getItems().isEmpty()) {
                continue;
            }

            List<OrderItem> items = new ArrayList<>(order.getItems());

            List<BigDecimal> discountedLineTotals =
                    calculateDiscountedLineTotals(order, items);

            for (int i = 0; i < items.size(); i++) {

                OrderItem item = items.get(i);

                Row row = sheet.createRow(rowIndex++);

                Product product = item.getProduct();

                setText(row, 0, order.getOrderNumber());
                setText(row, 1, formatDate(order));
                setText(row, 2, getCustomerName(order));
                setText(row, 3, getCustomerPhone(order));

                if (product != null) {
                    setText(row, 4, product.getCategory() != null
                            ? product.getCategory().getDisplayName()
                            : null);

                    setText(row, 5, product.getBarcode());
                    setText(row, 6, product.getBrand());
                    setText(row, 7, product.getModel());
                    setValue(row, 8, product.getSizeOrDiameter());

                    setDiopter(row, 9, product.getSph());
                    setDiopter(row, 10, product.getCyl());
                    setDiopter(row, 11, product.getAddPower());
                    setDiopter(row, 12, product.getPrism());
                }

                setValue(row, 13, item.getEyeSide());
                setValue(row, 14, item.getAxis());
                setValue(row, 15, item.getPrismBase());
                setValue(row, 16, item.getQuantity());

                BigDecimal salePrice = item.getSalePriceAtSale();

                BigDecimal lineTotal = calculateLineTotal(item);

                setNumber(row, 17, salePrice, moneyStyle);
                setNumber(row, 18, lineTotal, moneyStyle);

                BigDecimal discountedLineTotal = discountedLineTotals.get(i);

                setNumber(
                        row,
                        19,
                        order.getDiscountPercentage(),
                        percentageStyle
                );

                BigDecimal discountedUnitPrice = null;

                if (discountedLineTotal != null
                        && item.getQuantity() != null
                        && item.getQuantity() > 0) {

                    discountedUnitPrice = discountedLineTotal.divide(
                            BigDecimal.valueOf(item.getQuantity()),
                            2,
                            RoundingMode.HALF_UP
                    );
                }

                setNumber(row, 20, discountedUnitPrice, moneyStyle);
                setNumber(row, 21, discountedLineTotal, moneyStyle);

                setText(row, 22, getStatus(order));
            }
        }

        finishSheet(sheet, DETAIL_HEADERS.length, rowIndex - 1);
    }

    private List<BigDecimal> calculateDiscountedLineTotals(
            Order order,
            List<OrderItem> items) {

        List<BigDecimal> result = new ArrayList<>();

        BigDecimal totalBeforeDiscount = BigDecimal.ZERO;

        for (OrderItem item : items) {
            totalBeforeDiscount = totalBeforeDiscount.add(
                    calculateLineTotal(item)
            );
        }

        BigDecimal totalAfterDiscount = order.getTotal() != null
                ? order.getTotal()
                : totalBeforeDiscount;

        BigDecimal remainingAmount = totalAfterDiscount;

        for (int i = 0; i < items.size(); i++) {

            BigDecimal discountedLineTotal;

            if (i == items.size() - 1) {

                // Последният ред получава остатъка от закръгляването.
                discountedLineTotal = remainingAmount;

            } else if (totalBeforeDiscount.compareTo(BigDecimal.ZERO) == 0) {

                discountedLineTotal = BigDecimal.ZERO;

            } else {

                BigDecimal originalLineTotal =
                        calculateLineTotal(items.get(i));

                discountedLineTotal = originalLineTotal
                        .multiply(totalAfterDiscount)
                        .divide(
                                totalBeforeDiscount,
                                2,
                                RoundingMode.HALF_UP
                        );
            }

            result.add(discountedLineTotal);
            remainingAmount = remainingAmount.subtract(discountedLineTotal);
        }

        return result;
    }

    private BigDecimal calculateLineTotal(OrderItem item) {

        if (item.getSalePriceAtSale() == null || item.getQuantity() == null) {
            return BigDecimal.ZERO;
        }

        return item.getSalePriceAtSale().multiply(
                BigDecimal.valueOf(item.getQuantity())
        );
    }

    private void createSummarySheet(
            Workbook workbook,
            List<Order> orders,
            CellStyle headerStyle,
            CellStyle moneyStyle,
            CellStyle percentageStyle) {

        Sheet sheet = workbook.createSheet("Обобщени поръчки");

        createHeader(sheet, SUMMARY_HEADERS, headerStyle);

        int rowIndex = 1;

        for (Order order : orders) {

            Row row = sheet.createRow(rowIndex++);

            setText(row, 0, order.getOrderNumber());
            setText(row, 1, formatDate(order));
            setText(row, 2, getCustomerName(order));
            setText(row, 3, getCustomerPhone(order));

            setNumber(row, 4, order.getTotalBeforeDiscount(), moneyStyle);
            setNumber(row, 5, order.getDiscountPercentage(), percentageStyle);
            setNumber(row, 6, order.getTotal(), moneyStyle);

            setText(row, 7, getStatus(order));
        }

        finishSheet(sheet, SUMMARY_HEADERS.length, rowIndex - 1);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {

        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setBold(true);

        style.setFont(font);
        style.setFillForegroundColor(
                IndexedColors.GREY_25_PERCENT.getIndex()
        );
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        return style;
    }

    private void createHeader(
            Sheet sheet,
            String[] headers,
            CellStyle headerStyle) {

        Row headerRow = sheet.createRow(0);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void finishSheet(
            Sheet sheet,
            int columnCount,
            int lastRowIndex) {

        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);

            if (sheet.getColumnWidth(i) > 15000) {
                sheet.setColumnWidth(i, 15000);
            }
        }

        sheet.createFreezePane(0, 1);

        if (lastRowIndex >= 1) {
            sheet.setAutoFilter(
                    new CellRangeAddress(
                            0,
                            lastRowIndex,
                            0,
                            columnCount - 1
                    )
            );
        }
    }

    private String formatDate(Order order) {
        return order.getCreatedOn() != null
                ? order.getCreatedOn().format(DATE_FORMATTER)
                : null;
    }

    private String getCustomerName(Order order) {
        return order.getCustomer() != null
                ? order.getCustomer().getFullName()
                : null;
    }

    private String getCustomerPhone(Order order) {
        return order.getCustomer() != null
                ? order.getCustomer().getPhone()
                : null;
    }

    private String getStatus(Order order) {
        return order.getStatus() != null
                ? order.getStatus().getDisplayName()
                : null;
    }

    private void setText(Row row, int columnIndex, String value) {

        Cell cell = row.createCell(columnIndex);

        if (value != null) {
            cell.setCellValue(value);
        }
    }

    private void setValue(Row row, int columnIndex, Object value) {

        Cell cell = row.createCell(columnIndex);

        if (value == null) {
            return;
        }

        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private void setNumber(
            Row row,
            int columnIndex,
            BigDecimal value,
            CellStyle style) {

        Cell cell = row.createCell(columnIndex);

        if (value != null) {
            cell.setCellValue(value.doubleValue());
            cell.setCellStyle(style);
        }
    }

    private void setDiopter(Row row, int columnIndex, Object value) {

        if (value == null) {
            return;
        }

        if (value instanceof BigDecimal decimal
                && decimal.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        if (value instanceof Number number
                && number.doubleValue() == 0.0) {
            return;
        }

        setValue(row, columnIndex, value);
    }
}
