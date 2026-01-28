package UI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;
import javafx.scene.Node;
import javafx.stage.Stage;
import java.io.IOException;
import javafx.application.Platform;

public class MenuController {

    // --- NOMBRES DE LAS VENTANAS (Tienen que coincidir con el título que pones al navegar) ---
    private static final String TITLE_MOVEMENTS = "Movements";
    private static final String TITLE_ACCOUNTS = "Accounts Management";
    private static final String TITLE_SIGNIN = "Sign In";

    // --- FILE ---

    @FXML
    private void handleExit(ActionEvent event) {
        // Ejemplo: Si quieres que Exit cierre la app por completo
        Platform.exit();
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        // Siempre permitimos ir al login
        navigate(event, "FXMLDocument.fxml", TITLE_SIGNIN);
    }

    // --- ACTIONS ---

    @FXML
    private void handleAccounts(ActionEvent event) {
        Stage stage = getCurrentStage(event);
        
        // CORRECCIÓN: Si ya estamos en Accounts, no hacer nada
        if (stage != null && TITLE_ACCOUNTS.equals(stage.getTitle())) {
            return; 
        }

        navigate(event, "FXMLAccounts.fxml", TITLE_ACCOUNTS);
    }

    @FXML
    private void handleMovements(ActionEvent event) {
        Stage stage = getCurrentStage(event);

        // CORRECCIÓN CLAVE: Si ya estamos en Movements, no hacer nada (no recargar)
        if (stage != null && TITLE_MOVEMENTS.equals(stage.getTitle())) {
            return;
        }

        // ADVERTENCIA: Si navegas desde "Accounts" a "Movements" por el menú, 
        // la tabla saldrá vacía porque el menú no sabe qué cliente pasar.
        // Para solucionar eso necesitarías una "Sesión" global, pero esto 
        // arregla el error de borrar datos si ya estás dentro.
        navigate(event, "FXMLDocumentMyMovements.fxml", TITLE_MOVEMENTS);
    }

    // --- HELP ---

    @FXML
    private void handleAbout(ActionEvent event) {
        showAlert("About", "Bank App v2.0", "Desarrollado por el equipo de desarrollo.");
    }

    @FXML
    private void handleHelp(ActionEvent event) {
        showAlert("Help", "Ayuda", "Contacte con el administrador del sistema.");
    }

    // --- MÉTODOS AUXILIARES ---

    /**
     * Navega a una nueva vista FXML.
     */
    private void navigate(ActionEvent event, String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();

            Stage stage = getCurrentStage(event);
            if (stage == null) return;

            Scene scene = new Scene(root);
            stage.setTitle(title); // IMPORTANTE: Fijamos el título para poder comprobarlo después
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Error de Navegación", "No se pudo cargar: " + fxmlFile);
        }
    }

    /**
     * Obtiene el Stage (Ventana) desde un evento de menú o botón.
     * Es necesario porque los MenuItems no son nodos gráficos normales.
     */
    private Stage getCurrentStage(ActionEvent event) {
        Object source = event.getSource();
        
        if (source instanceof MenuItem) {
            // Truco para obtener el stage desde un ítem de menú desplegable
            return (Stage) ((MenuItem) source).getParentPopup().getOwnerWindow();
        } else if (source instanceof Node) {
            // Forma normal para botones
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