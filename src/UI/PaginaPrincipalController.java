package UI;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Customer;

/**
 * Controlador de la Página Principal (después del login exitoso)
 * Muestra información del usuario autenticado y permite navegar a otras funcionalidades
 * 
 * @author Eduardo
 */
public class PaginaPrincipalController {

    // ======================== COMPONENTES FXML ========================
    @FXML private Label WelcomeLabel;
    @FXML private Label CustomerNameLabel;
    @FXML private Label EmailLabel;
    @FXML private Label CustomerIdLabel;
    @FXML private Button LogoutButton;

    // ======================== CONSTANTES ========================
    private static final Logger LOGGER = Logger.getLogger("SignUpSignIn.PaginaPrincipal");

    // ======================== VARIABLES DE INSTANCIA ========================
    private Stage stage;
    private Customer customer;

    /**
     * Inicializa el controlador y configura la ventana principal.
     * 
     * @param stage Stage de la ventana principal
     * @param root Nodo raíz del FXML
     */
    public void init(Stage stage, Parent root) {
        try {
            this.stage = stage;
            LOGGER.info("Initializing main window for customer: " + 
                       (customer != null ? customer.getEmail() : "unknown"));
            
            // Configuración de la ventana
            stage.setScene(new Scene(root));
            stage.setTitle("Página Principal");
            stage.setResizable(false);
            
            // Verificar que el customer no sea null
            if (customer == null) {
                LOGGER.severe("Error: Customer is null in PaginaPrincipalController");
                showErrorAlert("Error: User information could not be loaded.");
                return;
            }
            
            // Cargar datos del usuario en la interfaz
            loadCustomerData();
            
            // Asociar eventos a manejadores
                        LogoutButton.setOnAction(this::handleLogoutButtonAction);
            
            stage.show();
            LOGGER.info("Main window initialized successfully.");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al inicializar ventana principal", e);
            showErrorAlert("Error initializing main window: " + e.getMessage());
        }
    }

    /**
     * Carga los datos del usuario en los componentes de la interfaz.
     */
    private void loadCustomerData() {
        try {
            // Construir nombre completo
            String fullName = customer.getFirstName();
            
            if (customer.getMiddleInitial() != null && !customer.getMiddleInitial().isEmpty()) {
                fullName += " " + customer.getMiddleInitial() + ".";
            }
            
            if (customer.getLastName() != null && !customer.getLastName().isEmpty()) {
                fullName += " " + customer.getLastName();
            }
            
            // Actualizar etiquetas
            CustomerNameLabel.setText(fullName);
            EmailLabel.setText("Email: " + customer.getEmail());
            CustomerIdLabel.setText("ID: " + customer.getId());
            
            // Personalizar mensaje de bienvenida según la hora del día
            String greeting = getGreetingByTime();
            WelcomeLabel.setText(greeting.toUpperCase());
            
            LOGGER.info("Customer data loaded successfully: " + fullName);
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al cargar datos del usuario", e);
            CustomerNameLabel.setText("Usuario");
        }
    }

    /**
     * Obtiene un saludo personalizado según la hora del día.
     * 
     * @return Saludo apropiado ("Buenos días", "Buenas tardes", etc.)
     */
    private String getGreetingByTime() {
        int hour = java.time.LocalTime.now().getHour();
        
        if (hour >= 6 && hour < 12) {
            return "Good morning";
        } else if (hour >= 12 && hour < 20) {
            return "Good afternoon";
        } else {
            return "Good evening";
        }
    }

    // ======================== MANEJADORES DE EVENTOS ========================

    /**
     * Maneja el evento de clic en el botón "Cerrar Sesión".
     * Solicita confirmación y regresa a la ventana de login.
     * 
     * @param event Evento de acción del botón
     */
    private void handleLogoutButtonAction(ActionEvent event) {
        LOGGER.info("Evento: logout_requested");
        
        try {
            // Verificar que el stage no sea null
            if (stage == null) {
                LOGGER.severe("ERROR: Stage es null en handleLogoutButtonAction");
                showErrorAlert("Error: No se puede cerrar sesión correctamente.");
                return;
            }
        // Mostrar diálogo de confirmación
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.initModality(Modality.APPLICATION_MODAL);
        confirmAlert.initOwner(stage);
        confirmAlert.setTitle("Confirm log out");
        confirmAlert.setHeaderText("Do you want to log out?");
        confirmAlert.setContentText("You will need to log in again to access the application.");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
                LOGGER.info("Evento: logout_confirmed - Cerrando sesión del usuario: " + 
                           (customer != null ? customer.getEmail() : "unknown"));
                returnToLogin();
            } else {
                LOGGER.info("Evento: logout_cancelled");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en handleLogoutButtonAction", e);
            showErrorAlert("Error al cerrar sesión: " + e.getMessage());
        }
    }

    /**
     * Regresa a la ventana de login y limpia la sesión actual.
     */
    private void returnToLogin() {
        try {
            LOGGER.info("Iniciando retorno a login...");
            
            // Verificar que el stage no sea null
            if (stage == null) {
                LOGGER.severe("ERROR: Stage es null en returnToLogin");
                showErrorAlert("Error: No se puede regresar al login.");
                return;
            }
            
            // Limpiar datos del usuario actual ANTES de cargar login
            String loggedOutUser = customer != null ? customer.getEmail() : "unknown";
            customer = null;
            LOGGER.info("Datos de sesión limpiados para usuario: " + loggedOutUser);
            
            // Cargar la ventana de login
            LOGGER.info("Cargando FXMLDocument.fxml...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UI/FXMLDocument.fxml"));
            Parent root = loader.load();
            
            if (root == null) {
                LOGGER.severe("ERROR: Root es null al cargar FXMLDocument.fxml");
                showErrorAlert("Error al cargar la interfaz de login.");
                return;
            }
            LOGGER.info("FXML de login cargado exitosamente");
            
            // Obtener el controlador de login
            GestionUsuariosController controller = loader.getController();
            
            if (controller == null) {
                LOGGER.severe("ERROR: Controller de login es null");
                showErrorAlert("Error al cargar el controlador de login.");
                return;
            }
            LOGGER.info("Controlador de login obtenido");
            
            // Inicializar el controlador de login (configura listeners, etc.)
            controller.init(stage, root);
            
            LOGGER.info("Sesión cerrada exitosamente. Ventana de login cargada.");
            
        } catch (java.io.IOException e) {
            LOGGER.log(Level.SEVERE, "Error de I/O al cargar login", e);
            showErrorAlert("Error al cargar la interfaz de login: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error inesperado al regresar a login", e);
            e.printStackTrace();
            showErrorAlert("Error al cerrar sesión: " + e.getMessage());
        }
    }

    // ======================== MÉTODOS AUXILIARES ========================

    /**
     * Muestra un diálogo de error modal.
     * 
     * @param message Mensaje de error a mostrar
     */
    private void showErrorAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.initOwner(stage);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Muestra un diálogo de información modal.
     * 
     * @param title Título del diálogo
     * @param message Mensaje informativo a mostrar
     */
    private void showInfoAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.initOwner(stage);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // ======================== GETTERS Y SETTERS ========================

    /**
     * Establece el usuario autenticado que se mostrará en la ventana.
     * IMPORTANTE: Debe llamarse ANTES de init().
     * 
     * @param customer Usuario autenticado
     */
    public void setCustomer(Customer customer) {
        this.customer = customer;
        LOGGER.info("Customer set in PaginaPrincipalController: " + 
                   (customer != null ? customer.getEmail() : "null"));
    }

    /**
     * Obtiene el usuario actual.
     * 
     * @return Customer actual o null si no hay sesión
     */
    public Customer getCustomer() {
        return customer;
    }

    /**
     * Establece el Stage principal (si es necesario desde fuera).
     * 
     * @param stage Stage principal de la aplicación
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }
}