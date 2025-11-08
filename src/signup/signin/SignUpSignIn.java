package signup.signin;

import UI.GestionUsuariosController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Punto de entrada de la aplicación Sign-In / Login
 */
public class SignUpSignIn extends Application {

    // Logger for main application events in SignUpSignIn
        private static final Logger LOGGER = Logger.getLogger("SignUpSignIn.Main");
        
    @Override
    public void start(Stage stage) {
        try {
            // Cargar interfaz desde FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UI/FXMLDocument.fxml"));
            Parent root = loader.load();

            if (root == null) {
                LOGGER.severe("No se pudo cargar la interfaz FXML: root es null.");
                showError("No se pudo cargar la interfaz de usuario.");
                return;
            }

            // Obtener controlador y pasar Stage
            GestionUsuariosController controller = loader.getController();
            if (controller == null) {
                LOGGER.severe("No se encontró el controlador en el FXML.");
                showError("No se encontró el controlador en el archivo FXML.");
                return;
            }
            controller.init(stage, root);

            // Confirmación al cerrar
            stage.setOnCloseRequest(event -> {
                event.consume(); // prevenir cierre automático
                handleExit(stage);
            });

            LOGGER.info("Ventana LOGIN iniciada correctamente.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al iniciar Sign-In", e);
            showError("Ha ocurrido un error al iniciar la aplicación. Por favor, inténtalo de nuevo más tarde.");
        }
    }

    /**
     * Muestra una confirmación antes de cerrar la aplicación.
     */
    private void handleExit(Stage stage) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.initOwner(stage);
        alert.setTitle("Confirmar salida");
        alert.setHeaderText("¿Deseas salir de la aplicación?");
        alert.setContentText("Se cerrarán todas las ventanas.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            LOGGER.info("Aplicación finalizada por el usuario.");
            stage.close();
        }
    }

    /**
     * Muestra un error genérico.
     * Este método es bloqueante: espera hasta que el usuario cierre el diálogo.
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    /**
     * Launches the JavaFX application.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}