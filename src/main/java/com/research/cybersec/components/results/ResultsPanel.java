package com.research.cybersec.components.results;

import com.research.cybersec.components.base.CyberSecComponent;
import com.research.cybersec.models.AttackResult;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
@Scope("prototype")
public class ResultsPanel extends CyberSecComponent<VBox> {
    
    @FXML private TableView<AttackResult> resultsTable;
    @FXML private LineChart<String, Number> successChart;
    @FXML private PieChart protocolChart;
    @FXML private Label totalLabel;
    @FXML private Label successLabel;
    @FXML private Label failureLabel;
    @FXML private Button exportBtn;
    @FXML private Button clearBtn;
    
    private final ObservableList<AttackResult> results = FXCollections.observableArrayList();
    private final XYChart.Series<String, Number> successSeries = new XYChart.Series<>();
    
    @Override
    protected void onComponentLoaded() {
        setupTable();
        setupCharts();
        resultsTable.setItems(results);
    }
    
    private void setupTable() {
        TableColumn<AttackResult, String> timestampCol = new TableColumn<>("Time");
        timestampCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().getTimestamp() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                    cellData.getValue().getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                );
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });
        timestampCol.setPrefWidth(80);
        
        TableColumn<AttackResult, String> targetCol = new TableColumn<>("Target");
        targetCol.setCellValueFactory(new PropertyValueFactory<>("target"));
        targetCol.setPrefWidth(150);
        
        TableColumn<AttackResult, String> protocolCol = new TableColumn<>("Protocol");
        protocolCol.setCellValueFactory(new PropertyValueFactory<>("protocol"));
        protocolCol.setPrefWidth(80);
        
        TableColumn<AttackResult, String> credentialsCol = new TableColumn<>("Credentials");
        credentialsCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getUsername() + ":" + cellData.getValue().getPassword()
            )
        );
        credentialsCol.setPrefWidth(200);
        
        TableColumn<AttackResult, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(this::createStatusCell);
        statusCol.setPrefWidth(100);
        
        resultsTable.getColumns().addAll(timestampCol, targetCol, protocolCol, credentialsCol, statusCol);
    }
    
    private TableCell<AttackResult, String> createStatusCell(TableColumn<AttackResult, String> column) {
        return new TableCell<AttackResult, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if ("SUCCESS".equals(status)) {
                        setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
                    }
                }
            }
        };
    }
    
    private void setupCharts() {
        successSeries.setName("Success Rate %");
        successChart.getData().add(successSeries);
        successChart.setTitle("Attack Success Rate Over Time");
        successChart.setCreateSymbols(false);
        
        protocolChart.setTitle("Results by Protocol");
    }
    
    public void addResult(AttackResult result) {
        Platform.runLater(() -> {
            results.add(result);
            updateStatistics();
            updateCharts();
        });
    }
    
    private void updateStatistics() {
        long total = results.size();
        long success = results.stream().filter(r -> "SUCCESS".equals(r.getStatus())).count();
        long failure = total - success;
        
        totalLabel.setText("Total: " + total);
        successLabel.setText("Success: " + success);
        failureLabel.setText("Failed: " + failure);
    }
    
    private void updateCharts() {
        // Update success rate chart every 10 results
        if (results.size() % 10 == 0 && !results.isEmpty()) {
            long success = results.stream().filter(r -> "SUCCESS".equals(r.getStatus())).count();
            double rate = (double) success / results.size() * 100;
            
            String timeLabel = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            successSeries.getData().add(new XYChart.Data<>(timeLabel, rate));
            
            // Keep only last 20 points
            if (successSeries.getData().size() > 20) {
                successSeries.getData().remove(0);
            }
        }
        
        // Update protocol chart
        updateProtocolChart();
    }
    
    private void updateProtocolChart() {
        protocolChart.getData().clear();
        
        results.stream()
            .filter(r -> "SUCCESS".equals(r.getStatus()))
            .collect(java.util.stream.Collectors.groupingBy(
                AttackResult::getProtocol,
                java.util.stream.Collectors.counting()
            ))
            .forEach((protocol, count) -> 
                protocolChart.getData().add(new PieChart.Data(protocol, count))
            );
    }
    
    @FXML
    private void exportResults() {
        // TODO: Implement export functionality
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export");
        alert.setHeaderText("Export Results");
        alert.setContentText("Export functionality will be implemented soon.");
        alert.showAndWait();
    }
    
    @FXML
    private void clearResults() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear Results");
        alert.setHeaderText("Clear all results?");
        alert.setContentText("This action cannot be undone.");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            results.clear();
            successSeries.getData().clear();
            protocolChart.getData().clear();
            updateStatistics();
        }
    }
}