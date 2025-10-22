package com.codexraziel.cybersec.services.reporting;

import com.codexraziel.cybersec.services.reporting.ExcelReportGenerator.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Professional PDF report generator using Apache PDFBox
 * Creates formatted reports with tables and styling
 */
@Slf4j
@Service
public class PDFReportGenerator {
    
    private static final DateTimeFormatter DATE_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    
    private static final Color HEADER_COLOR = new Color(0, 51, 102);
    private static final Color ALT_ROW_COLOR = new Color(240, 240, 240);
    
    /**
     * Generate WiFi scan report
     */
    public void generateWiFiScanReport(WiFiScanData data, Path outputPath) {
        try (PDDocument document = new PDDocument()) {
            
            // Summary page
            PDPage summaryPage = new PDPage(PDRectangle.A4);
            document.addPage(summaryPage);
            createWiFiSummaryPage(document, summaryPage, data);
            
            // Networks pages
            createNetworksPages(document, data.getNetworks());
            
            // Save
            document.save(outputPath.toFile());
            log.info("WiFi scan PDF report generated: {}", outputPath);
            
        } catch (Exception e) {
            log.error("Failed to generate WiFi scan PDF report", e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }
    
    /**
     * Generate bruteforce attack report
     */
    public void generateBruteForceReport(BruteForceData data, Path outputPath) {
        try (PDDocument document = new PDDocument()) {
            
            // Summary page
            PDPage summaryPage = new PDPage(PDRectangle.A4);
            document.addPage(summaryPage);
            createBruteForceSummaryPage(document, summaryPage, data);
            
            // Results pages
            createBruteForceResultsPages(document, data);
            
            // Save
            document.save(outputPath.toFile());
            log.info("Bruteforce PDF report generated: {}", outputPath);
            
        } catch (Exception e) {
            log.error("Failed to generate bruteforce PDF report", e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }
    
    /**
     * Generate keylogger activity report
     */
    public void generateKeyloggerReport(KeyloggerData data, Path outputPath) {
        try (PDDocument document = new PDDocument()) {
            
            // Summary page
            PDPage summaryPage = new PDPage(PDRectangle.A4);
            document.addPage(summaryPage);
            createKeyloggerSummaryPage(document, summaryPage, data);
            
            // Events pages (limited)
            createKeyloggerEventsPages(document, data);
            
            // Save
            document.save(outputPath.toFile());
            log.info("Keylogger PDF report generated: {}", outputPath);
            
        } catch (Exception e) {
            log.error("Failed to generate keylogger PDF report", e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }
    
    // WiFi Report Pages
    
    private void createWiFiSummaryPage(PDDocument document, PDPage page, WiFiScanData data) throws IOException {
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            
            float yPosition = page.getMediaBox().getHeight() - 50;
            
            // Title
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, 20);
            content.setNonStrokingColor(HEADER_COLOR);
            content.newLineAtOffset(50, yPosition);
            content.showText("WiFi Scan Report");
            content.endText();
            
            yPosition -= 40;
            
            // Horizontal line
            content.setStrokingColor(HEADER_COLOR);
            content.setLineWidth(2);
            content.moveTo(50, yPosition);
            content.lineTo(page.getMediaBox().getWidth() - 50, yPosition);
            content.stroke();
            
            yPosition -= 30;
            
            // Info section
            content.setNonStrokingColor(Color.BLACK);
            PDFont font = PDType1Font.HELVETICA;
            PDFont boldFont = PDType1Font.HELVETICA_BOLD;
            
            yPosition = addInfoLine(content, "Scan Date:", formatInstant(data.getScanDate()), 
                    50, yPosition, boldFont, font);
            yPosition = addInfoLine(content, "Interface:", data.getInterfaceName(), 
                    50, yPosition, boldFont, font);
            yPosition = addInfoLine(content, "Duration:", data.getDuration() + " seconds", 
                    50, yPosition, boldFont, font);
            yPosition = addInfoLine(content, "Networks Found:", String.valueOf(data.getNetworks().size()), 
                    50, yPosition, boldFont, font);
            
            if (data.getClients() != null) {
                yPosition = addInfoLine(content, "Clients Found:", String.valueOf(data.getClients().size()), 
                        50, yPosition, boldFont, font);
            }
            
            // Footer
            addFooter(content, page, 1);
        }
    }
    
    private void createNetworksPages(PDDocument document, List<NetworkInfo> networks) throws IOException {
        int pageNum = 2;
        int networksPerPage = 15;
        
        for (int i = 0; i < networks.size(); i += networksPerPage) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            List<NetworkInfo> pageNetworks = networks.subList(i, 
                    Math.min(i + networksPerPage, networks.size()));
            
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                createNetworksTable(content, page, pageNetworks, pageNum++);
            }
        }
    }
    
    private void createNetworksTable(PDPageContentStream content, PDPage page, 
                                      List<NetworkInfo> networks, int pageNum) throws IOException {
        
        float yPosition = page.getMediaBox().getHeight() - 50;
        float margin = 50;
        float tableWidth = page.getMediaBox().getWidth() - 2 * margin;
        
        PDFont font = PDType1Font.HELVETICA;
        PDFont boldFont = PDType1Font.HELVETICA_BOLD;
        
        // Title
        content.beginText();
        content.setFont(boldFont, 16);
        content.setNonStrokingColor(HEADER_COLOR);
        content.newLineAtOffset(margin, yPosition);
        content.showText("Detected Networks");
        content.endText();
        
        yPosition -= 30;
        
        // Table header
        String[] headers = {"SSID", "BSSID", "Ch", "Signal", "Encryption"};
        float[] colWidths = {150, 120, 30, 50, 100};
        
        // Header row
        content.setNonStrokingColor(HEADER_COLOR);
        content.addRect(margin, yPosition - 15, tableWidth, 20);
        content.fill();
        
        content.beginText();
        content.setFont(boldFont, 10);
        content.setNonStrokingColor(Color.WHITE);
        float xPos = margin + 5;
        content.newLineAtOffset(xPos, yPosition - 10);
        
        for (int i = 0; i < headers.length; i++) {
            content.showText(headers[i]);
            content.newLineAtOffset(colWidths[i], 0);
        }
        content.endText();
        
        yPosition -= 20;
        
        // Data rows
        content.setFont(font, 9);
        boolean alternate = false;
        
        for (NetworkInfo network : networks) {
            // Alternate row background
            if (alternate) {
                content.setNonStrokingColor(ALT_ROW_COLOR);
                content.addRect(margin, yPosition - 15, tableWidth, 18);
                content.fill();
            }
            
            content.setNonStrokingColor(Color.BLACK);
            content.beginText();
            xPos = margin + 5;
            content.newLineAtOffset(xPos, yPosition - 10);
            
            // Truncate long SSIDs
            String ssid = network.getSsid();
            if (ssid.length() > 20) ssid = ssid.substring(0, 17) + "...";
            
            content.showText(ssid);
            content.newLineAtOffset(colWidths[0], 0);
            content.showText(network.getBssid());
            content.newLineAtOffset(colWidths[1], 0);
            content.showText(String.valueOf(network.getChannel()));
            content.newLineAtOffset(colWidths[2], 0);
            content.showText(network.getSignalStrength() + " dBm");
            content.newLineAtOffset(colWidths[3], 0);
            content.showText(network.getEncryption());
            content.endText();
            
            yPosition -= 18;
            alternate = !alternate;
            
            if (yPosition < 100) break; // Page full
        }
        
        // Footer
        addFooter(content, page, pageNum);
    }
    
    // Bruteforce Report Pages
    
    private void createBruteForceSummaryPage(PDDocument document, PDPage page, 
                                              BruteForceData data) throws IOException {
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            
            float yPosition = page.getMediaBox().getHeight() - 50;
            
            // Title
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, 20);
            content.setNonStrokingColor(HEADER_COLOR);
            content.newLineAtOffset(50, yPosition);
            content.showText("Bruteforce Attack Report");
            content.endText();
            
            yPosition -= 40;
            
            // Horizontal line
            content.setStrokingColor(HEADER_COLOR);
            content.setLineWidth(2);
            content.moveTo(50, yPosition);
            content.lineTo(page.getMediaBox().getWidth() - 50, yPosition);
            content.stroke();
            
            yPosition -= 30;
            
            // Info section
            content.setNonStrokingColor(Color.BLACK);
            PDFont font = PDType1Font.HELVETICA;
            PDFont boldFont = PDType1Font.HELVETICA_BOLD;
            
            yPosition = addInfoLine(content, "Attack Date:", formatInstant(data.getAttackDate()), 
                    50, yPosition, boldFont, font);
            yPosition = addInfoLine(content, "Target:", data.getTarget(), 
                    50, yPosition, boldFont, font);
            yPosition = addInfoLine(content, "Protocol:", data.getProtocol(), 
                    50, yPosition, boldFont, font);
            yPosition = addInfoLine(content, "Duration:", data.getDuration() + " seconds", 
                    50, yPosition, boldFont, font);
            yPosition = addInfoLine(content, "Total Attempts:", String.valueOf(data.getTotalAttempts()), 
                    50, yPosition, boldFont, font);
            yPosition = addInfoLine(content, "Successful:", String.valueOf(data.getSuccessCount()), 
                    50, yPosition, boldFont, font);
            
            // Success rate
            double successRate = (data.getTotalAttempts() > 0) ? 
                    (data.getSuccessCount() * 100.0 / data.getTotalAttempts()) : 0;
            yPosition = addInfoLine(content, "Success Rate:", String.format("%.2f%%", successRate), 
                    50, yPosition, boldFont, font);
            
            addFooter(content, page, 1);
        }
    }
    
    private void createBruteForceResultsPages(PDDocument document, BruteForceData data) throws IOException {
        // Only show successful attempts
        List<CredentialResult> successfulResults = data.getResults().stream()
                .filter(CredentialResult::isSuccess)
                .toList();
        
        if (successfulResults.isEmpty()) return;
        
        int pageNum = 2;
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            float yPosition = page.getMediaBox().getHeight() - 50;
            PDFont boldFont = PDType1Font.HELVETICA_BOLD;
            PDFont font = PDType1Font.HELVETICA;
            
            // Title
            content.beginText();
            content.setFont(boldFont, 16);
            content.setNonStrokingColor(HEADER_COLOR);
            content.newLineAtOffset(50, yPosition);
            content.showText("Successful Credentials");
            content.endText();
            
            yPosition -= 30;
            
            // Results
            for (CredentialResult result : successfulResults) {
                content.beginText();
                content.setFont(font, 11);
                content.setNonStrokingColor(Color.BLACK);
                content.newLineAtOffset(50, yPosition);
                content.showText(String.format("✓ %s:%s", result.getUsername(), result.getPassword()));
                content.endText();
                
                yPosition -= 20;
                
                if (yPosition < 100) break;
            }
            
            addFooter(content, page, pageNum);
        }
    }
    
    // Keylogger Report Pages
    
    private void createKeyloggerSummaryPage(PDDocument document, PDPage page, 
                                             KeyloggerData data) throws IOException {
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            
            float yPosition = page.getMediaBox().getHeight() - 50;
            
            // Title
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, 20);
            content.setNonStrokingColor(HEADER_COLOR);
            content.newLineAtOffset(50, yPosition);
            content.showText("Keylogger Activity Report");
            content.endText();
            
            yPosition -= 40;
            
            // Horizontal line
            content.setStrokingColor(HEADER_COLOR);
            content.setLineWidth(2);
            content.moveTo(50, yPosition);
            content.lineTo(page.getMediaBox().getWidth() - 50, yPosition);
            content.stroke();
            
            yPosition -= 30;
            
            // Info section
            content.setNonStrokingColor(Color.BLACK);
            PDFont font = PDType1Font.HELVETICA;
            PDFont boldFont = PDType1Font.HELVETICA_BOLD;
            
            yPosition = addInfoLine(content, "Capture Date:", formatInstant(data.getCaptureDate()), 
                    50, yPosition, boldFont, font);
            yPosition = addInfoLine(content, "Duration:", data.getDuration() + " seconds", 
                    50, yPosition, boldFont, font);
            yPosition = addInfoLine(content, "Total Events:", String.valueOf(data.getTotalEvents()), 
                    50, yPosition, boldFont, font);
            yPosition = addInfoLine(content, "Key Events:", String.valueOf(data.getKeyEventCount()), 
                    50, yPosition, boldFont, font);
            yPosition = addInfoLine(content, "Mouse Events:", String.valueOf(data.getMouseEventCount()), 
                    50, yPosition, boldFont, font);
            
            addFooter(content, page, 1);
        }
    }
    
    private void createKeyloggerEventsPages(PDDocument document, KeyloggerData data) throws IOException {
        // Limit events to avoid huge PDFs
        int maxEvents = Math.min(data.getEvents().size(), 1000);
        List<KeyloggerEvent> limitedEvents = data.getEvents().subList(0, maxEvents);
        
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            float yPosition = page.getMediaBox().getHeight() - 50;
            PDFont font = PDType1Font.HELVETICA;
            PDFont boldFont = PDType1Font.HELVETICA_BOLD;
            
            // Title
            content.beginText();
            content.setFont(boldFont, 16);
            content.setNonStrokingColor(HEADER_COLOR);
            content.newLineAtOffset(50, yPosition);
            content.showText("Captured Events (First " + maxEvents + ")");
            content.endText();
            
            yPosition -= 30;
            
            // Events (compact format)
            content.setFont(PDType1Font.COURIER, 8);
            content.setNonStrokingColor(Color.BLACK);
            
            for (KeyloggerEvent event : limitedEvents) {
                content.beginText();
                content.newLineAtOffset(50, yPosition);
                content.showText(String.format("%s [%s] %s %s", 
                        formatInstant(event.getTimestamp()),
                        event.getType(),
                        event.getKey(),
                        event.getModifiers() != null ? event.getModifiers() : ""));
                content.endText();
                
                yPosition -= 12;
                
                if (yPosition < 100) break;
            }
            
            addFooter(content, page, 2);
        }
    }
    
    // Helper methods
    
    private float addInfoLine(PDPageContentStream content, String label, String value, 
                               float x, float y, PDFont labelFont, PDFont valueFont) throws IOException {
        content.beginText();
        content.setFont(labelFont, 12);
        content.newLineAtOffset(x, y);
        content.showText(label);
        content.endText();
        
        content.beginText();
        content.setFont(valueFont, 12);
        content.newLineAtOffset(x + 150, y);
        content.showText(value);
        content.endText();
        
        return y - 20;
    }
    
    private void addFooter(PDPageContentStream content, PDPage page, int pageNum) throws IOException {
        content.beginText();
        content.setFont(PDType1Font.HELVETICA, 9);
        content.setNonStrokingColor(Color.GRAY);
        content.newLineAtOffset(50, 30);
        content.showText("Cyber Security Research Suite - Professional Security Analysis");
        content.endText();
        
        content.beginText();
        content.newLineAtOffset(page.getMediaBox().getWidth() - 80, 30);
        content.showText("Page " + pageNum);
        content.endText();
    }
    
    private String formatInstant(Instant instant) {
        if (instant == null) return "N/A";
        return DATE_FORMATTER.format(instant);
    }
}
