package UI;

import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
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
            PasswordTooltip.setText("La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres.");
            Tooltip.install(LabelTooltipPassword, PasswordTooltip);

            stage.show();
        } catch (Exception e) {
            showError("Error al inicializar la ventana: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error al inicializar Sign-In", e);
        }
    }

    /**
     * Evento: Pulsación del botón Login
     */
    private void handleLoginButtonOnAction(ActionEvent event) {
        // Autenticación: usar el servicio REST para obtener el Customer por email+password
        try {
            String email = EmailTextField.getText().trim();
            String password = PasswordField.getText();

            if (!EMAIL_REGEX.matcher(email).matches() || password.length() < MIN_PASSWORD_LENGTH) {
                String emailError = !EMAIL_REGEX.matcher(email).matches() ? "Formato de email inválido" : "";
                String passwordError = password.length() < MIN_PASSWORD_LENGTH ? "Contraseña demasiado corta" : "";
                showInlineError(emailError, passwordError);
                return;
            }

            LOGGER.info("Evento: login_attempt (REST)");

            CustomerRESTClient client = new CustomerRESTClient();
            try {
                // Llamada al servicio REST que busca por email y password
                Customer found = client.findCustomerByEmailPassword_XML(Customer.class, email, password);

                if (found != null && found.getId() != null) {
                    LOGGER.info("Evento: login_success (REST)");
                    // Aquí puedes guardar el customer en contexto de la app si lo necesitas
                    Platform.runLater(() -> navigateToMain());
                } else {
                    // Credenciales incorrectas
                    showInlineError("", "Email o contraseña incorrectos.");
                    LOGGER.info("Evento: login_failed (REST): credenciales inválidas");
                    PasswordField.requestFocus();
                    PasswordField.selectAll();
                }

            } catch (Exception e) {
                // Errores de conexión / servidor
                String msg = e.getMessage() != null ? e.getMessage() : "No se pudo conectar con el servicio.";
                showInlineError("", "Error al autenticar: " + msg);
                LOGGER.log(Level.SEVERE, "Error durante autenticación REST", e);
            } finally {
                client.close();
            }

        } catch (NoSuchElementException ex) {
            showInlineError("", "Email o contraseña incorrectos.");
            LOGGER.log(Level.WARNING, "Usuario no encontrado", ex);
        } catch (Exception ex) {
            showError("Ha ocurrido un error inesperado.");
            LOGGER.log(Level.SEVERE, "Error en login", ex);
        } finally {
            LoginButton.setDisable(false);
        }
    }

    /**
     * Validación en tiempo real de formato del email.
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
     * Validación en tiempo real de contraseña.
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
            Error_email.setText("Formato de email inválido");
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
            Error_password.setText("Contraseña demasiado corta");
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
     * Simula autenticación de usuario.
     */
    private boolean fakeBackendAuth(String email, String password) {
        // Simulación simple: solo "test@correo.com" con "12345678" es válido
        return email.equals("test@correo.com") && password.equals("12345678");
    }

    private void navigateToMain() {
        LOGGER.info("Navegando a ventana principal...");
        // TODO: Implementar la carga y el cambio de escena a la pantalla principal de la aplicación.
        // Ejemplo: cargar el FXML de la pantalla principal y establecerlo en el Stage actual.
        Platform.runLater(() -> {
            // TODO: Reemplazar esto por la lógica real de navegación.
            showError("Login correcto (simulación).");
        });
    }

    private void handleForgotPassword() {
        LOGGER.info("Evento: forgot_password_requested");
        try {
            // Aquí cargarías la ventana de recuperación de contraseña
            showError("Ir a ventana de recuperación de contraseña (a implementar)");
        } catch (Exception e) {
            showInlineError("", "No se pudo abrir la ventana de recuperación.");
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
