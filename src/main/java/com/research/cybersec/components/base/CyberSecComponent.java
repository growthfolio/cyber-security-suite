package com.research.cybersec.components.base;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public abstract class CyberSecComponent<T extends Parent> extends T {
    
    protected CyberSecComponent() {
        loadFXML();
        onComponentLoaded();
    }
    
    private void loadFXML() {
        String fxmlFile = "/" + this.getClass().getSimpleName() + ".fxml";
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
        loader.setRoot(this);
        loader.setController(this);
        
        try {
            loader.load();
        } catch (IOException e) {
            log.error("Failed to load FXML: {}", fxmlFile, e);
            throw new RuntimeException(e);
        }
    }
    
    protected void onComponentLoaded() {
        // Override para inicialização pós-FXML
    }
}