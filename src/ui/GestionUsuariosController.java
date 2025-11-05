/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ui;

import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;
import logic.CustomerRESTClient;
import model.Customer;
import java.util.Optional; 
import javafx.scene.control.ButtonType;
import java.util.regex.Pattern; // Para una mejor gestión de expresiones regulares

/**
 * Controlador para la ventana de Registro (CREATE ACCOUNT)
 *
 * @author pablo
 */
public class GestionUsuariosController {
    // --- FXML INYECTIONS ---
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
    
    // --- ATRIBUTOS DE CLASE ---
    private static final Logger LOGGER = Logger.getLogger("ui");
    private Stage stage;

    // --- CONSTANTES DE VALIDACIÓN ---
    private final int MIN_PASSWORD_LENGTH = 8;
    private final int ZIP_LENGTH = 5;
    private final int PHONE_MIN_LENGTH = 9;

    // Patrones de validación
    private static final Pattern LETTER_PATTERN = Pattern.compile("^[a-zA-ZÁáÉéÍíÓóÚúñÑ\\s]+$");
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^[a-zA-Z0-9.,\\-/\\s]+$");
    private static final Pattern ZIP_PATTERN = Pattern.compile("^\\d{5}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{9,}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$");

    // --- VARIABLES DE ESTADO PARA VALIDACIÓN (Todos los campos son obligatorios) [cite: 38] ---
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
    
    // ------------------------------------------

    /**
     * Inicializa la ventana y configura manejadores de eventos y propiedades.
     * @param stage El Stage principal.
     * @param root El Parent (root node) cargado desde el FXML.
     */
    public void init(Stage parentStage, Parent root) {
    try {
        LOGGER.log(Level.INFO, "Initializing SignUp (CREATE ACCOUNT)");

        Scene scene = new Scene(root);
        
        // --- CORRECCIÓN DEL ERROR DE MODALIDAD ---
        // 1. Crear un NUEVO Stage para la ventana de registro
        Stage dialogStage = new Stage();
        this.stage = dialogStage; // Guarda la referencia del nuevo Stage
        
        // 2. Establecer el Stage principal (parentStage) como propietario
        dialogStage.initOwner(parentStage); 
        
        // 3. Establecer la modalidad en el NUEVO Stage
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        // ----------------------------------------
        
        dialogStage.setScene(scene);
        dialogStage.setTitle("CREATE ACCOUNT");
        dialogStage.setResizable(false); // La ventana no debe ser redimensionable
        
        // Estado inicial de los botones
        btBack.setDisable(false); // BACK habilitado
        btCreate.setDisable(true); // CREATE ACCOUNT deshabilitado inicialmente
        
        // Foco inicial en el campo Nombre
        tfFName.requestFocus();
        
        // Asignación de manejadores a eventos y properties
        btCreate.setOnAction(this::handleBtCreateOnAction);
        btBack.setOnAction(this::handleBtBackOnAction);

        // Configuramos los listeners para todos los campos obligatorios
        // FNAME
        tfFName.textProperty().addListener(this::handleTfFNameTextChange);
        tfFName.focusedProperty().addListener(this::handleTfFNameFocusChange);
        // MIDDLE NAME
        tfMName.textProperty().addListener(this::handleTfMNameTextChange);
        tfMName.focusedProperty().addListener(this::handleTfMNameFocusChange);
        // LAST NAME
        tfLName.textProperty().addListener(this::handleTfLNameTextChange);
        tfLName.focusedProperty().addListener(this::handleTfLNameFocusChange);
        // ADDRESS
        tfAddress.textProperty().addListener(this::handleTfAddressTextChange);
        tfAddress.focusedProperty().addListener(this::handleTfAddressFocusChange);
        // CITY
        tfCity.textProperty().addListener(this::handleTfCityTextChange);
        tfCity.focusedProperty().addListener(this::handleTfCityFocusChange);
        // STATE
        tfState.textProperty().addListener(this::handleTfStateTextChange);
        tfState.focusedProperty().addListener(this::handleTfStateFocusChange);
        // ZIP
        tfZip.textProperty().addListener(this::handleTfZipTextChange);
        tfZip.focusedProperty().addListener(this::handleTfZipFocusChange);
        // PHONE
        tfPhone.textProperty().addListener(this::handleTfPhoneTextChange);
        tfPhone.focusedProperty().addListener(this::handleTfPhoneFocusChange);
        // EMAIL
        tfEmail.textProperty().addListener(this::handleTfEmailTextChange);
        tfEmail.focusedProperty().addListener(this::handleTfEmailFocusChange);
        // PASSWORD
        tfPass.textProperty().addListener(this::handleTfPassTextChange);
        tfPass.focusedProperty().addListener(this::handleTfPassFocusChange);
        // REPEAT PASSWORD
        tfRPass.textProperty().addListener(this::handleTfRPassTextChange);
        tfRPass.focusedProperty().addListener(this::handleTfRPassFocusChange);
        

        // Mostrar la ventana
        dialogStage.show(); // Mostrar el NUEVO Stage

    } catch (Exception e) {
        // Si se produce cualquier excepción durante la inicialización, mostrar un mensaje [cite: 34]
        LOGGER.log(Level.SEVERE, "Error fatal al inicializar la ventana SignUp", e);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error de Aplicación");
        alert.setHeaderText("No se pudo cargar la ventana de registro.");
        alert.setContentText("Ocurrió un error inesperado: " + e.getMessage());
        alert.showAndWait();
    }
}
    
    // -------------------------------------------------------------------------
    // --- LÓGICA DE VALIDACIÓN CENTRAL ---
    // -------------------------------------------------------------------------

    /**
     * Revisa el estado de validez de todos los campos obligatorios [cite: 38]
     * y habilita/deshabilita el botón CREATE ACCOUNT.
     * Regla: El botón solo se habilita cuando todos los campos son válidos y las contraseñas coinciden. [cite: 38]
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
     * @param text El texto a validar.
     * @return true si está vacío.
     */
    private boolean isTextEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }
    
    // -------------------------------------------------------------------------
    // --- MÉTODOS DE VALIDACIÓN POR CAMPO [cite: 34, 35] ---
    // -------------------------------------------------------------------------

    // Nombre (tfFName): No vacío y solo letras [cite: 34]
    private void validateTfFName(String text) {
        if (isTextEmpty(text)) {
            // Mostrar error inline: "Campo obligatorio"
            isFNameValid = false;
        } else if (!LETTER_PATTERN.matcher(text).matches()) {
            // Mostrar error inline: "Solo letras permitidas"
            isFNameValid = false;
        } else {
            isFNameValid = true;
        }
    }

    // Segundo Nombre (tfMName): No vacío y Letra y punto (ejemplo 'J.') [cite: 35]
    private void validateTfMName(String text) {
        if (isTextEmpty(text)) {
            // Mostrar error inline: "Campo obligatorio" [cite: 35]
            isMNameValid = false;
        } else if (!text.matches("^[a-zA-Z]\\.$")) { // Regex para una letra seguida de un punto
            // Mostrar error inline: "Formato: Letra y punto, ejemplo 'J.'" [cite: 35]
            isMNameValid = false;
        } else {
            isMNameValid = true;
        }
    }
    
    // Apellido (tfLName): No vacío y solo letras [cite: 35]
    private void validateTfLName(String text) {
        if (isTextEmpty(text)) {
            // Mostrar error inline: "Campo obligatorio"
            isLNameValid = false;
        } else if (!LETTER_PATTERN.matcher(text).matches()) {
            // Mostrar error inline: "Solo letras permitidas"
            isLNameValid = false;
        } else {
            isLNameValid = true;
        }
    }
    
    // Dirección (tfAddress): No vacío y letras, números, puntuación básica (., - /) [cite: 35]
    private void validateTfAddress(String text) {
        if (isTextEmpty(text)) {
            // Mostrar error inline: "Campo obligatorio"
            isAddressValid = false;
        } else if (!ADDRESS_PATTERN.matcher(text).matches()) {
            // Mostrar error inline: "Dirección inválida"
            isAddressValid = false;
        } else {
            isAddressValid = true;
        }
    }

    // Ciudad (tfCity): No vacío y solo letras [cite: 35]
    private void validateTfCity(String text) {
        if (isTextEmpty(text)) {
            // Mostrar error inline: "Ciudad obligatoria" [cite: 35]
            isCityValid = false;
        } else if (!LETTER_PATTERN.matcher(text).matches()) {
            // Mostrar error inline: "Solo letras"
            isCityValid = false;
        } else {
            isCityValid = true;
        }
    }
    
    // Estado (tfState): No vacío y solo letras o abreviaturas válidas [cite: 35]
    private void validateTfState(String text) {
        if (isTextEmpty(text)) {
            // Mostrar error inline: "Estado obligatorio" [cite: 35]
            isStateValid = false;
        } else if (!LETTER_PATTERN.matcher(text).matches() && !text.matches("^[A-Z]{2}$")) {
            // Se asume que una abreviatura válida son 2 mayúsculas, además de la regla de solo letras
            isStateValid = false; 
        } else {
            isStateValid = true;
        }
    }

    // Código Postal (tfZip): Numérico y longitud (ej. 5 dígitos) [cite: 35]
    private void validateTfZip(String text) {
        if (isTextEmpty(text) || !ZIP_PATTERN.matcher(text).matches()) {
            // Mostrar error inline: "Código postal inválido" [cite: 35]
            isZipValid = false;
        } else {
            isZipValid = true;
        }
    }

    // Teléfono (tfPhone): Numérico y longitud mínima (ej. 9 dígitos) [cite: 35]
    private void validateTfPhone(String text) {
        if (isTextEmpty(text) || !PHONE_PATTERN.matcher(text).matches() || text.length() < PHONE_MIN_LENGTH) {
            // Mostrar error inline: "Número de teléfono inválido" [cite: 35]
            isPhoneValid = false;
        } else {
            isPhoneValid = true;
        }
    }
    
    // Email (tfEmail): Formato de correo electrónico válido [cite: 35]
    private void validateTfEmail(String text) {
        if (isTextEmpty(text) || !EMAIL_PATTERN.matcher(text).matches()) {
            // Mostrar error inline: "Formato de email no válido" [cite: 35]
            isEmailValid = false;
        } else {
            isEmailValid = true;
        }
    }

    // Contraseña (tfPass): Longitud mínima, mayúsculas, minúsculas, número y símbolo [cite: 35]
    private void validateTfPassword(String text) {
        if (isTextEmpty(text) || text.length() < MIN_PASSWORD_LENGTH) {
            isPasswordValid = false;
            // Actualizar barra a débil/rojo [cite: 35]
            return;
        }
        
        // Validación de complejidad (requiere mayúsculas, minúsculas, número y símbolo) [cite: 35]
        boolean hasUpperCase = text.matches(".*[A-Z].*");
        boolean hasLowerCase = text.matches(".*[a-z].*");
        boolean hasDigit = text.matches(".*[0-9].*");
        boolean hasSymbol = text.matches(".*[^a-zA-Z0-9\\s].*");

        if (hasUpperCase && hasLowerCase && hasDigit && hasSymbol) {
            isPasswordValid = true;
            // Actualizar barra de progreso a fuerte (verde) [cite: 35]
        } else {
            isPasswordValid = false;
            // Actualizar barra de progreso a media/débil [cite: 35]
        }
    }

    // Repetir Contraseña (tfRPass): Coincidir exactamente con tfPass [cite: 35]
    private void validateTfRPass(String text) {
        if (isTextEmpty(text) || !text.equals(tfPass.getText())) {
            // Mostrar error inline: "Las contraseñas no coinciden" [cite: 35]
            isRepeatPasswordMatching = false;
        } else {
            isRepeatPasswordMatching = true;
            // La barra de progreso se mantiene sincronizada [cite: 35]
        }
    }
    
    // -------------------------------------------------------------------------
    // --- MANEJADORES DE TEXT CHANGE (Validación en tiempo real) ---
    // -------------------------------------------------------------------------

    // Nota: Todos los manejadores llaman a validateX() para la validación y a checkGlobalValidation() 
    // para actualizar el estado del botón CREATE ACCOUNT.

    private void handleTfFNameTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfFName(newValue);
        checkGlobalValidation();
    }
    private void handleTfMNameTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfMName(newValue);
        checkGlobalValidation();
    }
    private void handleTfLNameTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfLName(newValue);
        checkGlobalValidation();
    }
    private void handleTfAddressTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfAddress(newValue);
        checkGlobalValidation();
    }
    private void handleTfCityTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfCity(newValue);
        checkGlobalValidation();
    }
    private void handleTfStateTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfState(newValue);
        checkGlobalValidation();
    }
    private void handleTfZipTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfZip(newValue);
        checkGlobalValidation();
    }
    private void handleTfPhoneTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfPhone(newValue);
        checkGlobalValidation();
    }
    private void handleTfEmailTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfEmail(newValue);
        checkGlobalValidation();
    }
    private void handleTfPassTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfPassword(newValue);
        validateTfRPass(tfRPass.getText()); // Revalidar RPass si cambia Pass
        checkGlobalValidation();
    }
    private void handleTfRPassTextChange(ObservableValue observable, String oldValue, String newValue) {
        validateTfRPass(newValue);
        checkGlobalValidation();
    }

    // -------------------------------------------------------------------------
    // --- MANEJADORES DE FOCUS CHANGE (Validación al perder el foco) [cite: 38] ---
    // -------------------------------------------------------------------------
    
    // Nota: Al perder el foco (!newValue), se valida el contenido del campo.
    
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
     * Regla: Enviar datos al backend. [cite: 36]
     */
    private void handleBtCreateOnAction(ActionEvent event) {
        // Deshabilitar campos y botón al pulsar [cite: 36]
        setFieldsDisabled(true);
        btCreate.setDisable(true);

        try {
            LOGGER.log(Level.INFO, "Attempting to create a new customer account.");
            
            // 1. Crear y asignar propiedades al objeto Customer (USANDO .getText())
            Customer customer = new Customer();
            customer.setFirstName(tfFName.getText());
            customer.setMiddleInitial(tfMName.getText()); 
            customer.setLastName(tfLName.getText());
            customer.setStreet(tfAddress.getText()); 
            customer.setCity(tfCity.getText());
            customer.setState(tfState.getText());
            customer.setZip(Integer.parseInt(tfZip.getText()));
            customer.setPhone(Long.parseLong(tfPhone.getText()));
            customer.setEmail(tfEmail.getText());
            customer.setPassword(tfPass.getText()); // Las contraseñas se deben enviar cifradas (HTTPS) [cite: 38]

            // 2. Llamar al servicio REST para la creación
            CustomerRESTClient client = new CustomerRESTClient();
            client.create_XML(customer); 
            client.close();
                
            // 3. Respuesta 201 Created: Mostrar mensaje y volver a Login [cite: 36]
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Registro Completo");
            successAlert.setHeaderText("¡Cuenta creada correctamente!");
            successAlert.setContentText("Volviendo a la ventana de Login.");
            successAlert.showAndWait();
                
            // Volver a la ventana de Login [cite: 36]
            ((Stage) btCreate.getScene().getWindow()).close(); 

        } catch (ForbiddenException e) {
            // Manejo de 409 Conflict: Mostrar mensaje "El correo ya está registrado." [cite: 36]
            LOGGER.log(Level.WARNING, "Creación fallida: Email ya registrado (409 Conflict).", e);
            new Alert(Alert.AlertType.WARNING, "El correo ya está registrado.").showAndWait();
            setFieldsDisabled(false); // Reactivar campos [cite: 36]
            checkGlobalValidation(); 
        } catch (InternalServerErrorException e) {
            // Manejo de 500 Error interno: Mostrar mensaje "Error en el servidor. Intenta más tarde." [cite: 37]
            LOGGER.log(Level.SEVERE, "Creación fallida: Error Interno del Servidor (500).", e);
            new Alert(Alert.AlertType.ERROR, "Error en el servidor. Intenta más tarde.").showAndWait();
            setFieldsDisabled(false); // Reactivar campos [cite: 36]
            checkGlobalValidation();
        } catch (Exception e) {
            // Manejo de otras excepciones (p. ej., 400 Bad Request, error de conexión) [cite: 36]
            LOGGER.log(Level.SEVERE, "Error inesperado al crear usuario: " + e.getMessage(), e);
            new Alert(Alert.AlertType.ERROR, "Datos inválidos o incompletos.").showAndWait(); // Mensaje genérico para 400 Bad Request
            setFieldsDisabled(false); // Reactivar campos [cite: 36]
            checkGlobalValidation();
        }
    }
    
    /**
     * @param event Manejador de evento al pulsar el botón BACK.
     * Regla: Pedir confirmación modal si hay datos introducidos. [cite: 37]
     */
    private void handleBtBackOnAction(ActionEvent event) {
        // Chequeo simple de si algún campo tiene contenido
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
                // Si el usuario confirma, cerrar la ventana actual y abrir la ventana Login [cite: 37]
                ((Stage) btBack.getScene().getWindow()).close();
            }
        } else {
            // Si no hay datos, simplemente vuelve al Login
            ((Stage) btBack.getScene().getWindow()).close();
        }
    }
    
    /**
     * Método auxiliar para habilitar o deshabilitar todos los TextFields.
     * @param disabled true para deshabilitar, false para habilitar.
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