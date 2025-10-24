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

    private static final Logger LOGGER = Logger.getLogger("SignUpSignIn.Main");

    @Override
    public void start(Stage stage) {
        try {
            // Cargar interfaz desde FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UI/FXMLDocument.fxml"));
            Parent root = loader.load();

            // Obtener controlador y pasar Stage
            GestionUsuariosController controller = loader.getController();
            controller.init(stage, root);

            // Crear escena
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("LOGIN");
            stage.setResizable(false);

            // (Opcional) aplicar estilos o íconos
            // scene.getStylesheets().add(getClass().getResource("/UI/styles.css").toExternalForm());
            // stage.getIcons().add(new Image(getClass().getResourceAsStream("/UI/icon.png")));

            // Confirmación al cerrar
            stage.setOnCloseRequest(event -> {
                event.consume(); // prevenir cierre automático
                handleExit(stage);
            });

            stage.show();
            LOGGER.info("Ventana LOGIN iniciada correctamente.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al iniciar Sign-In", e);
            showError("Error al iniciar la aplicación:\n" + e.getMessage());
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
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
