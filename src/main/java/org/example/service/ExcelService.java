package org.example.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExcelService {

    private static final String EXCEL_FILE_NAME_TEMPLATE = "PrometheusData_%s.xlsx";
    private static final List<String> HTTP_REQUEST_LATENCY_HEADERS = Arrays.asList("Instance", "Method", "Status", "Outcome", "Exception", "URI", "Timestamp", "Duration (s)");
    private static final List<String> REPOSITORY_REQUEST_LATENCY_HEADERS = Arrays.asList("Instance", "Repository", "Method", "State", "Exception", "Timestamp", "Duration (s)");
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ExcelService() {
    }

    public Workbook createWorkbook() {
        return new XSSFWorkbook();
    }

    public String getExcelFileName() {
        return String.format(EXCEL_FILE_NAME_TEMPLATE, LocalDateTime.now().toLocalDate());
    }

    public void writeHttpRequestLatencyToExcel(Workbook workbook, String sheetName, String response) throws IOException {
        List<Object[]> rowDataList = parseHttpRequestLatencyJsonResponse(response);
        Sheet sheet = createSheetWithHttpRequestLatencyHeaders(workbook, sheetName);

        writeToExcel(rowDataList, HTTP_REQUEST_LATENCY_HEADERS.indexOf("Timestamp") + 1, sheet, workbook);
    }

    public void writeRepositoryRequestLatencyToExcel(Workbook workbook, String sheetName, String response) throws IOException {
        List<Object[]> rowDataList = parseRepositoryRequestLatencyJsonResponse(response);
        Sheet sheet = createSheetWithRepositoryRequestLatencyHeaders(workbook, sheetName);

        writeToExcel(rowDataList, REPOSITORY_REQUEST_LATENCY_HEADERS.indexOf("Timestamp") + 1, sheet, workbook);
    }

    private void writeToExcel(List<Object[]> rowDataList, int durationIndex, Sheet sheet, Workbook workbook) {
        rowDataList.sort((r1, r2) -> {
            double d1 = "+Inf".equals(r1[durationIndex]) ? Double.MAX_VALUE : Double.parseDouble(r1[durationIndex].toString());
            double d2 = "+Inf".equals(r2[durationIndex]) ? Double.MAX_VALUE : Double.parseDouble(r2[durationIndex].toString());
            return Double.compare(d2, d1);
        });

        int rowIndex = 1;
        for (Object[] rowData : rowDataList) {
            Row row = sheet.createRow(rowIndex++);
            for (int i = 0; i < rowData.length; i++) {
                Cell cell = row.createCell(i);

                if (rowData[i] instanceof String) {
                    cell.setCellValue(rowData[i].toString());
                } else if (rowData[i] instanceof Number) {
                    cell.setCellValue(((Number) rowData[i]).doubleValue());
                } else {
                    cell.setCellValue(rowData[i].toString());
                }

                double latencyValue = "+Inf".equals(rowData[durationIndex]) ? Double.MAX_VALUE : Double.parseDouble(rowData[durationIndex].toString());
                cell.setCellStyle(createCellStyle(workbook, latencyValue));
            }
        }

        if (!rowDataList.isEmpty()) {
            for (int i = 0; i < rowDataList.get(0).length; i++) {
                sheet.autoSizeColumn(i);
            }
        }

    }

    public static Sheet createSheetWithHttpRequestLatencyHeaders(Workbook workbook, String sheetName) {
        return createSheetWithHeaders(workbook, sheetName, HTTP_REQUEST_LATENCY_HEADERS);
    }

    private static Sheet createSheetWithRepositoryRequestLatencyHeaders(Workbook workbook, String sheetName) {
        return createSheetWithHeaders(workbook, sheetName, REPOSITORY_REQUEST_LATENCY_HEADERS);
    }

    private static Sheet createSheetWithHeaders(Workbook workbook, String sheetName, List<String> headers) {
        Sheet sheet = workbook.createSheet(sheetName);
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = createHeaderCellStyle(workbook);

        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(headerStyle);
        }

        sheet.createFreezePane(0, 1);
        return sheet;
    }

    private static CellStyle createHeaderCellStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return headerStyle;
    }

    private static List<Object[]> parseHttpRequestLatencyJsonResponse(String response) throws IOException {
        List<Object[]> rowDataList = new ArrayList<>();
        JsonNode resultArray = objectMapper.readTree(response).path("data").path("result");

        for (JsonNode result : resultArray) {
            for (JsonNode value : result.path("values")) {
                String durationString = value.path(1).asText();
                if (!"NaN".equals(durationString)) {
                    long timestamp = value.path(0).asLong();
                    LocalDateTime dateTime = LocalDateTime.ofEpochSecond(timestamp, 0, ZoneId.of("UTC").getRules().getOffset(Instant.now()));

                    rowDataList.add(new Object[]{
                            result.path("metric").path("instance").asText(),
                            result.path("metric").path("method").asText(),
                            result.path("metric").path("status").asText(),
                            result.path("metric").path("outcome").asText(),
                            result.path("metric").path("exception").asText(),
                            result.path("metric").path("uri").asText(),
                            dateTime.format(dateTimeFormatter),
                            Double.parseDouble(durationString)
                    });
                }
            }
        }
        return rowDataList;
    }

    private List<Object[]> parseRepositoryRequestLatencyJsonResponse(String response) throws JsonProcessingException {
        List<Object[]> rowDataList = new ArrayList<>();
        JsonNode resultArray = objectMapper.readTree(response).path("data").path("result");

        for (JsonNode result : resultArray) {
            for (JsonNode value : result.path("values")) {
                String durationString = value.path(1).asText();
                if (!"NaN".equals(durationString)) {
                    long timestamp = value.path(0).asLong();
                    LocalDateTime dateTime = LocalDateTime.ofEpochSecond(timestamp, 0, ZoneId.of("UTC").getRules().getOffset(Instant.now()));

                    rowDataList.add(new Object[]{
                            result.path("metric").path("instance").asText(),
                            result.path("metric").path("repository").asText(),
                            result.path("metric").path("method").asText(),
                            result.path("metric").path("state").asText(),
                            result.path("metric").path("exception").asText(),
                            dateTime.format(dateTimeFormatter),
                            durationString
                    });
                }
            }
        }
        return rowDataList;
    }

    private static CellStyle createCellStyle(Workbook workbook, double duration) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(duration < 1 ? IndexedColors.LIGHT_GREEN.getIndex() : IndexedColors.RED1.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    public void saveWorkbook(Workbook workbook, String fileName) throws IOException {

        String filePath = "/tmp/" + fileName;

        try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
            workbook.write(outputStream);
        }
        workbook.close();
    }
}
