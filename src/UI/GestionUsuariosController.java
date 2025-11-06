package UI;

import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.ClientErrorException;
import model.Customer;
import logic.CustomerRESTClient;


/**
 * Controlador de la ventana Sign-In (Login)
 */
public class GestionUsuariosController {

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

    private static final Logger LOGGER = Logger.getLogger("SignUpSignIn.UI");

    private static final Pattern EMAIL_REGEX = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int MIN_PASSWORD_LENGTH = 8;

    public void init(Stage stage, Parent root) {
        try {
            LOGGER.info("Initializing login stage.");
            stage.setTitle("LOGIN");
            stage.setResizable(false);
            LoginButton.setDisable(true);

            // Asociar eventos
            LoginButton.setOnAction(this::handleLoginButtonOnAction);
            EmailTextField.textProperty().addListener(this::handleEmailTextChange);
            EmailTextField.focusedProperty().addListener(this::handleEmailFocusChange);
            PasswordField.textProperty().addListener(this::handlePasswordChange);

            GetPasswordLink.setOnAction(e -> handleForgotPassword());
            SignUpLink.setOnAction(e -> handleSignUp());

            // Mostrar tooltip de requisitos
            PasswordTooltip.setText("La contrasenia debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres.");
            Tooltip.install(LabelTooltipPassword, PasswordTooltip);

            stage.show();
        } catch (Exception e) {
            showError("Error al inicializar la ventana: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error al inicializar Sign-In", e);
        }
    }

    /**
     * Evento: Pulsaci�n del bot�n Login
     */
    private void handleLoginButtonOnAction(ActionEvent event) {
    String email = EmailTextField.getText().trim();
    String password = PasswordField.getText();

    if (!EMAIL_REGEX.matcher(email).matches() || password.length() < MIN_PASSWORD_LENGTH) {
        String emailError = !EMAIL_REGEX.matcher(email).matches() ? "Formato de email invalido" : "";
        String passwordError = password.length() < MIN_PASSWORD_LENGTH ? "Contrasenia demasiado corta" : "";
        showInlineError(emailError, passwordError);
        return;
    }

    LOGGER.info("Evento: login_attempt");
    LoginButton.setDisable(true);
    Error_email.setText("");
    Error_password.setText("");

    Task<Customer> task = new Task<Customer>() {
        @Override
        protected Customer call() throws Exception {
            CustomerRESTClient client = new CustomerRESTClient();
            try {
                String encEmail = URLEncoder.encode(email, StandardCharsets.UTF_8.toString());
                String encPassword = URLEncoder.encode(password, StandardCharsets.UTF_8.toString());
                
                // AÑADE ESTE LOG PARA VER LA URL EXACTA
                String urlFinal = "http://localhost:8080/CRUDBankServerSide/webresources/customer/signin/" 
                                + encEmail + "/" + encPassword;
                LOGGER.info("Intentando conectar a: " + urlFinal);
                
                return client.findCustomerByEmailPassword_XML(Customer.class, encEmail, encPassword);
            } catch (ClientErrorException e) {
                // CAPTURA EL ERROR ESPECÍFICO
                LOGGER.severe("Error ClientErrorException: " + e.getMessage() + " - Status: " + e.getResponse().getStatus());
                throw e;
            } finally {
                client.close();
            }
        }
    };

    task.setOnSucceeded(workerStateEvent -> {
        Customer found = task.getValue();
        if (found != null && found.getId() != null) {
            LOGGER.info("Evento: login_success");
            navigateToMain();
        } else {
            showInlineError("", "Email o contrasenia incorrectos.");
            LOGGER.info("Evento: login_failed: credenciales invalidas");
            PasswordField.requestFocus();
            PasswordField.selectAll();
        }
        LoginButton.setDisable(false);
    });

    task.setOnFailed(workerStateEvent -> {
        Throwable ex = task.getException();
        
        // MANEJO MEJORADO DE EXCEPCIONES
        if (ex instanceof ClientErrorException) {
            ClientErrorException clientEx = (ClientErrorException) ex;
            int status = clientEx.getResponse().getStatus();
            
            if (status == 404) {
                showInlineError("", "Error: El servicio de autenticacion no esta disponible (404).");
                LOGGER.severe("Error 404: El endpoint REST no existe en el servidor.");
            } else if (status == 401) {
                showInlineError("", "Email o contrasenia incorrectos.");
                LOGGER.info("Error 401: Credenciales invalidas.");
            } else {
                showInlineError("", "Error del servidor: " + status);
                LOGGER.severe("Error " + status + ": " + clientEx.getMessage());
            }
        } else {
            String msg = ex != null && ex.getMessage() != null ? ex.getMessage() : "No se pudo conectar con el servicio.";
            showInlineError("", "Error al autenticar: " + msg);
            LOGGER.log(Level.SEVERE, "Error durante autenticacion REST", ex);
        }
        
        LoginButton.setDisable(false);
    });

    Thread th = new Thread(task, "login-rest-thread");
    th.setDaemon(true);
    th.start();
}


    /**
     * Validaci�n en tiempo real de formato del email.
     */
    private void handleEmailTextChange(ObservableValue<? extends String> obs, String oldValue, String newValue) {
        validateInputs();
    }

    private void handleEmailFocusChange(ObservableValue<? extends Boolean> obs, Boolean oldValue, Boolean newValue) {
        if (!newValue) { // Al perder foco
            validateEmail();
        }
    }

    /**
     * Validaci�n en tiempo real de contrase�a.
     */
    private void handlePasswordChange(ObservableValue<? extends String> obs, String oldValue, String newValue) {
        validateInputs();
    }

    private void validateInputs() {
        boolean emailValid = validateEmail();
        boolean passwordValid = validatePassword();

        LoginButton.setDisable(!(emailValid && passwordValid));
    }

    private boolean validateEmail() {
        String email = EmailTextField.getText().trim();
        boolean valid = EMAIL_REGEX.matcher(email).matches();

        if (email.isEmpty() || !valid) {
            Error_email.setText("Formato de email invalido");
            return false;
        } else {
            Error_email.setText("");
            return true;
        }
    }

    private boolean validatePassword() {
        String password = PasswordField.getText();
        boolean valid = password != null && password.length() >= MIN_PASSWORD_LENGTH;

        if (!valid) {
            Error_password.setText("Contrasenia demasiado corta");
            return false;
        } else {
            Error_password.setText("");
            return true;
        }
    }

    private void showInlineError(String emailMessage, String passwordMessage) {
        Error_email.setText(emailMessage);
        Error_password.setText(passwordMessage);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Simula autenticaci�n de usuario.
     */
    private boolean fakeBackendAuth(String email, String password) {
        // Simulaci�n simple: solo "test@correo.com" con "12345678" es v�lido
        return email.equals("test@correo.com") && password.equals("12345678");
    }

    private void navigateToMain() {
        LOGGER.info("Navegando a ventana principal...");
        // TODO: Implementar la carga y el cambio de escena a la pantalla principal de la aplicaci�n.
        // Ejemplo: cargar el FXML de la pantalla principal y establecerlo en el Stage actual.
        Platform.runLater(() -> {
            // TODO: Reemplazar esto por la l�gica real de navegaci�n.
            showError("Login correcto (simulacion).");
        });
    }

    private void handleForgotPassword() {
        LOGGER.info("Evento: forgot_password_requested");
        try {
            // Aqu� cargar�as la ventana de recuperaci�n de contrase�a
            showError("Ir a ventana de recuperacion de contrasenia (a implementar)");
        } catch (Exception e) {
            showInlineError("", "No se pudo abrir la ventana de recuperacion.");
        }
    }

    private void handleSignUp() {
        LOGGER.info("Evento: register_navigated");
        try {
            // TODO: Implementar la carga y apertura de la ventana de registro (Sign-Up).
            // Ejemplo: cargar el FXML de registro y mostrarlo en una nueva escena o ventana.
            showError("Ir a ventana de registro (a implementar)");
        } catch (Exception e) {
            showInlineError("", "No se pudo abrir la ventana de registro.");
        }
    }
}
