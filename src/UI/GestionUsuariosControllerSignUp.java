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
 * Controlador (Controller) para la ventana de Registro de Usuario (Sign Up).
 *
 * <p>Esta clase gestiona la lógica de la interfaz de usuario para la vista
 * {@code SignUp.fxml}. Sus responsabilidades principales incluyen:</p>
 * <ul>
 * <li>Validar en tiempo real las entradas del usuario (nombre, email, contraseña, etc.)
 * utilizando patrones de expresiones regulares (Regex).</li>
 * <li>Mostrar mensajes de error específicos para cada campo inválido.</li>
 * <li>Habilitar el botón "Create Account" (Crear Cuenta) únicamente cuando todos
 * los campos del formulario son válidos.</li>
 * <li>Manejar el evento de creación de cuenta, comunicándose con un
 * {@link logic.CustomerRESTClient} para persistir el nuevo {@link model.Customer}.</li>
 * <li>Gestionar las excepciones que puedan ocurrir durante la llamada al servicio REST,
 * como emails duplicados ({@link javax.ws.rs.ForbiddenException}) o errores
 * del servidor ({@link javax.ws.rs.InternalServerErrorException}).</li>
 * <li>Manejar el evento del botón "Back" (Volver), mostrando una confirmación
 * si hay datos introducidos.</li>
 * </ul>
 *
 * @author pablo
 */
public class GestionUsuariosControllerSignUp {

    // --- FXML INJECTIONS (Campos de Texto) ---
    /**
     * Campo de texto FXML para el nombre (First Name).
     */
    @FXML private TextField tfFName;
    /**
     * Campo de texto FXML para la inicial del segundo nombre (Middle Name).
     */
    @FXML private TextField tfMName;
    /**
     * Campo de texto FXML para el apellido (Last Name).
     */
    @FXML private TextField tfLName;
    /**
     * Campo de texto FXML para la dirección (Street Address).
     */
    @FXML private TextField tfAddress;
    /**
     * Campo de texto FXML para la ciudad (City).
     */
    @FXML private TextField tfCity;
    /**
     * Campo de texto FXML para el estado/provincia (State).
     */
    @FXML private TextField tfState;
    /**
     * Campo de texto FXML para el código postal (Zip Code).
     */
    @FXML private TextField tfZip;
    /**
     * Campo de texto FXML para el número de teléfono (Phone).
     */
    @FXML private TextField tfPhone;
    /**
     * Campo de texto FXML para el correo electrónico (Email).
     */
    @FXML private TextField tfEmail;
    /**
     * Campo de texto FXML para la contraseña (Password).
     */
    @FXML private TextField tfPass;
    /**
     * Campo de texto FXML para la repetición de la contraseña (Repeat Password).
     */
    @FXML private TextField tfRPass;

    // --- FXML INJECTIONS (Botones) ---
    /**
     * Botón FXML para volver a la ventana anterior (Login).
     */
    @FXML private Button btBack;
    /**
     * Botón FXML para enviar el formulario de creación de cuenta.
     */
    @FXML private Button btCreate;

    // --- FXML INJECTIONS (Etiquetas de Error) ---
    /**
     * Etiqueta FXML para mostrar errores de validación del First Name.
     */
    @FXML private Label firstNameError;
    /**
     * Etiqueta FXML para mostrar errores de validación del Middle Name.
     */
    @FXML private Label middleNameError;
    /**
     * Etiqueta FXML para mostrar errores de validación del Last Name.
     */
    @FXML private Label lastNameError;
    /**
     * Etiqueta FXML para mostrar errores de validación de la Dirección.
     */
    @FXML private Label addressError;
    /**
     * Etiqueta FXML para mostrar errores de validación de la Ciudad.
     */
    @FXML private Label cityError;
    /**
     * Etiqueta FXML para mostrar errores de validación del Estado.
     */
    @FXML private Label stateError;
    /**
     * Etiqueta FXML para mostrar errores de validación del Código Postal.
     */
    @FXML private Label zipError;
    /**
     * Etiqueta FXML para mostrar errores de validación del Teléfono.
     */
    @FXML private Label phoneError;
    /**
     * Etiqueta FXML para mostrar errores de validación del Email.
     */
    @FXML private Label emailError;
    /**
     * Etiqueta FXML para mostrar errores de validación de la Contraseña.
     */
    @FXML private Label passwordError;
    /**
     * Etiqueta FXML para mostrar errores de validación de la repetición de Contraseña.
     */
    @FXML private Label repeatPasswordError;

    // --- ATRIBUTOS DE CLASE ---
    /**
     * Logger para registrar eventos y errores de esta clase.
     */
    private static final Logger LOGGER = Logger.getLogger("SignUpSignIn.SignUp");

    /**
     * El {@link Stage} (ventana) asociado a este controlador. Se utiliza para
     * gestionar la ventana (ej. cerrarla).
     */
    private Stage stage;

    // --- CONSTANTES DE VALIDACIÓN (De la base de datos) ---
    /**
     * Longitud mínima requerida para la contraseña.
     */
    private static final int MIN_PASSWORD_LENGTH = 8;
    /**
     * Longitud máxima permitida para la contraseña.
     */
    private static final int MAX_PASSWORD_LENGTH = 15;

    // --- PATRONES DE VALIDACIÓN (Regex) ---

    /**
     * Patrón Regex para validar nombres y apellidos.
     * Permite letras (incluyendo acentos y ñ), espacios, y una longitud de 1 a 40 caracteres.
     */
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-ZÁáÉéÍíÓóÚúñÑ\\s]{1,40}$");

    /**
     * Patrón Regex para validar la inicial del segundo nombre (Middle Name).
     * Requiere exactamente una letra seguida de un punto (ej. "A.").
     */
    private static final Pattern MNAME_PATTERN = Pattern.compile("^[a-zA-Z]\\.$");

    /**
     * Patrón Regex para validar direcciones.
     * Permite letras (con acentos), números, espacios y caracteres comunes en direcciones (.,-/ºª).
     * Longitud de 1 a 50 caracteres.
     */
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^[a-zA-ZÁáÉéÍíÓóÚúñÑ0-9.,\\-/ºª\\s]{1,50}$");

    /**
     * Patrón Regex para validar ciudades y estados.
     * Permite letras (con acentos) y espacios. Longitud de 1 a 20 caracteres.
     */
    private static final Pattern CITY_STATE_PATTERN = Pattern.compile("^[a-zA-ZÁáÉéÍíÓóÚúñÑ\\s]{1,20}$");

    /**
     * Patrón Regex para validar códigos postales (Zip).
     * Requiere exactamente 5 dígitos numéricos.
     */
    private static final Pattern ZIP_PATTERN = Pattern.compile("^\\d{5}$");

    /**
     * Patrón Regex para validar números de teléfono.
     * Requiere entre 9 y 15 dígitos numéricos.
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{9,15}$");

    /**
     * Patrón Regex para validar correos electrónicos.
     * Sigue un formato estándar de email (usuario@dominio.ext) y limita la parte
     * del usuario a 35 caracteres (para un total máx de 50).
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]{1,35}+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$");

    /**
     * Patrón Regex para validar la fortaleza de la contraseña.
     * Requiere:
     * <ul>
     * <li>Al menos una letra minúscula ({@code (?=.*[a-z])}).</li>
     * <li>Al menos una letra mayúscula ({@code (?=.*[A-Z])}).</li>
     * <li>Al menos un dígito ({@code (?=.*\\d)}).</li>
     * <li>Al menos un carácter especial (no alfanumérico ni espacio) ({@code (?=.*[^a-zA-Z0-9\\s])}).</li>
     * <li>Longitud entre {@link #MIN_PASSWORD_LENGTH} y {@link #MAX_PASSWORD_LENGTH} caracteres.</li>
     * </ul>
     */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9\\s]).{" + MIN_PASSWORD_LENGTH + "," + MAX_PASSWORD_LENGTH + "}$");


    // --- VARIABLES DE ESTADO PARA VALIDACIÓN ---
    /**
     * Flag que indica si el campo First Name es válido.
     */
    private boolean isFNameValid = false;
    /**
     * Flag que indica si el campo Middle Name es válido.
     */
    private boolean isMNameValid = false;
    /**
     * Flag que indica si el campo Last Name es válido.
     */
    private boolean isLNameValid = false;
    /**
     * Flag que indica si el campo Address es válido.
     */
    private boolean isAddressValid = false;
    /**
     * Flag que indica si el campo City es válido.
     */
    private boolean isCityValid = false;
    /**
     * Flag que indica si el campo State es válido.
     */
    private boolean isStateValid = false;
    /**
     * Flag que indica si el campo Zip es válido.
     */
    private boolean isZipValid = false;
    /**
     * Flag que indica si el campo Phone es válido.
     */
    private boolean isPhoneValid = false;
    /**
     * Flag que indica si el campo Email es válido.
     */
    private boolean isEmailValid = false;
    /**
     * Flag que indica si el campo Password es válido (cumple fortaleza).
     */
    private boolean isPasswordValid = false;
    /**
     * Flag que indica si el campo Repeat Password coincide con Password.
     */
    private boolean isRepeatPasswordMatching = false;

// ============================================================================
// MÉTODOS DE INICIALIZACIÓN
// ============================================================================

    /**
     * Inicializa el controlador utilizando un {@link Stage} existente.
     * <p>
     * Este método está diseñado para ser llamado cuando la vista de Sign-Up
     * se carga como parte de un flujo existente (ej. desde la ventana de Login),
     * reutilizando el {@link Stage} modal ya creado por el llamador.
     * </p>
     *
     * @param stage El {@link Stage} ya configurado (modal) desde el controlador padre.
     * @param root  El nodo {@link Parent} (raíz) de la escena FXML ya cargada.
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
            tfFName.requestFocus();

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
     * Inicializa el controlador creando un <strong>nuevo</strong> {@link Stage} modal.
     * <p>
     * Este método es el método de inicialización original, útil si la ventana
     * de Sign-Up se lanza de forma independiente. Crea su propia ventana modal
     * que es propiedad del {@code parentStage}.
     * </p>
     *
     * @param parentStage El {@link Stage} padre (propietario) de esta nueva ventana.
     * @param root        El nodo {@link Parent} (raíz) de la escena FXML.
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
     * Método auxiliar para configurar todos los manejadores de eventos (event handlers).
     * <p>
     * Asigna los métodos de esta clase a los eventos de los componentes FXML
     * (ej. {@code setOnAction} para botones, {@code textProperty().addListener}
     * y {@code focusedProperty().addListener} para campos de texto).
     * </p>
     * <p>
     * Esto centraliza la configuración de eventos y evita la duplicación de código
     * entre {@link #init(Stage, Parent)} y {@link #initFromLogin(Stage, Parent)}.
     * </p>
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
     * Aplica listeners a los campos de texto para limitar la longitud máxima.
     * <p>
     * Añade un {@code ChangeListener} a la propiedad de texto de cada
     * {@link TextField} que revierte el cambio si el nuevo valor excede
     * la longitud máxima definida (basada en las restricciones de la BD).
     * Esto previene que el usuario pueda escribir más caracteres de los permitidos.
     * </p>
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
     * Comprueba el estado de todas las variables booleanas de validación.
     * <p>
     * Habilita el botón {@link #btCreate} si todos los campos son válidos
     * ({@code isFNameValid && isMNameValid && ... && isRepeatPasswordMatching}).
     * Si algún campo es inválido, deshabilita el botón.
     * </p>
     */
    private void checkGlobalValidation() {
        boolean allFieldsValid = isFNameValid && isMNameValid && isLNameValid &&
                isAddressValid && isCityValid && isStateValid &&
                isZipValid && isPhoneValid && isEmailValid &&
                isPasswordValid && isRepeatPasswordMatching;

        btCreate.setDisable(!allFieldsValid);
    }

    /**
     * Comprueba si un texto es nulo, vacío o solo contiene espacios en blanco.
     *
     * @param text El {@link String} a comprobar.
     * @return {@code true} si el texto está vacío o es nulo, {@code false} en caso contrario.
     */
    private boolean isTextEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }

    /**
     * Método auxiliar para actualizar una etiqueta de error ({@link Label}).
     * <p>
     * Si {@code isValid} es {@code true}, el texto de la etiqueta se borra.
     * Si {@code isValid} es {@code false}, la etiqueta muestra el {@code errorMessage}.
     * </p>
     *
     * @param label        La {@link Label} de error a actualizar.
     * @param isValid      {@code true} si el campo asociado es válido, {@code false} si hay un error.
     * @param errorMessage El mensaje a mostrar si {@code isValid} es {@code false}.
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

    /**
     * Valida el campo First Name ({@link #tfFName}).
     * Comprueba que no esté vacío y que cumpla con {@link #NAME_PATTERN}.
     * Actualiza {@link #firstNameError} y {@link #isFNameValid}.
     *
     * @param text El contenido actual del campo tfFName.
     */
    private void validateTfFName(String text) {
        boolean isValid = !isTextEmpty(text) && NAME_PATTERN.matcher(text).matches();
        String errorMsg = isTextEmpty(text) ? "Campo obligatorio" : "Máx 40 caracteres, solo letras";
        updateErrorLabel(firstNameError, isValid, errorMsg);
        isFNameValid = isValid;
    }

    /**
     * Valida el campo Middle Name ({@link #tfMName}).
     * Comprueba que no esté vacío y que cumpla con {@link #MNAME_PATTERN} (ej. "A.").
     * Actualiza {@link #middleNameError} y {@link #isMNameValid}.
     *
     * @param text El contenido actual del campo tfMName.
     */
    private void validateTfMName(String text) {
        // MName es obligatorio y debe cumplir el patrón A.
        boolean isValid = !isTextEmpty(text) && MNAME_PATTERN.matcher(text).matches();
        String errorMsg = isTextEmpty(text) ? "Campo obligatorio" : "Formato: 'A.' (2 carac.)";
        updateErrorLabel(middleNameError, isValid, errorMsg);
        isMNameValid = isValid;
    }

    /**
     * Valida el campo Last Name ({@link #tfLName}).
     * Comprueba que no esté vacío y que cumpla con {@link #NAME_PATTERN}.
     * Actualiza {@link #lastNameError} y {@link #isLNameValid}.
     *
     * @param text El contenido actual del campo tfLName.
     */
    private void validateTfLName(String text) {
        boolean isValid = !isTextEmpty(text) && NAME_PATTERN.matcher(text).matches();
        String errorMsg = isTextEmpty(text) ? "Campo obligatorio" : "Máx 40 caracteres, solo letras";
        updateErrorLabel(lastNameError, isValid, errorMsg);
        isLNameValid = isValid;
    }

    /**
     * Valida el campo Address ({@link #tfAddress}).
     * Comprueba que no esté vacío y que cumpla con {@link #ADDRESS_PATTERN}.
     * Actualiza {@link #addressError} y {@link #isAddressValid}.
     *
     * @param text El contenido actual del campo tfAddress.
     */
    private void validateTfAddress(String text) {
        boolean isValid = !isTextEmpty(text) && ADDRESS_PATTERN.matcher(text).matches();
        String errorMsg = isTextEmpty(text) ? "Campo obligatorio" : "Máx 50 caracteres. Caracteres inválidos";
        updateErrorLabel(addressError, isValid, errorMsg);
        isAddressValid = isValid;
    }

    /**
     * Valida el campo City ({@link #tfCity}).
     * Comprueba que no esté vacío y que cumpla con {@link #CITY_STATE_PATTERN}.
     * Actualiza {@link #cityError} y {@link #isCityValid}.
     *
     * @param text El contenido actual del campo tfCity.
     */
    private void validateTfCity(String text) {
        // Usa el patrón de Ciudad/Estado
        boolean isValid = !isTextEmpty(text) && CITY_STATE_PATTERN.matcher(text).matches();
        String errorMsg = isTextEmpty(text) ? "Campo obligatorio" : "Máx 20 caracteres, solo letras";
        updateErrorLabel(cityError, isValid, errorMsg);
        isCityValid = isValid;
    }

    /**
     * Valida el campo State ({@link #tfState}).
     * Comprueba que no esté vacío y que cumpla con {@link #CITY_STATE_PATTERN}.
     * Actualiza {@link #stateError} y {@link #isStateValid}.
     *
     * @param text El contenido actual del campo tfState.
     */
    private void validateTfState(String text) {
        // Usa el patrón de Ciudad/Estado
        boolean isValid = !isTextEmpty(text) && CITY_STATE_PATTERN.matcher(text).matches();
        String errorMsg = isTextEmpty(text) ? "Campo obligatorio" : "Máx 20 caracteres, solo letras";
        updateErrorLabel(stateError, isValid, errorMsg);
        isStateValid = isValid;
    }

    /**
     * Valida el campo Zip ({@link #tfZip}).
     * Comprueba que no esté vacío y que cumpla con {@link #ZIP_PATTERN} (5 dígitos).
     * Actualiza {@link #zipError} y {@link #isZipValid}.
     *
     * @param text El contenido actual del campo tfZip.
     */
    private void validateTfZip(String text) {
        boolean isValid = !isTextEmpty(text) && ZIP_PATTERN.matcher(text).matches();
        String errorMsg = isTextEmpty(text) ? "Campo obligatorio" : "Debe tener exactamente 5 dígitos";
        updateErrorLabel(zipError, isValid, errorMsg);
        isZipValid = isValid;
    }

    /**
     * Valida el campo Phone ({@link #tfPhone}).
     * Comprueba que no esté vacío y que cumpla con {@link #PHONE_PATTERN} (9-15 dígitos).
     * Actualiza {@link #phoneError} y {@link #isPhoneValid}.
     *
     * @param text El contenido actual del campo tfPhone.
     */
    private void validateTfPhone(String text) {
        boolean isValid = !isTextEmpty(text) && PHONE_PATTERN.matcher(text).matches();
        String errorMsg = isTextEmpty(text) ? "Campo obligatorio" : "Mínimo 9 dígitos";
        updateErrorLabel(phoneError, isValid, errorMsg);
        isPhoneValid = isValid;
    }

    /**
     * Valida el campo Email ({@link #tfEmail}).
     * Comprueba que no esté vacío y que cumpla con {@link #EMAIL_PATTERN}.
     * Actualiza {@link #emailError} y {@link #isEmailValid}.
     *
     * @param text El contenido actual del campo tfEmail.
     */
    private void validateTfEmail(String text) {
        // La longitud se maneja en el Listener de longitud, aquí validamos el formato.
        boolean isValid = !isTextEmpty(text) && EMAIL_PATTERN.matcher(text).matches();
        String errorMsg = isTextEmpty(text) ? "Campo obligatorio" : "Formato de email inválido (Máx 50)";
        updateErrorLabel(emailError, isValid, errorMsg);
        isEmailValid = isValid;
    }

    /**
     * Valida el campo Password ({@link #tfPass}).
     * Comprueba que no esté vacío y que cumpla con los requisitos de fortaleza
     * definidos en {@link #PASSWORD_PATTERN} (longitud, mayús, minús, núm, símb).
     * Actualiza {@link #passwordError} y {@link #isPasswordValid}.
     *
     * @param text El contenido actual del campo tfPass.
     */
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

    /**
     * Valida el campo Repeat Password ({@link #tfRPass}).
     * Comprueba que no esté vacío y que su contenido sea idéntico al de {@link #tfPass}.
     * Actualiza {@link #repeatPasswordError} y {@link #isRepeatPasswordMatching}.
     *
     * @param text El contenido actual del campo tfRPass.
     */
    private void validateTfRPass(String text) {
        isRepeatPasswordMatching = !isTextEmpty(text) && text.equals(tfPass.getText());
        updateErrorLabel(repeatPasswordError, isRepeatPasswordMatching, "Las contraseñas no coinciden");
    }

    // -------------------------------------------------------------------------
    // --- MANEJADORES DE TEXT CHANGE (Validación en tiempo real) ---
    // -------------------------------------------------------------------------

    /**
     * Manejador para cambios en el texto de {@link #tfFName}.
     * Llama a {@link #validateTfFName(String)} y {@link #checkGlobalValidation()}.
     */
    private void handleTfFNameTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfFName(newValue); checkGlobalValidation();
    }
    /**
     * Manejador para cambios en el texto de {@link #tfMName}.
     * Llama a {@link #validateTfMName(String)} y {@link #checkGlobalValidation()}.
     */
    private void handleTfMNameTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfMName(newValue); checkGlobalValidation();
    }
    /**
     * Manejador para cambios en el texto de {@link #tfLName}.
     * Llama a {@link #validateTfLName(String)} y {@link #checkGlobalValidation()}.
     */
    private void handleTfLNameTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfLName(newValue); checkGlobalValidation();
    }
    /**
     * Manejador para cambios en el texto de {@link #tfAddress}.
     * Llama a {@link #validateTfAddress(String)} y {@link #checkGlobalValidation()}.
     */
    private void handleTfAddressTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfAddress(newValue); checkGlobalValidation();
    }
    /**
     * Manejador para cambios en el texto de {@link #tfCity}.
     * Llama a {@link #validateTfCity(String)} y {@link #checkGlobalValidation()}.
     */
    private void handleTfCityTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfCity(newValue); checkGlobalValidation();
    }
    /**
     * Manejador para cambios en el texto de {@link #tfState}.
     * Llama a {@link #validateTfState(String)} y {@link #checkGlobalValidation()}.
     */
    private void handleTfStateTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfState(newValue); checkGlobalValidation();
    }
    /**
     * Manejador para cambios en el texto de {@link #tfZip}.
     * Llama a {@link #validateTfZip(String)} y {@link #checkGlobalValidation()}.
     */
    private void handleTfZipTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfZip(newValue); checkGlobalValidation();
    }
    /**
     * Manejador para cambios en el texto de {@link #tfPhone}.
     * Llama a {@link #validateTfPhone(String)} y {@link #checkGlobalValidation()}.
     */
    private void handleTfPhoneTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfPhone(newValue); checkGlobalValidation();
    }
    /**
     * Manejador para cambios en el texto de {@link #tfEmail}.
     * Llama a {@link #validateTfEmail(String)} y {@link #checkGlobalValidation()}.
     */
    private void handleTfEmailTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfEmail(newValue); checkGlobalValidation();
    }
    /**
     * Manejador para cambios en el texto de {@link #tfPass}.
     * Llama a {@link #validateTfPassword(String)}, {@link #validateTfRPass(String)}
     * (para re-validar la coincidencia) y {@link #checkGlobalValidation()}.
     */
    private void handleTfPassTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfPassword(newValue); validateTfRPass(tfRPass.getText()); checkGlobalValidation();
    }
    /**
     * Manejador para cambios en el texto de {@link #tfRPass}.
     * Llama a {@link #validateTfRPass(String)} y {@link #checkGlobalValidation()}.
     */
    private void handleTfRPassTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfRPass(newValue); checkGlobalValidation();
    }

    // -------------------------------------------------------------------------
    // --- MANEJADORES DE FOCUS CHANGE (Validación al perder el foco) ---
    // -------------------------------------------------------------------------

    /**
     * Manejador para la pérdida de foco de {@link #tfFName}.
     * Fuerza la validación si el usuario abandona el campo.
     *
     * @param observable El valor observable (no usado).
     * @param oldValue El valor de foco anterior (no usado).
     * @param newValue {@code false} si el campo pierde el foco.
     */
    private void handleTfFNameFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue) {
        if (!newValue) { validateTfFName(tfFName.getText()); checkGlobalValidation(); }
    }
    /**
     * Manejador para la pérdida de foco de {@link #tfMName}.
     */
    private void handleTfMNameFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue) {
        if (!newValue) { validateTfMName(tfMName.getText()); checkGlobalValidation(); }
    }
    /**
     * Manejador para la pérdida de foco de {@link #tfLName}.
     */
    private void handleTfLNameFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue) {
        if (!newValue) { validateTfLName(tfLName.getText()); checkGlobalValidation(); }
    }
    /**
     * Manejador para la pérdida de foco de {@link #tfAddress}.
     */
    private void handleTfAddressFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue) {
        if (!newValue) { validateTfAddress(tfAddress.getText()); checkGlobalValidation(); }
    }
    /**
     * Manejador para la pérdida de foco de {@link #tfCity}.
     */
    private void handleTfCityFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue) {
        if (!newValue) { validateTfCity(tfCity.getText()); checkGlobalValidation(); }
    }
    /**
     * Manejador para la pérdida de foco de {@link #tfState}.
     */
    private void handleTfStateFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue) {
        if (!newValue) { validateTfState(tfState.getText()); checkGlobalValidation(); }
    }
    /**
     * Manejador para la pérdida de foco de {@link #tfZip}.
     */
    private void handleTfZipFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue) {
        if (!newValue) { validateTfZip(tfZip.getText()); checkGlobalValidation(); }
    }
    /**
     * Manejador para la pérdida de foco de {@link #tfPhone}.
     */
    private void handleTfPhoneFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue) {
        if (!newValue) { validateTfPhone(tfPhone.getText()); checkGlobalValidation(); }
    }
    /**
     * Manejador para la pérdida de foco de {@link #tfEmail}.
     */
    private void handleTfEmailFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue) {
        if (!newValue) { validateTfEmail(tfEmail.getText()); checkGlobalValidation(); }
    }
    /**
     * Manejador para la pérdida de foco de {@link #tfPass}.
     */
    private void handleTfPassFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue) {
        if (!newValue) { validateTfPassword(tfPass.getText()); checkGlobalValidation(); }
    }
    /**
     * Manejador para la pérdida de foco de {@link #tfRPass}.
     */
    private void handleTfRPassFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue) {
        if (!newValue) { validateTfRPass(tfRPass.getText()); checkGlobalValidation(); }
    }

    // -------------------------------------------------------------------------
    // --- MANEJADORES DE BOTONES ---
    // -------------------------------------------------------------------------

    /**
     * Manejador de evento para el botón "CREATE ACCOUNT" ({@link #btCreate}).
     * <p>
     * 1. Deshabilita los campos para prevenir ediciones durante la petición.
     * 2. Recoge los datos de los {@link TextField}.
     * 3. Convierte Zip y Phone a los tipos numéricos esperados.
     * 4. Crea un objeto {@link Customer}.
     * 5. Llama al método {@code create_XML} del {@link CustomerRESTClient}.
     * 6. Si tiene éxito, muestra una alerta informativa y cierra la ventana.
     * 7. Si falla, captura excepciones específicas (ej. {@link ForbiddenException}
     * para email duplicado, {@link InternalServerErrorException} para errores 500)
     * y muestra alertas de error al usuario.
     * 8. Vuelve a habilitar los campos si la operación falló.
     * </p>
     *
     * @param event El {@link ActionEvent} generado por el clic del botón.
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
     * Manejador de evento para el botón "BACK" ({@link #btBack}).
     * <p>
     * Comprueba si alguno de los campos de texto tiene contenido.
     * <ul>
     * <li>Si hay datos, muestra una {@link Alert} de confirmación
     * preguntando al usuario si desea descartar los cambios. Si el usuario
     * acepta (OK), cierra la ventana.</li>
     * <li>Si no hay datos, cierra la ventana ({@link #stage}) inmediatamente.</li>
     * </ul>
     * </p>
     *
     * @param event El {@link ActionEvent} generado por el clic del botón.
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
     * Método auxiliar para habilitar o deshabilitar todos los campos de texto
     * del formulario.
     * <p>
     * Se utiliza para prevenir que el usuario modifique los datos mientras se
     * está procesando la solicitud de creación de cuenta en el servidor.
     * </p>
     *
     * @param disabled {@code true} para deshabilitar todos los campos,
     * {@code false} para habilitarlos.
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