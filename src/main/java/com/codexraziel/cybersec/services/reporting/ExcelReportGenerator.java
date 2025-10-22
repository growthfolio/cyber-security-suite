package com.codexraziel.cybersec.services.reporting;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Professional Excel report generator using Apache POI
 * Creates formatted reports with charts and styling
 */
@Slf4j
@Service
public class ExcelReportGenerator {
    
    private static final DateTimeFormatter DATE_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    
    /**
     * Generate WiFi scan report
     */
    public void generateWiFiScanReport(WiFiScanData data, Path outputPath) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            
            // Create styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            
            // Summary sheet
            XSSFSheet summarySheet = workbook.createSheet("Summary");
            createSummarySheet(summarySheet, data, titleStyle, headerStyle, dataStyle);
            
            // Networks sheet
            XSSFSheet networksSheet = workbook.createSheet("Networks");
            createNetworksSheet(networksSheet, data.getNetworks(), headerStyle, dataStyle);
            
            // Clients sheet (if available)
            if (data.getClients() != null && !data.getClients().isEmpty()) {
                XSSFSheet clientsSheet = workbook.createSheet("Clients");
                createClientsSheet(clientsSheet, data.getClients(), headerStyle, dataStyle);
            }
            
            // Write to file
            try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
                workbook.write(fos);
                log.info("WiFi scan report generated: {}", outputPath);
            }
            
        } catch (Exception e) {
            log.error("Failed to generate WiFi scan report", e);
            throw new RuntimeException("Report generation failed", e);
        }
    }
    
    /**
     * Generate bruteforce attack report
     */
    public void generateBruteForceReport(BruteForceData data, Path outputPath) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            
            // Summary sheet
            XSSFSheet summarySheet = workbook.createSheet("Summary");
            createBruteForceeSummary(summarySheet, data, titleStyle, headerStyle, dataStyle);
            
            // Results sheet
            XSSFSheet resultsSheet = workbook.createSheet("Results");
            createBruteForceResults(resultsSheet, data, headerStyle, dataStyle);
            
            // Write to file
            try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
                workbook.write(fos);
                log.info("Bruteforce report generated: {}", outputPath);
            }
            
        } catch (Exception e) {
            log.error("Failed to generate bruteforce report", e);
            throw new RuntimeException("Report generation failed", e);
        }
    }
    
    /**
     * Generate keylogger activity report
     */
    public void generateKeyloggerReport(KeyloggerData data, Path outputPath) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            
            // Summary sheet
            XSSFSheet summarySheet = workbook.createSheet("Summary");
            createKeyloggerSummary(summarySheet, data, titleStyle, headerStyle, dataStyle);
            
            // Events sheet
            XSSFSheet eventsSheet = workbook.createSheet("Events");
            createKeyloggerEvents(eventsSheet, data.getEvents(), headerStyle, dataStyle);
            
            // Write to file
            try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
                workbook.write(fos);
                log.info("Keylogger report generated: {}", outputPath);
            }
            
        } catch (Exception e) {
            log.error("Failed to generate keylogger report", e);
            throw new RuntimeException("Report generation failed", e);
        }
    }
    
    // Summary sheets
    
    private void createSummarySheet(XSSFSheet sheet, WiFiScanData data, 
                                     CellStyle titleStyle, CellStyle headerStyle, CellStyle dataStyle) {
        int rowNum = 0;
        
        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("WiFi Scan Report");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
        
        rowNum++; // Empty row
        
        // Info section
        createInfoRow(sheet, rowNum++, "Scan Date:", formatInstant(data.getScanDate()), headerStyle, dataStyle);
        createInfoRow(sheet, rowNum++, "Interface:", data.getInterfaceName(), headerStyle, dataStyle);
        createInfoRow(sheet, rowNum++, "Duration:", data.getDuration() + " seconds", headerStyle, dataStyle);
        createInfoRow(sheet, rowNum++, "Networks Found:", String.valueOf(data.getNetworks().size()), headerStyle, dataStyle);
        
        // Auto-size columns
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createNetworksSheet(XSSFSheet sheet, List<NetworkInfo> networks, 
                                      CellStyle headerStyle, CellStyle dataStyle) {
        int rowNum = 0;
        
        // Header
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"SSID", "BSSID", "Channel", "Signal (dBm)", "Encryption", "Clients"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Data
        for (NetworkInfo network : networks) {
            Row row = sheet.createRow(rowNum++);
            
            createDataCell(row, 0, network.getSsid(), dataStyle);
            createDataCell(row, 1, network.getBssid(), dataStyle);
            createDataCell(row, 2, String.valueOf(network.getChannel()), dataStyle);
            createDataCell(row, 3, String.valueOf(network.getSignalStrength()), dataStyle);
            createDataCell(row, 4, network.getEncryption(), dataStyle);
            createDataCell(row, 5, String.valueOf(network.getClientCount()), dataStyle);
        }
        
        // Auto-size
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createClientsSheet(XSSFSheet sheet, List<ClientInfo> clients, 
                                     CellStyle headerStyle, CellStyle dataStyle) {
        int rowNum = 0;
        
        // Header
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"MAC Address", "Connected To", "Signal (dBm)", "Packets", "First Seen"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Data
        for (ClientInfo client : clients) {
            Row row = sheet.createRow(rowNum++);
            
            createDataCell(row, 0, client.getMacAddress(), dataStyle);
            createDataCell(row, 1, client.getConnectedBssid(), dataStyle);
            createDataCell(row, 2, String.valueOf(client.getSignalStrength()), dataStyle);
            createDataCell(row, 3, String.valueOf(client.getPacketCount()), dataStyle);
            createDataCell(row, 4, formatInstant(client.getFirstSeen()), dataStyle);
        }
        
        // Auto-size
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createBruteForceeSummary(XSSFSheet sheet, BruteForceData data, 
                                          CellStyle titleStyle, CellStyle headerStyle, CellStyle dataStyle) {
        int rowNum = 0;
        
        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Bruteforce Attack Report");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
        
        rowNum++; // Empty row
        
        // Info
        createInfoRow(sheet, rowNum++, "Attack Date:", formatInstant(data.getAttackDate()), headerStyle, dataStyle);
        createInfoRow(sheet, rowNum++, "Target:", data.getTarget(), headerStyle, dataStyle);
        createInfoRow(sheet, rowNum++, "Protocol:", data.getProtocol(), headerStyle, dataStyle);
        createInfoRow(sheet, rowNum++, "Duration:", data.getDuration() + " seconds", headerStyle, dataStyle);
        createInfoRow(sheet, rowNum++, "Attempts:", String.valueOf(data.getTotalAttempts()), headerStyle, dataStyle);
        createInfoRow(sheet, rowNum++, "Success Count:", String.valueOf(data.getSuccessCount()), headerStyle, dataStyle);
        
        // Auto-size
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createBruteForceResults(XSSFSheet sheet, BruteForceData data, 
                                          CellStyle headerStyle, CellStyle dataStyle) {
        int rowNum = 0;
        
        // Header
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Username", "Password", "Status", "Timestamp"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Data
        for (CredentialResult result : data.getResults()) {
            Row row = sheet.createRow(rowNum++);
            
            createDataCell(row, 0, result.getUsername(), dataStyle);
            createDataCell(row, 1, result.getPassword(), dataStyle);
            createDataCell(row, 2, result.isSuccess() ? "SUCCESS" : "FAILED", dataStyle);
            createDataCell(row, 3, formatInstant(result.getTimestamp()), dataStyle);
        }
        
        // Auto-size
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createKeyloggerSummary(XSSFSheet sheet, KeyloggerData data, 
                                         CellStyle titleStyle, CellStyle headerStyle, CellStyle dataStyle) {
        int rowNum = 0;
        
        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Keylogger Activity Report");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
        
        rowNum++; // Empty row
        
        // Info
        createInfoRow(sheet, rowNum++, "Capture Date:", formatInstant(data.getCaptureDate()), headerStyle, dataStyle);
        createInfoRow(sheet, rowNum++, "Duration:", data.getDuration() + " seconds", headerStyle, dataStyle);
        createInfoRow(sheet, rowNum++, "Total Events:", String.valueOf(data.getTotalEvents()), headerStyle, dataStyle);
        createInfoRow(sheet, rowNum++, "Key Events:", String.valueOf(data.getKeyEventCount()), headerStyle, dataStyle);
        createInfoRow(sheet, rowNum++, "Mouse Events:", String.valueOf(data.getMouseEventCount()), headerStyle, dataStyle);
        
        // Auto-size
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createKeyloggerEvents(XSSFSheet sheet, List<KeyloggerEvent> events, 
                                        CellStyle headerStyle, CellStyle dataStyle) {
        int rowNum = 0;
        
        // Header
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Timestamp", "Type", "Key/Button", "Modifiers"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Data (limit to avoid huge files)
        int maxEvents = Math.min(events.size(), 10000);
        for (int i = 0; i < maxEvents; i++) {
            KeyloggerEvent event = events.get(i);
            Row row = sheet.createRow(rowNum++);
            
            createDataCell(row, 0, formatInstant(event.getTimestamp()), dataStyle);
            createDataCell(row, 1, event.getType(), dataStyle);
            createDataCell(row, 2, event.getKey(), dataStyle);
            createDataCell(row, 3, event.getModifiers(), dataStyle);
        }
        
        // Auto-size
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    // Styling
    
    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
    
    private CellStyle createDataStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
    
    private CellStyle createTitleStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
    
    // Helpers
    
    private void createInfoRow(XSSFSheet sheet, int rowNum, String label, String value, 
                                CellStyle labelStyle, CellStyle valueStyle) {
        Row row = sheet.createRow(rowNum);
        
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);
        
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        valueCell.setCellStyle(valueStyle);
    }
    
    private void createDataCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }
    
    private String formatInstant(Instant instant) {
        if (instant == null) return "N/A";
        return DATE_FORMATTER.format(instant);
    }
    
    // Data models
    
    @Data
    public static class WiFiScanData {
        private Instant scanDate;
        private String interfaceName;
        private long duration;
        private List<NetworkInfo> networks;
        private List<ClientInfo> clients;
    }
    
    @Data
    public static class NetworkInfo {
        private String ssid;
        private String bssid;
        private int channel;
        private int signalStrength;
        private String encryption;
        private int clientCount;
    }
    
    @Data
    public static class ClientInfo {
        private String macAddress;
        private String connectedBssid;
        private int signalStrength;
        private long packetCount;
        private Instant firstSeen;
    }
    
    @Data
    public static class BruteForceData {
        private Instant attackDate;
        private String target;
        private String protocol;
        private long duration;
        private long totalAttempts;
        private int successCount;
        private List<CredentialResult> results;
    }
    
    @Data
    public static class CredentialResult {
        private String username;
        private String password;
        private boolean success;
        private Instant timestamp;
    }
    
    @Data
    public static class KeyloggerData {
        private Instant captureDate;
        private long duration;
        private long totalEvents;
        private long keyEventCount;
        private long mouseEventCount;
        private List<KeyloggerEvent> events;
    }
    
    @Data
    public static class KeyloggerEvent {
        private Instant timestamp;
        private String type;
        private String key;
        private String modifiers;
    }
}
