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
        try{
            // 1. Crear alerta de confirmación
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Cerrar Aplicación");
            alert.setHeaderText("Salir definitivamente");
            alert.setContentText("¿Está seguro de que desea cerrar la aplicación?");

            // 2. Esperar respuesta
            java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();

            // 3. Si el usuario dice OK, cerramos
            if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
                // Cierra todas las ventanas y detiene el hilo de JavaFX
                Platform.exit();
                System.exit(0);
            }
        }catch (java.lang.IllegalStateException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Error al cerrar la aplicacion.");
            alert.showAndWait();
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        // 1. Mostrar confirmación
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cerrar Sesión");
        alert.setHeaderText("Salir de la aplicación");
        alert.setContentText("¿Está seguro de que desea cerrar sesión y volver al login?");

        java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            
            // 1. Obtener la ventana actual (donde se pulsó el menú)
            Stage currentStage = getCurrentStage(event);
            
            // 2. Antes de cerrarla, miramos si tiene un "Padre" (Owner)
            // En Java 8, Window es la clase padre de Stage
            javafx.stage.Window parentWindow = currentStage.getOwner();
            
            // 3. Cerramos la ventana actual (Movimientos)
            currentStage.close();

            // 4. Si tenía padre y es un Stage, lo cerramos también (Cuentas)
            if (parentWindow != null && parentWindow instanceof Stage) {
                ((Stage) parentWindow).close();
            }
            
            // NOTA: Al cerrarse el padre (Cuentas), el Login que estaba debajo
            // esperando (por el showAndWait) se reactivará automáticamente
            // gracias al código que pusimos antes en GestionUsuariosController.
        }
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