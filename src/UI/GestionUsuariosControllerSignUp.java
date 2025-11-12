/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package UI;

import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label; // Asegúrate de importar Label
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.logging.Logger;
import java.util.logging.Level;

// Imports de JAX-RS
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.ForbiddenException; // Ya no la necesitas aquí si usas ConflictException

import logic.CustomerRESTClient;
import model.Customer;
import java.util.Optional;
import java.util.regex.Pattern;
import javafx.application.Platform;

/**
 * Controlador para la ventana de Registro (CREATE ACCOUNT)
 *
 * @author pablo
 */
public class GestionUsuariosControllerSignUp {
    // --- FXML INJECTIONS ---
    @FXML private TextField tfFName;
    @FXML private TextField tfMName;
    @FXML private TextField tfLName;
    @FXML private TextField tfAddress;
    @FXML private TextField tfCity;
    @FXML private TextField tfState;
    @FXML private TextField tfZip;
    @FXML private TextField tfPhone;
    @FXML private TextField tfEmail;
    @FXML private TextField tfPass;
    @FXML private TextField tfRPass;
    @FXML private Button btBack;
    @FXML private Button btCreate;

    // --- FXML INJECTIONS (Error Labels) ---
    @FXML private Label firstNameError;
    @FXML private Label middleNameError;
    @FXML private Label lastNameError;
    @FXML private Label addressError;
    @FXML private Label cityError;
    @FXML private Label stateError;
    @FXML private Label zipError;
    @FXML private Label phoneError;
    @FXML private Label emailError;
    @FXML private Label passwordError;
    @FXML private Label repeatPasswordError;

    // --- ATRIBUTOS DE CLASE ---
    private static final Logger LOGGER = Logger.getLogger("SignUpSignIn.SignUp");
    /**
     * El Stage (ventana) de este controlador. Se inicializa en init().
     */
    private Stage stage;

    // --- CONSTANTES DE VALIDACIÓN ---
    private final int MIN_PASSWORD_LENGTH = 8;

    // Patrones de validación (compilados para eficiencia)
    private static final Pattern LETTER_PATTERN = Pattern.compile("^[a-zA-ZÁáÉéÍíÓóÚúñÑ\\s]+$");
    private static final Pattern MNAME_PATTERN = Pattern.compile("^[a-zA-Z]\\.$");
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^[a-zA-ZÁáÉéÍíÓóÚúñÑ0-9.,\\-/ºª\\s]+$");
    private static final Pattern ZIP_PATTERN = Pattern.compile("^\\d{5}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{9,}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$");

    // --- VARIABLES DE ESTADO PARA VALIDACIÓN (Todos los campos son obligatorios) ---
    private boolean isFNameValid = false;
    private boolean isMNameValid = false;
    private boolean isLNameValid = false;
    private boolean isAddressValid = false;
    private boolean isCityValid = false;
    private boolean isStateValid = false;
    private boolean isZipValid = false;
    private boolean isPhoneValid = false;
    private boolean isEmailValid = false;
    private boolean isPasswordValid = false;
    private boolean isRepeatPasswordMatching = false;

// ============================================================================
// MÉTODOS DE INICIALIZACIÓN
// ============================================================================

    /**
 * Inicialización NUEVA: Para ser llamado desde Login.
 * Usa el Stage ya creado en lugar de crear uno nuevo.
 * 
 * @param stage El Stage ya configurado (modal) desde Login
 * @param root El Parent ya cargado desde Login
     */
public void initFromLogin(Stage stage, Parent root) {
    try {
        LOGGER.log(Level.INFO, "Initializing SignUp from Login (using existing Stage)");

        // Guardar referencia al Stage
        this.stage = stage;

        // Configurar el Stage (ya debería tener modality y owner desde Login)
        stage.setTitle("CREATE ACCOUNT");
        stage.setResizable(false);
        
        // Configurar la escena (ya debería estar asignada desde Login, pero por seguridad)
        if (stage.getScene() == null) {
            stage.setScene(new Scene(root));
        }

        // Estado inicial de los botones
        btBack.setDisable(false);
        btCreate.setDisable(true);

        // Foco inicial en el campo Nombre
        Platform.runLater(() -> tfFName.requestFocus());

        // Configurar manejadores de eventos
        setupEventHandlers();

        LOGGER.log(Level.INFO, "SignUp initialized successfully from Login");

    } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Error fatal al inicializar Sign-Up desde Login", e);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error de Aplicación");
        alert.setHeaderText("No se pudo cargar la ventana de registro.");
        alert.setContentText("Ocurrió un error inesperado: " + e.getMessage());
        alert.showAndWait();
    }
}

/**
 * Inicialización ORIGINAL: Mantener para compatibilidad si se usa como app independiente.
 * (Conservar el método init() existente sin cambios por si acaso)
 */
    public void init(Stage parentStage, Parent root) {
        try {
        LOGGER.log(Level.INFO, "Initializing SignUp (CREATE ACCOUNT) - Standalone mode");

            Scene scene = new Scene(root);

        // Crear un nuevo Stage modal
            Stage dialogStage = new Stage();
        this.stage = dialogStage;

            dialogStage.initOwner(parentStage);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(scene);
            dialogStage.setTitle("CREATE ACCOUNT");
            dialogStage.setResizable(false);

        // Estado inicial
            btBack.setDisable(false);
            btCreate.setDisable(true);

        // Foco inicial
            tfFName.requestFocus();

        // Configurar manejadores
        setupEventHandlers();

        // Mostrar la ventana
        dialogStage.show();

    } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Error fatal al inicializar Sign-Up", e);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error de Aplicación");
        alert.setHeaderText("No se pudo cargar la ventana de registro.");
        alert.setContentText("Ocurrió un error inesperado: " + e.getMessage());
        alert.showAndWait();
    }
}

/**
 * Método auxiliar para configurar todos los event handlers.
 * Evita duplicación de código entre init() e initFromLogin().
 */
private void setupEventHandlers() {
    // Asignación de manejadores a eventos
            btCreate.setOnAction(this::handleBtCreateOnAction);
            btBack.setOnAction(this::handleBtBackOnAction);

    // Listeners para todos los campos (FNAME, MNAME, LNAME, etc.)
            tfFName.textProperty().addListener(this::handleTfFNameTextChange);
            tfFName.focusedProperty().addListener(this::handleTfFNameFocusChange);
    
            tfMName.textProperty().addListener(this::handleTfMNameTextChange);
            tfMName.focusedProperty().addListener(this::handleTfMNameFocusChange);
    
            tfLName.textProperty().addListener(this::handleTfLNameTextChange);
            tfLName.focusedProperty().addListener(this::handleTfLNameFocusChange);
    
            tfAddress.textProperty().addListener(this::handleTfAddressTextChange);
            tfAddress.focusedProperty().addListener(this::handleTfAddressFocusChange);
    
            tfCity.textProperty().addListener(this::handleTfCityTextChange);
            tfCity.focusedProperty().addListener(this::handleTfCityFocusChange);
    
            tfState.textProperty().addListener(this::handleTfStateTextChange);
            tfState.focusedProperty().addListener(this::handleTfStateFocusChange);
    
            tfZip.textProperty().addListener(this::handleTfZipTextChange);
            tfZip.focusedProperty().addListener(this::handleTfZipFocusChange);
    
            tfPhone.textProperty().addListener(this::handleTfPhoneTextChange);
            tfPhone.focusedProperty().addListener(this::handleTfPhoneFocusChange);
    
            tfEmail.textProperty().addListener(this::handleTfEmailTextChange);
            tfEmail.focusedProperty().addListener(this::handleTfEmailFocusChange);
    
            tfPass.textProperty().addListener(this::handleTfPassTextChange);
            tfPass.focusedProperty().addListener(this::handleTfPassFocusChange);
    
            tfRPass.textProperty().addListener(this::handleTfRPassTextChange);
            tfRPass.focusedProperty().addListener(this::handleTfRPassFocusChange);
        }

    // -------------------------------------------------------------------------
    // --- LÓGICA DE VALIDACIÓN CENTRAL ---
    // -------------------------------------------------------------------------

    /**
     * Revisa el estado de validez de todos los campos obligatorios
     * y habilita/deshabilita el botón CREATE ACCOUNT.
     */
    private void checkGlobalValidation() {
        boolean allFieldsValid = isFNameValid && isMNameValid && isLNameValid &&
                isAddressValid && isCityValid && isStateValid &&
                isZipValid && isPhoneValid && isEmailValid &&
                isPasswordValid && isRepeatPasswordMatching;

        btCreate.setDisable(!allFieldsValid);
    }

    /**
     * Valida si un texto está vacío.
     */
    private boolean isTextEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }

    /**
     * Método auxiliar para actualizar las etiquetas de error visualmente.
     */
    private void updateErrorLabel(Label label, boolean isValid, String errorMessage) {
        if (isValid) {
            label.setText(""); // Oculta el error si es válido
        } else {
            label.setText(errorMessage); // Muestra el mensaje si no es válido
        }
    }

    // -------------------------------------------------------------------------
    // --- MÉTODOS DE VALIDACIÓN POR CAMPO (ACTUALIZADOS CON LABELS) ---
    // -------------------------------------------------------------------------

    private void validateTfFName(String text) {
        boolean isValid = !isTextEmpty(text) && LETTER_PATTERN.matcher(text).matches();
        String errorMsg = isTextEmpty(text) ? "Campo obligatorio" : "Solo letras permitidas";
        updateErrorLabel(firstNameError, isValid, errorMsg);
        isFNameValid = isValid;
    }

    private void validateTfMName(String text) {
        boolean isValid = !isTextEmpty(text) && MNAME_PATTERN.matcher(text).matches();
        String errorMsg = isTextEmpty(text) ? "Campo obligatorio" : "Formato: 'A.'";
        updateErrorLabel(middleNameError, isValid, errorMsg);
        isMNameValid = isValid;
    }

    private void validateTfLName(String text) {
        boolean isValid = !isTextEmpty(text) && LETTER_PATTERN.matcher(text).matches();
        String errorMsg = isTextEmpty(text) ? "Campo obligatorio" : "Solo letras permitidas";
        updateErrorLabel(lastNameError, isValid, errorMsg);
        isLNameValid = isValid;
    }

    private void validateTfAddress(String text) {
        boolean isValid = !isTextEmpty(text) && ADDRESS_PATTERN.matcher(text).matches();
        String errorMsg = isTextEmpty(text) ? "Campo obligatorio" : "Caracteres inválidos";
        updateErrorLabel(addressError, isValid, errorMsg);
        isAddressValid = isValid;
    }

    private void validateTfCity(String text) {
        boolean isValid = !isTextEmpty(text) && LETTER_PATTERN.matcher(text).matches();
        String errorMsg = isTextEmpty(text) ? "Campo obligatorio" : "Solo letras permitidas";
        updateErrorLabel(cityError, isValid, errorMsg);
        isCityValid = isValid;
    }

    private void validateTfState(String text) {
        boolean isValid = !isTextEmpty(text) && (LETTER_PATTERN.matcher(text).matches() || text.matches("^[A-Z]{2}$"));
        String errorMsg = isTextEmpty(text) ? "Campo obligatorio" : "Solo letras o formato 'NY'";
        updateErrorLabel(stateError, isValid, errorMsg);
        isStateValid = isValid;
    }

    private void validateTfZip(String text) {
        boolean isValid = !isTextEmpty(text) && ZIP_PATTERN.matcher(text).matches();
        String errorMsg = isTextEmpty(text) ? "Campo obligatorio" : "Debe tener minimo 5 dígitos";
        updateErrorLabel(zipError, isValid, errorMsg);
        isZipValid = isValid;
    }

    private void validateTfPhone(String text) {
        boolean isValid = !isTextEmpty(text) && PHONE_PATTERN.matcher(text).matches();
        String errorMsg = isTextEmpty(text) ? "Campo obligatorio" : "Mínimo 9 dígitos";
        updateErrorLabel(phoneError, isValid, errorMsg);
        isPhoneValid = isValid;
    }

    private void validateTfEmail(String text) {
        boolean isValid = !isTextEmpty(text) && EMAIL_PATTERN.matcher(text).matches();
        String errorMsg = isTextEmpty(text) ? "Campo obligatorio" : "Formato de email inválido";
        updateErrorLabel(emailError, isValid, errorMsg);
        isEmailValid = isValid;
    }

    private void validateTfPassword(String text) {
        if (isTextEmpty(text) || text.length() < MIN_PASSWORD_LENGTH) {
            isPasswordValid = false;
            updateErrorLabel(passwordError, false, "Mínimo " + MIN_PASSWORD_LENGTH + " caracteres");
            return;
        }

        boolean hasUpperCase = text.matches(".*[A-Z].*");
        boolean hasLowerCase = text.matches(".*[a-z].*");
        boolean hasDigit = text.matches(".*[0-9].*");
        boolean hasSymbol = text.matches(".*[^a-zA-Z0-9\\s].*");

        isPasswordValid = hasUpperCase && hasLowerCase && hasDigit && hasSymbol;
        String errorMsg = "Requiere: Mayús, minús, número y símbolo";
        updateErrorLabel(passwordError, isPasswordValid, errorMsg);
    }

    private void validateTfRPass(String text) {
        isRepeatPasswordMatching = !isTextEmpty(text) && text.equals(tfPass.getText());
        updateErrorLabel(repeatPasswordError, isRepeatPasswordMatching, "Las contraseñas no coinciden");
    }

    // -------------------------------------------------------------------------
    // --- MANEJADORES DE TEXT CHANGE (Validación en tiempo real) ---
    // -------------------------------------------------------------------------

    private void handleTfFNameTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfFName(newValue); checkGlobalValidation();
    }
    private void handleTfMNameTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfMName(newValue); checkGlobalValidation();
    }
    private void handleTfLNameTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfLName(newValue); checkGlobalValidation();
    }
    private void handleTfAddressTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfAddress(newValue); checkGlobalValidation();
    }
    private void handleTfCityTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfCity(newValue); checkGlobalValidation();
    }
    private void handleTfStateTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfState(newValue); checkGlobalValidation();
    }
    private void handleTfZipTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfZip(newValue); checkGlobalValidation();
    }
    private void handleTfPhoneTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfPhone(newValue); checkGlobalValidation();
    }
    private void handleTfEmailTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfEmail(newValue); checkGlobalValidation();
    }
    private void handleTfPassTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfPassword(newValue); validateTfRPass(tfRPass.getText()); checkGlobalValidation();
    }
    private void handleTfRPassTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfRPass(newValue); checkGlobalValidation();
    }

    // -------------------------------------------------------------------------
    // --- MANEJADORES DE FOCUS CHANGE (Validación al perder el foco) ---
    // -------------------------------------------------------------------------

    private void handleTfFNameFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue) {
        if (!newValue) { validateTfFName(tfFName.getText()); checkGlobalValidation(); }
    }
    private void handleTfMNameFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue) {
        if (!newValue) { validateTfMName(tfMName.getText()); checkGlobalValidation(); }
    }
    private void handleTfLNameFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue) {
        if (!newValue) { validateTfLName(tfLName.getText()); checkGlobalValidation(); }
    }
    private void handleTfAddressFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue) {
        if (!newValue) { validateTfAddress(tfAddress.getText()); checkGlobalValidation(); }
    }
    private void handleTfCityFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue) {
        if (!newValue) { validateTfCity(tfCity.getText()); checkGlobalValidation(); }
    }
    private void handleTfStateFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue) {
        if (!newValue) { validateTfState(tfState.getText()); checkGlobalValidation(); }
    }
    private void handleTfZipFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue) {
        if (!newValue) { validateTfZip(tfZip.getText()); checkGlobalValidation(); }
    }
    private void handleTfPhoneFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue) {
        if (!newValue) { validateTfPhone(tfPhone.getText()); checkGlobalValidation(); }
    }
    private void handleTfEmailFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue) {
        if (!newValue) { validateTfEmail(tfEmail.getText()); checkGlobalValidation(); }
    }
    private void handleTfPassFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue) {
        if (!newValue) { validateTfPassword(tfPass.getText()); checkGlobalValidation(); }
    }
    private void handleTfRPassFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue) {
        if (!newValue) { validateTfRPass(tfRPass.getText()); checkGlobalValidation(); }
    }

    // -------------------------------------------------------------------------
    // --- MANEJADORES DE BOTONES ---
    // -------------------------------------------------------------------------

    /**
     * @param event Manejador de evento al pulsar el botón CREATE ACCOUNT.
     */
    private void handleBtCreateOnAction(ActionEvent event) {
        setFieldsDisabled(true);
        btCreate.setDisable(true);

        try {
            LOGGER.log(Level.INFO, "Attempting to create a new customer account.");

            Customer customer = new Customer();
            customer.setFirstName(tfFName.getText());
            customer.setMiddleInitial(tfMName.getText());
            customer.setLastName(tfLName.getText());
            customer.setStreet(tfAddress.getText());
            customer.setCity(tfCity.getText());
            customer.setState(tfState.getText());

            // --- CORRECCIÓN DE TIPOS (ZIP y PHONE) ---
            customer.setZip(Integer.parseInt(tfZip.getText()));
            customer.setPhone(Long.parseLong(tfPhone.getText()));
            // ------------------------------------------

            customer.setEmail(tfEmail.getText());
            customer.setPassword(tfPass.getText());

            CustomerRESTClient client = new CustomerRESTClient();
            client.create_XML(customer);
            client.close();

            // SI LLEGAS AQUÍ, ES PORQUE EL SERVIDOR DIJO "ÉXITO"
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Registro Completo");
            successAlert.setHeaderText("¡Cuenta creada correctamente!");
            successAlert.setContentText("Volviendo a la ventana de Login.");
            successAlert.showAndWait();

            if (this.stage != null) {
                this.stage.close();
            }

        } catch (ForbiddenException e) {
            // USAMOS ForbiddenException
            LOGGER.log(Level.WARNING, "Creación fallida: Email ya registrado.", e);
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Error de Creación");
            alert.setHeaderText("El correo ya está registrado.");
            alert.setContentText("El correo proporcionado\nya está registrado en el sistema.");
            alert.showAndWait();
            setFieldsDisabled(false);
            checkGlobalValidation();

        } catch (InternalServerErrorException e) {
            // Manejo de 500
            LOGGER.log(Level.SEVERE, "Creación fallida: Error Interno del Servidor.", e);
            new Alert(Alert.AlertType.ERROR, "Error en el servidor. Intenta más tarde.").showAndWait();
            setFieldsDisabled(false);
            checkGlobalValidation();

        } catch (NumberFormatException nfe) {
            // Manejo de error de conversión de ZIP/Phone
            LOGGER.log(Level.WARNING, "Error de formato: El ZIP o Teléfono no son números válidos.", nfe);
            new Alert(Alert.AlertType.ERROR, "Por favor, introduzca solo dígitos en Código Postal y Teléfono.").showAndWait();
            setFieldsDisabled(false);
            checkGlobalValidation();

        } catch (Exception e) {
            // Manejo de otros errores (400 Bad Request, o error de conexión)
            LOGGER.log(Level.SEVERE, "Error inesperado al crear usuario: " + e.getMessage(), e);
            new Alert(Alert.AlertType.ERROR, "Datos inválidos\nO no se pudo conectar con el servidor.").showAndWait();
            setFieldsDisabled(false);
            checkGlobalValidation();
        }
    }

    /**
     * @param event Manejador de evento al pulsar el botón BACK.
     */
    private void handleBtBackOnAction(ActionEvent event) {
        boolean hasData = !isTextEmpty(tfFName.getText()) || !isTextEmpty(tfMName.getText()) ||
                !isTextEmpty(tfLName.getText()) || !isTextEmpty(tfEmail.getText()) ||
                !isTextEmpty(tfPass.getText()) || !isTextEmpty(tfAddress.getText()) ||
                !isTextEmpty(tfCity.getText()) || !isTextEmpty(tfState.getText()) ||
                !isTextEmpty(tfZip.getText()) || !isTextEmpty(tfPhone.getText());

        if (hasData) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmación para Volver");
            alert.setHeaderText("¿Deseas volver al Login?");
            alert.setContentText("Los datos introducidos no se guardarán.");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                this.stage.close(); // Cierra el Stage de esta ventana
            }
        } else {
            this.stage.close(); // Cierra el Stage de esta ventana
        }
    }

    /**
     * Método auxiliar para habilitar o deshabilitar todos los TextFields.
     */
    private void setFieldsDisabled(boolean disabled) {
        tfFName.setDisable(disabled);
        tfMName.setDisable(disabled);
        tfLName.setDisable(disabled);
        tfAddress.setDisable(disabled);
        tfCity.setDisable(disabled);
        tfState.setDisable(disabled);
        tfZip.setDisable(disabled);
        tfPhone.setDisable(disabled);
        tfEmail.setDisable(disabled);
        tfPass.setDisable(disabled);
        tfRPass.setDisable(disabled);
    }
}