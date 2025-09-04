package com.research.cybersec.components.dashboard;

import com.research.cybersec.components.base.CyberSecComponent;
import com.research.cybersec.components.bruteforce.BruteForcePanel;
import com.research.cybersec.components.results.ResultsPanel;
import com.research.cybersec.services.ProcessManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class DashboardView extends CyberSecComponent<BorderPane> {
    
    @FXML private TabPane mainTabs;
    @FXML private Label statusLabel;
    @FXML private ProgressBar globalProgress;
    @FXML private MenuBar menuBar;
    
    @Autowired private ProcessManager processManager;
    @Autowired private ApplicationContext context;
    
    private BruteForcePanel bruteForcePanel;
    private ResultsPanel resultsPanel;
    
    @Override
    protected void onComponentLoaded() {
        initializeTabs();
        bindToServices();
    }
    
    private void initializeTabs() {
        bruteForcePanel = context.getBean(BruteForcePanel.class);
        resultsPanel = context.getBean(ResultsPanel.class);
        
        Tab bruteForceTab = new Tab("🔴 BruteForce", bruteForcePanel);
        Tab keyloggerTab = new Tab("🎯 Keylogger", new Label("Keylogger Panel - Coming Soon"));
        Tab androidTab = new Tab("🤖 Android", new Label("Android Panel - Coming Soon"));
        Tab resultsTab = new Tab("📊 Results", resultsPanel);
        
        bruteForceTab.setClosable(false);
        keyloggerTab.setClosable(false);
        androidTab.setClosable(false);
        resultsTab.setClosable(false);
        
        mainTabs.getTabs().addAll(bruteForceTab, keyloggerTab, androidTab, resultsTab);
        
        // Connect panels
        bruteForcePanel.setResultsPanel(resultsPanel);
    }
    
    private void bindToServices() {
        processManager.statusProperty().addListener((obs, old, status) -> 
            Platform.runLater(() -> statusLabel.setText(status))
        );
        
        processManager.progressProperty().addListener((obs, old, progress) ->
            Platform.runLater(() -> globalProgress.setProgress(progress.doubleValue()))
        );
    }
}