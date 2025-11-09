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
 * Controller class for the main page (PaginaPrincipal) of the application.
 * Manages the user interface and interactions after successful login.
 * 
 * This controller handles:
 * - Displaying user information (name, email, ID)
 * - Customized welcome messages based on time of day
 * - Logout functionality with confirmation
 * - Error handling and user notifications
 * 
 * The class follows a standard JavaFX controller pattern with FXML injection
 * and requires proper initialization through the init() method.
 * 
 * Important notes:
 * - setCustomer() must be called BEFORE init()
 * - Requires valid FXML components to be properly injected
 * - Implements logging for debugging and error tracking
 * 
 * @author Eduardo
 * @version 1.0
 * @see Customer
 * @see GestionUsuariosController
 * @see PaginaPrincipal.fxml
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
            stage.setTitle("Main window");
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
            showErrorAlert("Error loading the main window: " + e.getMessage());
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
            LOGGER.log(Level.WARNING, "Error loading user data", e);
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
            return "Good mornign";
        } else if (hour >= 12 && hour < 20) {
            return "Good afternoon";
        } else {
            return "Good night";
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
                showErrorAlert("Error: Unable to log out successfully.");
                return;
            }
            // Mostrar diálogo de confirmación
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.initModality(Modality.APPLICATION_MODAL);
            confirmAlert.initOwner(stage);
            confirmAlert.setTitle("Log out confirm");
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
            showErrorAlert("Error to log out: " + e.getMessage());
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
                showErrorAlert("Error: Cannot return to login.");
                return;
            }
            
            // Limpiar datos del usuario actual ANTES de cargar login
            String loggedOutUser = customer != null ? customer.getEmail() : "unknown";
            customer = null;
            LOGGER.info("Session data cleared for user: " + loggedOutUser);
            
            // Cargar la ventana de login
            LOGGER.info("Loading FXMLDocument.fxml...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UI/FXMLDocument.fxml"));
            Parent root = loader.load();
            
            if (root == null) {
                LOGGER.severe("ERROR: Root is null when loading FXMLDocument.fxml");
                showErrorAlert("Error loading login interface.");
                return;
            }
            LOGGER.info("Login FXML loaded successfully");
            
            // Obtener el controlador de login
            GestionUsuariosController controller = loader.getController();
            
            if (controller == null) {
                LOGGER.severe("ERROR: Login controller is null");
                showErrorAlert("Error loading login controller.");
                return;
            }
            LOGGER.info("Login controller obtained");
            
            // Inicializar el controlador SIN mostrar la ventana (ya está visible)
            controller.initWithoutShow(stage, root);
            
            LOGGER.info("Session closed successfully. Login window loaded.");
            
        } catch (java.io.IOException e) {
            LOGGER.log(Level.SEVERE, "I/O error loading login", e);
            showErrorAlert("Error loading login interface: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error returning to login", e);
            e.printStackTrace();
            showErrorAlert("Error logging out: " + e.getMessage());
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