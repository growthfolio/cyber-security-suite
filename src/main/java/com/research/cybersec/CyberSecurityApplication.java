package com.research.cybersec;

import com.research.cybersec.components.dashboard.DashboardView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@Slf4j
public class CyberSecurityApplication extends Application {
    private ConfigurableApplicationContext context;
    
    @Override
    public void init() {
        context = SpringApplication.run(CyberSecurityApplication.class);
    }
    
    @Override
    public void start(Stage stage) {
        DashboardView dashboard = context.getBean(DashboardView.class);
        
        Scene scene = new Scene(dashboard, 1400, 900);
        scene.getStylesheets().add(getClass().getResource("/css/cybersec.css").toExternalForm());
        
        stage.setTitle("🔬 Cyber Security Research Suite");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();
        
        log.info("Cyber Security Research Suite started successfully");
    }
    
    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}