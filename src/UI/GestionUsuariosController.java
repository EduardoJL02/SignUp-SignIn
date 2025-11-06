package UI;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import logic.CustomerRESTClient;
import model.Customer;

/**
 * Controlador de la ventana Sign-In (Login)
 * Maneja autenticación de usuarios mediante REST API
 * 
 * @author Tu Nombre
 */
public class GestionUsuariosController {

    // ======================== COMPONENTES FXML ========================
    @FXML private Button LoginButton;
    @FXML private PasswordField PasswordField;
    @FXML private TextField EmailTextField;
    @FXML private Hyperlink GetPasswordLink;
    @FXML private Hyperlink SignUpLink;
    @FXML private Label LabelTooltipPassword;
    @FXML private Tooltip PasswordTooltip;
    @FXML private Label EmailText;
    @FXML private Label PasswordText;
    @FXML private Label LoginText;
    @FXML private Text AccountText;
    @FXML private Label Error_email;
    @FXML private Label Error_password;

    // ======================== CONSTANTES ========================
    private static final Logger LOGGER = Logger.getLogger("SignUpSignIn.UI");
    private static final Pattern EMAIL_REGEX = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int MIN_PASSWORD_LENGTH = 8;
    
    // Estilos CSS inline para feedback visual
    private static final String STYLE_ERROR_BORDER = "-fx-border-color: red; -fx-border-width: 2px;";
    private static final String STYLE_NORMAL_BORDER = "-fx-border-color: grey; -fx-border-width: 1px;";
    private static final String STYLE_FOCUS_BORDER = "-fx-border-color: #0078d4; -fx-border-width: 2px;";

    // ======================== VARIABLES DE INSTANCIA ========================
    private Stage stage;
    private Customer loggedCustomer; // Almacena el usuario autenticado

    /**
     * Inicializa el controlador y configura la ventana de login.
     * 
     * @param stage Stage principal de la aplicación
     * @param root Nodo raíz del FXML
     */
    public void init(Stage stage, Parent root) {
        initializeController(stage, root);
        stage.show(); // Mostrar ventana solo en inicio
        LOGGER.info("Login window initialized and shown successfully.");
    }

    /**
     * Inicializa el controlador sin mostrar la ventana (para navegación desde logout).
     * 
     * @param stage Stage principal de la aplicación
     * @param root Nodo raíz del FXML
     */
    public void initWithoutShow(Stage stage, Parent root) {
        initializeController(stage, root);
        LOGGER.info("Login window reinitialized successfully (from logout).");
    }

    /**
     * Lógica común de inicialización del controlador.
     * 
     * @param stage Stage principal
     * @param root Nodo raíz del FXML
     */
    private void initializeController(Stage stage, Parent root) {
        try {
            this.stage = stage;
            LOGGER.info("Initializing login controller.");
            
            // Configuración de la ventana
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("LOGIN");
            stage.setResizable(false);
            
            // Estado inicial de controles
            LoginButton.setDisable(true);
            clearErrorMessages();
            
            // Asociar eventos a manejadores
            LoginButton.setOnAction(this::handleLoginButtonOnAction);
            EmailTextField.textProperty().addListener(this::handleEmailTextChange);
            EmailTextField.focusedProperty().addListener(this::handleEmailFocusChange);
            PasswordField.textProperty().addListener(this::handlePasswordChange);
            PasswordField.focusedProperty().addListener(this::handlePasswordFocusChange);
            GetPasswordLink.setOnAction(e -> handleForgotPassword());
            SignUpLink.setOnAction(e -> handleSignUp());
            
            // Configurar tooltip de requisitos de contraseña
            PasswordTooltip.setText("La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres.");
            Tooltip.install(LabelTooltipPassword, PasswordTooltip);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al inicializar controlador de login", e);
            showErrorAlert("Error al inicializar la ventana: " + e.getMessage());
        }
    }

    // ======================== MANEJADORES DE EVENTOS ========================

    /**
     * Maneja el evento de clic en el botón Login.
     * Realiza autenticación contra el backend REST.
     * 
     * @param event Evento de acción del botón
     */
    private void handleLoginButtonOnAction(ActionEvent event) {
        String email = EmailTextField.getText().trim();
        String password = PasswordField.getText();

        // Validación final (por si acaso)
        if (!EMAIL_REGEX.matcher(email).matches() || password.length() < MIN_PASSWORD_LENGTH) {
            String emailError = !EMAIL_REGEX.matcher(email).matches() ? "Formato de email inválido" : "";
            String passwordError = password.length() < MIN_PASSWORD_LENGTH ? "Contraseña demasiado corta" : "";
            showInlineError(emailError, passwordError);
            return;
        }

        LOGGER.info("Evento: login_attempt para email: " + email);
        
        // Deshabilitar controles durante la petición
        setControlsDisabled(true);
        clearErrorMessages();

        // Crear tarea asíncrona para autenticación REST
        Task<Customer> loginTask = new Task<Customer>() {
            @Override
            protected Customer call() throws Exception {
                CustomerRESTClient client = new CustomerRESTClient();
                try {
                    // Codificar parámetros para URL
                    String encEmail = URLEncoder.encode(email, StandardCharsets.UTF_8.toString());
                    String encPassword = URLEncoder.encode(password, StandardCharsets.UTF_8.toString());
                    
                    LOGGER.info("Conectando a REST API para autenticación...");
                    
                    // Llamada al servicio REST
                    return client.findCustomerByEmailPassword_XML(Customer.class, encEmail, encPassword);
                    
                } finally {
                    client.close();
                }
            }
        };

        // Manejo de éxito
        loginTask.setOnSucceeded(workerStateEvent -> {
            Customer customer = loginTask.getValue();
            
            if (customer != null && customer.getId() != null) {
                LOGGER.info("Evento: login_success para ID: " + customer.getId());
                loggedCustomer = customer; // Almacenar usuario autenticado
                
                // Navegar directamente a la ventana principal
                navigateToMain();
            } else {
                LOGGER.warning("Evento: login_failed - Customer null o sin ID");
                showInlineError("", "Error inesperado: datos de usuario incompletos.");
                setControlsDisabled(false);
            }
        });

        // Manejo de errores
        loginTask.setOnFailed(workerStateEvent -> {
            Throwable ex = loginTask.getException();
            handleLoginError(ex);
            setControlsDisabled(false);
        });

        // Ejecutar tarea en hilo separado
        Thread loginThread = new Thread(loginTask, "login-rest-thread");
        loginThread.setDaemon(true);
        loginThread.start();
    }

    /**
     * Maneja los diferentes tipos de errores de autenticación.
     * 
     * @param ex Excepción capturada durante el login
     */
    private void handleLoginError(Throwable ex) {
        if (ex instanceof NotAuthorizedException) {
            // 401: Credenciales incorrectas
            LOGGER.info("Evento: login_failed - Credenciales inválidas");
            showInlineError("", "Email o contraseña incorrectos.");
            highlightErrorFields(true, true);
            PasswordField.requestFocus();
            PasswordField.selectAll();
            
        } else if (ex instanceof InternalServerErrorException) {
            // 500: Error del servidor
            LOGGER.severe("Evento: login_error_server - Error interno del servidor");
            showErrorAlert("Error en el servidor.\nPor favor, inténtalo más tarde.\n\n" +
                          "Si el problema persiste, contacta con el administrador.");
            
        } else if (ex instanceof ClientErrorException) {
            ClientErrorException clientEx = (ClientErrorException) ex;
            int status = clientEx.getResponse().getStatus();
            
            if (status == 404) {
                LOGGER.severe("Error 404: El endpoint REST no existe");
                showErrorAlert("Error: El servicio de autenticación no está disponible.\n\n" +
                              "Contacta con el administrador del sistema.");
            } else {
                LOGGER.severe("Error REST " + status + ": " + clientEx.getMessage());
                showInlineError("", "Error del servidor: código " + status);
            }
            
        } else {
            // Error genérico (red, timeout, etc.)
            String msg = ex != null && ex.getMessage() != null ? ex.getMessage() : "No se pudo conectar con el servicio.";
            LOGGER.log(Level.SEVERE, "Error durante autenticación REST", ex);
            showErrorAlert("Error al conectar con el servidor:\n" + msg + "\n\n" +
                          "Verifica tu conexión a internet y que el servidor esté activo.");
        }
    }

    /**
     * Muestra mensaje de bienvenida al usuario.
     * 
     * @param customer Usuario autenticado
     */
    private void showWelcomeMessage(Customer customer) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.initOwner(stage);
            alert.setTitle("Login exitoso");
            alert.setHeaderText("¡Bienvenido!");
            alert.setContentText("Hola, " + customer.getFirstName() + " " + customer.getLastName());
            alert.showAndWait();
        });
    }

    // ======================== VALIDACIONES EN TIEMPO REAL ========================

    /**
     * Valida el formato del email en tiempo real.
     */
    private void handleEmailTextChange(ObservableValue<? extends String> obs, String oldValue, String newValue) {
        validateInputs();
    }

    /**
     * Maneja el cambio de foco en el campo email.
     */
    private void handleEmailFocusChange(ObservableValue<? extends Boolean> obs, Boolean oldValue, Boolean newValue) {
        if (newValue) {
            // Gana foco: limpiar estilo de error
            EmailTextField.setStyle(STYLE_FOCUS_BORDER);
        } else {
            // Pierde foco: validar
            if (validateEmail()) {
                EmailTextField.setStyle(STYLE_NORMAL_BORDER);
            }
        }
    }

    /**
     * Valida la contraseña en tiempo real.
     */
    private void handlePasswordChange(ObservableValue<? extends String> obs, String oldValue, String newValue) {
        validateInputs();
    }

    /**
     * Maneja el cambio de foco en el campo contraseña.
     */
    private void handlePasswordFocusChange(ObservableValue<? extends Boolean> obs, Boolean oldValue, Boolean newValue) {
        if (newValue) {
            // Gana foco: limpiar estilo de error
            PasswordField.setStyle(STYLE_FOCUS_BORDER);
        } else {
            // Pierde foco: validar
            if (validatePassword()) {
                PasswordField.setStyle(STYLE_NORMAL_BORDER);
            }
        }
    }

    /**
     * Valida ambos campos y habilita/deshabilita el botón Login.
     */
    private void validateInputs() {
        boolean emailValid = validateEmail();
        boolean passwordValid = validatePassword();
        LoginButton.setDisable(!(emailValid && passwordValid));
    }

    /**
     * Valida el formato del email.
     * 
     * @return true si el email es válido
     */
    private boolean validateEmail() {
        String email = EmailTextField.getText().trim();
        boolean valid = !email.isEmpty() && EMAIL_REGEX.matcher(email).matches();

        if (!valid) {
            Error_email.setText(email.isEmpty() ? "El email es obligatorio" : "Formato de email inválido");
            return false;
        } else {
            Error_email.setText("");
            return true;
        }
    }

    /**
     * Valida la longitud de la contraseña.
     * 
     * @return true si la contraseña es válida
     */
    private boolean validatePassword() {
        String password = PasswordField.getText();
        boolean valid = password != null && password.length() >= MIN_PASSWORD_LENGTH;

        if (!valid) {
            Error_password.setText("La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres");
            return false;
        } else {
            Error_password.setText("");
            return true;
        }
    }

    // ======================== NAVEGACIÓN ========================

    /**
     * Navega a la ventana principal de la aplicación.
     */
    private void navigateToMain() {
        LOGGER.info("=== INICIO NAVEGACIÓN A VENTANA PRINCIPAL ===");
        LOGGER.info("Stage actual: " + (stage != null ? "OK" : "NULL"));
        LOGGER.info("Logged customer: " + (loggedCustomer != null ? loggedCustomer.getEmail() : "NULL"));
        
        Platform.runLater(() -> {
            try {
                // 1. Verificar recurso FXML
                LOGGER.info("Paso 1: Buscando recurso /UI/PaginaPrincipal.fxml");
                java.net.URL fxmlUrl = getClass().getResource("/UI/PaginaPrincipal.fxml");
                
                if (fxmlUrl == null) {
                    LOGGER.severe("ERROR: No se encontró el archivo PaginaPrincipal.fxml en /UI/");
                    showErrorAlert("Error: No se encontró el archivo PaginaPrincipal.fxml\n\n" +
                                  "Verifica que el archivo esté en src/UI/PaginaPrincipal.fxml");
                    return;
                }
                LOGGER.info("Recurso encontrado: " + fxmlUrl);
                
                // 2. Cargar el FXML
                LOGGER.info("Paso 2: Cargando FXML...");
                FXMLLoader loader = new FXMLLoader(fxmlUrl);
                Parent root = loader.load();
                LOGGER.info("FXML cargado exitosamente. Root: " + (root != null ? "OK" : "NULL"));
                
                // 3. Obtener el controlador
                LOGGER.info("Paso 3: Obteniendo controlador...");
                PaginaPrincipalController controller = loader.getController();
                
                if (controller == null) {
                    LOGGER.severe("ERROR: Controller es NULL. Verifica fx:controller en PaginaPrincipal.fxml");
                    showErrorAlert("Error: No se pudo cargar el controlador de la ventana principal.\n\n" +
                                  "Verifica que PaginaPrincipal.fxml tenga: fx:controller=\"UI.PaginaPrincipalController\"");
                    return;
                }
                LOGGER.info("Controlador obtenido: " + controller.getClass().getName());
                
                // 4. Pasar el usuario autenticado
                LOGGER.info("Paso 4: Pasando customer al controlador...");
                if (loggedCustomer == null) {
                    LOGGER.severe("ERROR: loggedCustomer es NULL");
                    showErrorAlert("Error: No hay información del usuario autenticado.");
                    return;
                }
                controller.setCustomer(loggedCustomer);
                LOGGER.info("Customer pasado correctamente");
                
                // 5. Verificar stage
                LOGGER.info("Paso 5: Verificando stage...");
                if (stage == null) {
                    LOGGER.severe("ERROR: Stage es NULL");
                    showErrorAlert("Error: No hay ventana disponible para navegar.");
                    return;
                }
                LOGGER.info("Stage verificado: OK");
                
                // 6. Inicializar la ventana principal
                LOGGER.info("Paso 6: Inicializando ventana principal...");
                controller.init(stage, root);
                
                LOGGER.info("=== NAVEGACIÓN EXITOSA ===");
                LOGGER.info("Usuario: " + loggedCustomer.getEmail());
                
            } catch (java.io.IOException e) {
                LOGGER.log(Level.SEVERE, "Error de I/O al cargar FXML", e);
                showErrorAlert("Error al cargar la interfaz:\n" + e.getMessage() + "\n\n" +
                              "Verifica que PaginaPrincipal.fxml esté en src/UI/");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error inesperado al navegar", e);
                e.printStackTrace(); // Para ver el stacktrace completo
                showErrorAlert("Error inesperado: " + e.getMessage());
            }
        });
    }

    /**
     * Maneja el evento "Olvidé mi contraseña".
     */
    private void handleForgotPassword() {
        LOGGER.info("Evento: forgot_password_requested");
        
        try {
            // TODO: Implementar navegación a ventana de recuperación
            /*
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UI/ForgotPassword.fxml"));
            Parent root = loader.load();
            ForgotPasswordController controller = loader.getController();
            controller.init(new Stage(), root);
            */
            
            // TEMPORAL
            showInfoAlert("Ir a ventana de recuperación de contraseña (a implementar)");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al abrir ventana de recuperación", e);
            showInlineError("", "No se pudo abrir la ventana de recuperación.");
        }
    }

    /**
     * Maneja el evento "Registrarse".
     */
    private void handleSignUp() {
        LOGGER.info("Evento: register_navigated");
        
        try {
            // TODO: Implementar navegación a ventana de registro
            /*
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UI/SignUp.fxml"));
            Parent root = loader.load();
            SignUpController controller = loader.getController();
            controller.init(new Stage(), root);
            */
            
            // TEMPORAL
            showInfoAlert("Ir a ventana de registro (a implementar)");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al abrir ventana de registro", e);
            showInlineError("", "No se pudo abrir la ventana de registro.");
        }
    }

    // ======================== MÉTODOS AUXILIARES ========================

    /**
     * Muestra mensajes de error inline debajo de los campos.
     * 
     * @param emailMessage Mensaje de error para email (vacío si no hay error)
     * @param passwordMessage Mensaje de error para contraseña (vacío si no hay error)
     */
    private void showInlineError(String emailMessage, String passwordMessage) {
        Error_email.setText(emailMessage);
        Error_password.setText(passwordMessage);
    }

    /**
     * Limpia todos los mensajes de error inline.
     */
    private void clearErrorMessages() {
        Error_email.setText("");
        Error_password.setText("");
    }

    /**
     * Resalta los campos con error mediante bordes rojos.
     * 
     * @param highlightEmail true para resaltar el campo email
     * @param highlightPassword true para resaltar el campo contraseña
     */
    private void highlightErrorFields(boolean highlightEmail, boolean highlightPassword) {
        if (highlightEmail) {
            EmailTextField.setStyle(STYLE_ERROR_BORDER);
        } else {
            EmailTextField.setStyle(STYLE_NORMAL_BORDER);
        }
        
        if (highlightPassword) {
            PasswordField.setStyle(STYLE_ERROR_BORDER);
        } else {
            PasswordField.setStyle(STYLE_NORMAL_BORDER);
        }
    }

    /**
     * Habilita o deshabilita los controles durante operaciones asíncronas.
     * 
     * @param disabled true para deshabilitar, false para habilitar
     */
    private void setControlsDisabled(boolean disabled) {
        LoginButton.setDisable(disabled);
        EmailTextField.setDisable(disabled);
        PasswordField.setDisable(disabled);
        GetPasswordLink.setDisable(disabled);
        SignUpLink.setDisable(disabled);
    }

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
     * @param message Mensaje informativo a mostrar
     */
    private void showInfoAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.initOwner(stage);
            alert.setTitle("Información");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // ======================== GETTERS Y SETTERS ========================

    /**
     * Obtiene el usuario autenticado.
     * 
     * @return Customer autenticado o null si no hay sesión
     */
    public Customer getLoggedCustomer() {
        return loggedCustomer;
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