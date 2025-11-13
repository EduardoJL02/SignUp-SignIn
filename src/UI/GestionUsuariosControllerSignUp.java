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
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.logging.Logger;
import java.util.logging.Level;

// Imports de JAX-RS
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.ForbiddenException;

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

    // --- CONSTANTES DE VALIDACIÓN (De la base de datos) ---
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 15;
    
    // --- PATRONES DE VALIDACIÓN ACTUALIZADOS CON RESTRICCIONES DE LONGITUD ---
    // FName, LName (Máx 40 caracteres)
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-ZÁáÉéÍíÓóÚúñÑ\\s]{1,40}$");
    
    // MName (Máx 2 caracteres: Letra y punto)
    private static final Pattern MNAME_PATTERN = Pattern.compile("^[a-zA-Z]\\.$");
    
    // Address (Máx 50 caracteres)
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^[a-zA-ZÁáÉéÍíÓóÚúñÑ0-9.,\\-/ºª\\s]{1,50}$");
    
    // City, State (Máx 20 caracteres) - Usaremos un patrón para letras con límite 20
    private static final Pattern CITY_STATE_PATTERN = Pattern.compile("^[a-zA-ZÁáÉéÍíÓóÚúñÑ\\s]{1,20}$"); 
    
    // Zip (Exactamente 5 dígitos)
    private static final Pattern ZIP_PATTERN = Pattern.compile("^\\d{5}$");
    
    // Phone (Mín 9, Máx 15 dígitos - Por seguridad, la base de datos es 9)
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{9,15}$"); 
    
    // Email (Máx 50 caracteres - La longitud se controlará principalmente por el String.length() 
    // y el TextField max length, la regex valida el formato).
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]{1,35}+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$");
    
    // Contraseña (Min 8, Max 15, requiere Mayús, minús, número y símbolo)
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9\\s]).{" + MIN_PASSWORD_LENGTH + "," + MAX_PASSWORD_LENGTH + "}$");


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
     * * @param stage El Stage ya configurado (modal) desde Login
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
        
        // Aplicar listener de longitud máxima
        applyTextLengthLimiters();


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
        
        // Aplicar listener de longitud máxima
        applyTextLengthLimiters();

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

/**
 * Aplica un Listener para truncar el texto si excede la longitud máxima.
 * Esto evita que el usuario pueda introducir más de lo permitido por la base de datos.
 */
private void applyTextLengthLimiters() {
    // FName (40)
    tfFName.textProperty().addListener((observable, oldValue, newValue) -> {
        if (newValue != null && newValue.length() > 40) {
            tfFName.setText(oldValue);
        }
    });
    // MName (2)
    tfMName.textProperty().addListener((observable, oldValue, newValue) -> {
        if (newValue != null && newValue.length() > 2) {
            tfMName.setText(oldValue);
        }
    });
    // LName (40)
    tfLName.textProperty().addListener((observable, oldValue, newValue) -> {
        if (newValue != null && newValue.length() > 40) {
            tfLName.setText(oldValue);
        }
    });
    // Address (50)
    tfAddress.textProperty().addListener((observable, oldValue, newValue) -> {
        if (newValue != null && newValue.length() > 50) {
            tfAddress.setText(oldValue);
        }
    });
    // City (20)
    tfCity.textProperty().addListener((observable, oldValue, newValue) -> {
        if (newValue != null && newValue.length() > 20) {
            tfCity.setText(oldValue);
        }
    });
    // State (20)
    tfState.textProperty().addListener((observable, oldValue, newValue) -> {
        if (newValue != null && newValue.length() > 20) {
            tfState.setText(oldValue);
        }
    });
    // Zip (5)
    tfZip.textProperty().addListener((observable, oldValue, newValue) -> {
        if (newValue != null && newValue.length() > 5) {
            tfZip.setText(oldValue);
        }
    });
    // Phone (9 - Usaremos 15 como límite superior en el UI, aunque la validación pide 9 mínimo)
    tfPhone.textProperty().addListener((observable, oldValue, newValue) -> {
        if (newValue != null && newValue.length() > 15) {
            tfPhone.setText(oldValue);
        }
    });
    // Email (50)
    tfEmail.textProperty().addListener((observable, oldValue, newValue) -> {
        if (newValue != null && newValue.length() > 50) {
            tfEmail.setText(oldValue);
        }
    });
    // Pass/RPass (15)
    tfPass.textProperty().addListener((observable, oldValue, newValue) -> {
        if (newValue != null && newValue.length() > MAX_PASSWORD_LENGTH) {
            tfPass.setText(oldValue);
        }
    });
    tfRPass.textProperty().addListener((observable, oldValue, newValue) -> {
        if (newValue != null && newValue.length() > MAX_PASSWORD_LENGTH) {
            tfRPass.setText(oldValue);
        }
    });
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
        boolean isValid = !isTextEmpty(text) && NAME_PATTERN.matcher(text).matches();
        String errorMsg = isTextEmpty(text) ? "Campo obligatorio" : "Máx 40 caracteres, solo letras";
        updateErrorLabel(firstNameError, isValid, errorMsg);
        isFNameValid = isValid;
    }

    private void validateTfMName(String text) {
        // MName es obligatorio y debe cumplir el patrón A.
        boolean isValid = !isTextEmpty(text) && MNAME_PATTERN.matcher(text).matches();
        String errorMsg = isTextEmpty(text) ? "Campo obligatorio" : "Formato: 'A.' (2 carac.)";
        updateErrorLabel(middleNameError, isValid, errorMsg);
        isMNameValid = isValid;
    }

    private void validateTfLName(String text) {
        boolean isValid = !isTextEmpty(text) && NAME_PATTERN.matcher(text).matches();
        String errorMsg = isTextEmpty(text) ? "Campo obligatorio" : "Máx 40 caracteres, solo letras";
        updateErrorLabel(lastNameError, isValid, errorMsg);
        isLNameValid = isValid;
    }

    private void validateTfAddress(String text) {
        boolean isValid = !isTextEmpty(text) && ADDRESS_PATTERN.matcher(text).matches();
        String errorMsg = isTextEmpty(text) ? "Campo obligatorio" : "Máx 50 caracteres. Caracteres inválidos";
        updateErrorLabel(addressError, isValid, errorMsg);
        isAddressValid = isValid;
    }

    private void validateTfCity(String text) {
        // Usa el patrón de Ciudad/Estado
        boolean isValid = !isTextEmpty(text) && CITY_STATE_PATTERN.matcher(text).matches();
        String errorMsg = isTextEmpty(text) ? "Campo obligatorio" : "Máx 20 caracteres, solo letras";
        updateErrorLabel(cityError, isValid, errorMsg);
        isCityValid = isValid;
    }

    private void validateTfState(String text) {
        // Usa el patrón de Ciudad/Estado
        boolean isValid = !isTextEmpty(text) && CITY_STATE_PATTERN.matcher(text).matches();
        String errorMsg = isTextEmpty(text) ? "Campo obligatorio" : "Máx 20 caracteres, solo letras";
        updateErrorLabel(stateError, isValid, errorMsg);
        isStateValid = isValid;
    }

    private void validateTfZip(String text) {
        boolean isValid = !isTextEmpty(text) && ZIP_PATTERN.matcher(text).matches();
        String errorMsg = isTextEmpty(text) ? "Campo obligatorio" : "Debe tener exactamente 5 dígitos";
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
        // La longitud se maneja en el Listener de longitud, aquí validamos el formato.
        boolean isValid = !isTextEmpty(text) && EMAIL_PATTERN.matcher(text).matches();
        String errorMsg = isTextEmpty(text) ? "Campo obligatorio" : "Formato de email inválido (Máx 50)";
        updateErrorLabel(emailError, isValid, errorMsg);
        isEmailValid = isValid;
    }

    private void validateTfPassword(String text) {
        // Usamos la regex robusta para la validación de complejidad y longitud.
        boolean isValid = !isTextEmpty(text) && PASSWORD_PATTERN.matcher(text).matches();
        
        if (isTextEmpty(text)) {
            updateErrorLabel(passwordError, false, "Campo obligatorio");
        } else if (text.length() < MIN_PASSWORD_LENGTH) {
             updateErrorLabel(passwordError, false, "Mínimo " + MIN_PASSWORD_LENGTH + " caracteres");
        } else if (text.length() > MAX_PASSWORD_LENGTH) {
             updateErrorLabel(passwordError, false, "Máximo " + MAX_PASSWORD_LENGTH + " caracteres");
        } else if (!isValid) {
            updateErrorLabel(passwordError, false, "Requiere: Mayús, minús, número y símbolo");
        } else {
            updateErrorLabel(passwordError, true, "");
        }
        
        isPasswordValid = isValid;
    }

    private void validateTfRPass(String text) {
        isRepeatPasswordMatching = !isTextEmpty(text) && text.equals(tfPass.getText());
        updateErrorLabel(repeatPasswordError, isRepeatPasswordMatching, "Las contraseñas no coinciden");
    }

    // -------------------------------------------------------------------------
    // --- MANEJADORES DE TEXT CHANGE (Validación en tiempo real) ---
    // -------------------------------------------------------------------------
    // NOTA: Se ha añadido la comprobación de longitud en applyTextLengthLimiters()
    // para evitar que el usuario pueda escribir más de la longitud máxima.

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
    
    // Se mantienen igual, ya que solo fuerzan la validación cuando el usuario cambia de campo.

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
            // Asumiendo que el campo zip es un entero de 5 dígitos
            customer.setZip(Integer.parseInt(tfZip.getText()));
            // Asumiendo que el campo phone es un Long
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
            // USAMOS ForbiddenException (o ConflictException, si es un 409)
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
            new Alert(Alert.AlertType.ERROR, "Por favor, introduzca solo dígitos\nEn Código Postal y Teléfono.").showAndWait();
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