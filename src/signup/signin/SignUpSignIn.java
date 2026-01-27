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
 * Clase principal de la aplicación para el sistema de Registro/Inicio de Sesión (Sign Up/Sign In).
 * Esta clase maneja la inicialización de la aplicación JavaFX, incluyendo la carga de la interfaz FXML,
 * la configuración del controlador principal y la gestión de los eventos del ciclo de vida de la aplicación.
 *
 * La clase proporciona funcionalidad para:
 * - Cargar e inicializar la interfaz de usuario principal.
 * - Configurar manejadores de eventos (event handlers) para la salida de la aplicación.
 * - Mostrar mensajes de error cuando sea necesario.
 * - Manejar el apagado (shutdown) de la aplicación.
 * Esta aplicación utiliza FXML para la definición de la interfaz de usuario e implementa el registro (logging) para el seguimiento de los eventos de la aplicación.
 * 
 * @extends Application JavaFX Application class
 * @see javafx.application.Application
 * @see GestionUsuariosController
 * @author Eduardo y Pablo
 */
public class SignUpSignIn extends Application {
    
    // Logger de los eventos principales de la aplicación en SignUpSignIn
        private static final Logger LOGGER = Logger.getLogger("SignUpSignIn.Main");
        
    @Override
    public void start(Stage stage) {
        try {
            // Cargar interfaz desde FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UI/FXMLDocument.fxml"));
            Parent root = loader.load();
        
            if (root == null) {
                LOGGER.severe("No se pudo cargar la interfaz FXML: root es null.");
                showError("The user interface could not be loaded.");
                return;
            }
        
            // Obtener controlador y pasar Stage
            GestionUsuariosController controller = loader.getController();
            if (controller == null) {
                LOGGER.severe("No se encontró el controlador en el FXML.");
                showError("The driver was not found in the FXML file.");
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
            showError("An error occurred while starting the application. Please try again later.");
    }
    }

    /**
     * Muestra una confirmación antes de cerrar la aplicación.
     */
    private void handleExit(Stage stage) {
        try{
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.initOwner(stage);
            alert.setTitle("Confirm exit");
            alert.setHeaderText("Do you want to exit the application?");
            alert.setContentText("All windows will be closed.");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                LOGGER.info("Aplicación finalizada por el usuario.");
                stage.close();
            }
        }catch(Exception e){
        String errorMsg="Error exiting application: "+e.getMessage();
        this.showError(errorMsg);
        LOGGER.log(Level.SEVERE, errorMsg);
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
     * Lanza la aplicacion JavaFX.
     * @param args
     */
    public static void main(String[] args) {
        launch(args);
    }
}