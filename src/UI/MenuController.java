package UI;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;
import javafx.scene.web.WebEngine; // Importante
import javafx.scene.web.WebView;   // Importante
import javafx.stage.Stage;

import java.net.URL; // Importante

public class MenuController {

    // --- TÍTULOS DE VENTANA (Deben coincidir con tus Stage.setTitle) ---
    private static final String TITLE_MOVEMENTS = "Movements";
    private static final String TITLE_ACCOUNTS = "Accounts Management";
    private static final String TITLE_SIGNIN = "Sign In";

    // --- SALIR Y CERRAR SESIÓN ---
    @FXML
    private void handleExit(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cerrar Aplicación");
        alert.setHeaderText("Salir definitivamente");
        alert.setContentText("¿Está seguro de que desea cerrar la aplicación?");
        if (alert.showAndWait().orElse(null) == javafx.scene.control.ButtonType.OK) {
            Platform.exit();
            System.exit(0);
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cerrar Sesión");
        alert.setHeaderText("Salir de la aplicación");
        alert.setContentText("¿Está seguro de que desea cerrar sesión y volver al login?");
        
        if (alert.showAndWait().orElse(null) == javafx.scene.control.ButtonType.OK) {
            Stage currentStage = getCurrentStage(event);
            if (currentStage != null) {
                javafx.stage.Window parentWindow = currentStage.getOwner();
                currentStage.close();
                if (parentWindow != null && parentWindow instanceof Stage) {
                    ((Stage) parentWindow).close();
                }
            }
        }
    }

    @FXML
    private void handleAbout(ActionEvent event) {
        showAlert("About", "Bank App v2.0", "Desarrollado por el alumno.");
    }

    // --- IMPLEMENTACIÓN RA6: AYUDA (DIRECTA EN RESOURCES) ---

    @FXML
    private void handleHelp(ActionEvent event) {
        try {
            Stage currentStage = getCurrentStage(event);
            String currentTitle = (currentStage != null && currentStage.getTitle() != null) 
                                ? currentStage.getTitle() 
                                : "";

            String helpFile = "index.html"; // Por defecto
            
            if (currentTitle.contains(TITLE_MOVEMENTS)) {
                helpFile = "movements.html";
            } else if (currentTitle.contains(TITLE_ACCOUNTS)) {
                helpFile = "accounts.html";
            }

            showHelpWindow(helpFile);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "No se pudo cargar la ayuda", e.getMessage());
        }
    }

    private void showHelpWindow(String htmlFileName) {
        Stage helpStage = new Stage();
        helpStage.setTitle("Ayuda - Bank App");

        WebView browser = new WebView();
        WebEngine webEngine = browser.getEngine();

        // --- CAMBIO PARA TU CASO: RUTA RAÍZ ---
        // Al poner "/" al principio, busca directamente en la raíz de resources
        URL url = getClass().getResource("/" + htmlFileName);
        
        if (url != null) {
            webEngine.load(url.toExternalForm());
        } else {
            System.err.println("No encontrado: " + htmlFileName);
            webEngine.loadContent(
                "<html><body style='color:red;'><h1>Error 404</h1>" +
                "<p>No se encuentra el archivo: <b>" + htmlFileName + "</b></p>" +
                "<p>Verifica que está suelto en <code>src/resources/</code></p></body></html>"
            );
        }

        Scene scene = new Scene(browser, 800, 600);
        helpStage.setScene(scene);
        helpStage.show();
    }

    // --- AUXILIARES ---

    private Stage getCurrentStage(ActionEvent event) {
        Object source = event.getSource();
        if (source instanceof MenuItem) {
            return (Stage) ((MenuItem) source).getParentPopup().getOwnerWindow();
        } else if (source instanceof Node) {
            return (Stage) ((Node) source).getScene().getWindow();
        }
        return null;
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}