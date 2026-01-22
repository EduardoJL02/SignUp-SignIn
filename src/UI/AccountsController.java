package UI;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javax.ws.rs.core.GenericType;
import logic.AccountRESTClient;
import model.Account;
import model.AccountType;
import model.Customer;

/**
 * Controlador para la gestión de cuentas (Accounts).
 *
 * @author Eduardo
 */
public class AccountsController implements Initializable {

    // Logger para trazas
    private static final Logger LOGGER = Logger.getLogger("UI.AccountsController");

    // Referencias a la UI (FXML)
    @FXML
    private TextField tfDescription;
    @FXML
    private ChoiceBox<AccountType> cbType;
    @FXML
    private TextField tfCreditLine;
    @FXML
    private TextField tfBalance;
    @FXML
    private TextField tfDate; // Creation Date (BeginBalanceTimestamp)

    @FXML
    private TableView<Account> tbAccounts;
    @FXML
    private TableColumn<Account, Long> tcId;
    @FXML
    private TableColumn<Account, String> tcDescription;
    @FXML
    private TableColumn<Account, AccountType> tcType;
    @FXML
    private TableColumn<Account, Double> tcBalance;
    @FXML
    private TableColumn<Account, Double> tcCreditLine;
    @FXML
    private TableColumn<Account, Double> tcBeginBalance;
    @FXML
    private TableColumn<Account, java.util.Date> tcDate;

    // Botonera
    @FXML
    private Button btnCreate;
    @FXML
    private Button btnModify;
    @FXML
    private Button btnDelete;
    @FXML
    private Button btnReport;
    @FXML
    private Button btnSearch;
    @FXML
    private Button btnHelp;
    
    @FXML
    private Label lblMessage;

    // Campos lógicos
    private Stage stage;
    private Customer user; // El usuario logueado
    private AccountRESTClient accountClient;
    private ObservableList<Account> accountsData;

    //Variable de estado
    private boolean isCreatingNewAccount = false;
    
    /**
     * Inicialización por defecto de JavaFX.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // No implementamos nada aquí porque preferimos hacerlo en setStage
        // para tener control total de cuándo inicia la ventana.
    }

    /**
     * Método principal para inicializar la ventana y recibir datos.
     *
     * @param root El nodo raíz cargado desde el FXML.
     * @param customer El cliente logueado que viene de la ventana anterior.
     */
    public void setStage(Parent root, Customer customer) {
        try {
            LOGGER.info("Inicializando ventana de Cuentas...");

            // 1. Crear la escena y el escenario (Stage)
            Scene scene = new Scene(root);
            stage = new Stage();
            stage.setScene(scene);

            // 2. Configurar propiedades de la ventana
            stage.setTitle("Accounts Management");
            stage.setResizable(false);
            stage.initModality(Modality.APPLICATION_MODAL); // Bloquea ventanas anteriores

            // 3. Guardar el usuario recibido
            this.user = customer;

            // 4. Inicializar componentes de UI
            // Cargar el Enum en el ChoiceBox
            cbType.setItems(FXCollections.observableArrayList(AccountType.values()));
            cbType.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<AccountType>() {
                @Override
                public void changed(ObservableValue<? extends AccountType> observable, AccountType oldValue, AccountType newValue) {
                    if (newValue == AccountType.CREDIT) {
                        tfCreditLine.setDisable(false);
                    } else {
                        tfCreditLine.setDisable(true);
                        tfCreditLine.setText(""); // Limpiar si cambia a STANDARD
                    }
                }
            });
            
            // La tabla NO debe ser editable
            tbAccounts.setEditable(false);

            // Configurar las columnas con los nombres de los atributos de Account
            tcId.setCellValueFactory(new PropertyValueFactory<>("id"));
            tcDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
            tcType.setCellValueFactory(new PropertyValueFactory<>("type"));
            tcBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));
            tcCreditLine.setCellValueFactory(new PropertyValueFactory<>("creditLine"));
            tcBeginBalance.setCellValueFactory(new PropertyValueFactory<>("beginBalance"));
            tcDate.setCellValueFactory(new PropertyValueFactory<>("beginBalanceTimestamp"));

            // 5. Instanciar el cliente REST
            accountClient = new AccountRESTClient();

            // 6. Cargar los datos del servidor
            loadAccountData();
            
            setupTableSelectionListener();

            // HANDLERS SELECCION DE LA TABLA
            root.setOnMouseClicked(new javafx.event.EventHandler<javafx.scene.input.MouseEvent>() {
                @Override
                public void handle(javafx.scene.input.MouseEvent event) {
                    // Solo si el clic no fue sobre la propia tabla (aunque la tabla suele consumir el evento)
                    // y para asegurar una buena UX, limpiamos la selección.
                    tbAccounts.getSelectionModel().clearSelection();
                }
            });

            // 2. Deseleccionar al pulsar la tecla ESCAPE
            scene.setOnKeyPressed(new javafx.event.EventHandler<javafx.scene.input.KeyEvent>() {
                @Override
                public void handle(javafx.scene.input.KeyEvent event) {
                    if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                        tbAccounts.getSelectionModel().clearSelection();
                    }
                }
            });
            
            // HANDLERS MODO CREACION
            root.setOnMouseClicked(new javafx.event.EventHandler<javafx.scene.input.MouseEvent>() {
                @Override
                public void handle(javafx.scene.input.MouseEvent event) {
                    if (isCreatingNewAccount) {
                        // Si estamos en modo creación, mostrar confirmación de cancelación
                        handleCancelCreation();
                    } else {
                        // Comportamiento normal: deseleccionar tabla
                        tbAccounts.getSelectionModel().clearSelection();
                    }
                }
            });

            scene.setOnKeyPressed(new javafx.event.EventHandler<javafx.scene.input.KeyEvent>() {
                @Override
                public void handle(javafx.scene.input.KeyEvent event) {
                    if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                        if (isCreatingNewAccount) {
                            // Si estamos en modo creación, mostrar confirmación de cancelación
                            handleCancelCreation();
                        } else {
                            // Comportamiento normal: deseleccionar tabla
                            tbAccounts.getSelectionModel().clearSelection();
                        }
                    }
                }
            });
            
            // Acción del botón Modificar
            btnModify.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
                @Override
                public void handle(javafx.event.ActionEvent event) {
                    handleModifyAction();
                }
            });
            
            //Accion del botón Create
            btnCreate.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
                @Override
                public void handle(javafx.event.ActionEvent event) {
                    handleCreateSaveAction();
                }
            });
        
            btnCreate.setDisable(false);
            btnModify.setDisable(true);
            btnDelete.setDisable(true);
            
            // 7. Mostrar la ventana
            stage.show();
            
            LOGGER.info("Ventana de Cuentas iniciada correctamente.");
        
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al inicializar la ventana de cuentas", e);
            showErrorAlert("Error initializing window: " + e.getMessage());
        }
        
        stage.setOnCloseRequest(new javafx.event.EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                cleanup();
            }
        });
    }

    /**
    * Carga los datos de las cuentas usando el cliente REST de forma síncrona.
    */
    private void loadAccountData() {
        try {
            LOGGER.info("Cargando cuentas para el cliente ID: " + user.getId());

            // PREPARAR GenericType para recibir List<Account>
            // Esto es necesario porque Java borra los tipos genéricos en tiempo de ejecución.
            GenericType<List<Account>> listType = new GenericType<List<Account>>() {};

            // LLAMADA AL SERVIDOR (Síncrona)
            // Usamos findAccountsByCustomerId_XML pasando el GenericType y el ID del usuario
            List<Account> accounts = accountClient.findAccountsByCustomerId_XML(listType, String.valueOf(user.getId()));

            // Convertir a ObservableList y setear en la tabla
            accountsData = FXCollections.observableArrayList(accounts);
            tbAccounts.setItems(accountsData);
            
            LOGGER.info("Se han cargado " + accountsData.size() + " cuentas.");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar datos del servidor", e);
            showErrorAlert("No se pudieron cargar los datos: " + e.getMessage());
        }
    }
    
    /**
     * Configura el listener de selección de la tabla.
     * Colocar este código dentro de setStage(...), después de cargar los datos.
     */
    private void setupTableSelectionListener() {
        tbAccounts.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Account>() {
            @Override
            public void changed(ObservableValue<? extends Account> observable, Account oldValue, Account newValue) {
                // CRÍTICO: No permitir selección si estamos creando
                if (isCreatingNewAccount) {
                    tbAccounts.getSelectionModel().clearSelection();
                    return;
                }

                if (newValue != null) {
                    // Si se selecciona una fila, rellenamos los campos
                    fillForm(newValue);

                    // Habilitar botones de edición/borrado
                    btnModify.setDisable(false);
                    btnDelete.setDisable(false);

                    // Deshabilitar botón de crear
                    btnCreate.setDisable(true);
                } else {
                    // Si se deselecciona, limpiamos el formulario
                    clearForm();

                    // Estado inicial de botones
                    btnModify.setDisable(true);
                    btnDelete.setDisable(true);
                    btnCreate.setDisable(false);
                }
            }
        });
    }

    /**
     * Rellena el formulario con los datos de la cuenta seleccionada
     * y gestiona la propiedad 'disable' de los campos según las reglas de negocio.
     * @param account La cuenta seleccionada.
     */
    private void fillForm(Account account) {
        // 1. Descripción: Siempre editable
        tfDescription.setText(account.getDescription());
        tfDescription.setDisable(false);

        // 2. Tipo: Seteamos el valor pero lo deshabilitamos (no se puede cambiar el tipo al editar)
        cbType.setValue(account.getType());
        cbType.setDisable(true); 

        // 3. Línea de Crédito: Depende del tipo
        if (account.getCreditLine() != null) {
            tfCreditLine.setText(String.valueOf(account.getCreditLine()));
        } else {
            tfCreditLine.setText("");
        }

        // REGLA DE NEGOCIO: Solo editable si es CREDIT
        if (account.getType() == AccountType.CREDIT) {
            tfCreditLine.setDisable(false);
        } else {
            tfCreditLine.setDisable(true);
        }

        // 4. Saldo: Solo lectura
        tfBalance.setText(String.valueOf(account.getBalance()));
        tfBalance.setDisable(true);
        
        // 5. Fecha: Formatear a String
        if (account.getBeginBalanceTimestamp() != null) {
            // Usamos SimpleDateFormat para formatear la fecha
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
            tfDate.setText(formatter.format(account.getBeginBalanceTimestamp()));
        } else {
            tfDate.setText("");
        }
        tfDate.setDisable(true);
    }

    /**
     * Limpia los campos del formulario y restablece su estado base.
     */
    private void clearForm() {
        tfDescription.setText("");
        tfDescription.setDisable(false); // Vuelve a ser editable para crear

        cbType.setValue(null);
        cbType.setDisable(false); // Vuelve a ser seleccionable para crear

        tfCreditLine.setText("");
        tfCreditLine.setDisable(true); // Se habilitará o no según lo que seleccione en el combo al crear

        tfBalance.setText("");
        tfBalance.setDisable(true);
        
        tfDate.setText("");
         tfDate.setDisable(true);
    }
    
    /**
     * Lógica para validar y actualizar la cuenta seleccionada.
     */
    private void handleModifyAction() {
        try {
            // 1. Obtener la cuenta seleccionada
            Account selectedAccount = tbAccounts.getSelectionModel().getSelectedItem();
            if (selectedAccount == null) {
                showErrorAlert("No account selected.");
                return;
            }

            // 2. VALIDACIÓN DE DATOS
            String newDescription = tfDescription.getText().trim();
            String creditLineStr = tfCreditLine.getText().trim();

            // Validación: Descripción no vacía
            if (newDescription.isEmpty()) {
                showErrorAlert("Error: The description cannot be empty.");
                tfDescription.requestFocus();
                return;
            }
            
            if (newDescription.length() > 255) {
                showErrorAlert("Error: Description is too long (max 255 characters).");
                tfDescription.requestFocus();
                return;
            }

            // Validación: Credit Line (Solo si es CREDIT)
            Double newCreditLine = selectedAccount.getCreditLine(); // Valor actual por defecto
            
            if (selectedAccount.getType() == AccountType.CREDIT) {
                if (creditLineStr.isEmpty()) {
                    showErrorAlert("Error: Credit Line cannot be empty for CREDIT accounts.");
                    tfCreditLine.requestFocus();
                    return;
                }
                try {
                    newCreditLine = Double.parseDouble(creditLineStr);
                    if (newCreditLine <= 0) {
                        showErrorAlert("Error: Credit Line must be greater than 0.");
                        tfCreditLine.requestFocus();
                        return;
                    }
                } catch (NumberFormatException e) {
                    showErrorAlert("Error: Credit Line must be a valid number.");
                    tfCreditLine.requestFocus();
                    return;
                }
            }

            // 3. CONFIRMACIÓN DEL USUARIO
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Modify Account");
            alert.setHeaderText("Updating Account ID: " + selectedAccount.getId());
            alert.setContentText("Are you sure you want to update this account?");

            java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {

                // 4. ACTUALIZAR OBJETO
                Account fullAccount = accountClient.find_XML(Account.class, String.valueOf(selectedAccount.getId()));
            
                fullAccount.setDescription(newDescription);
                if (fullAccount.getType() == AccountType.CREDIT) {
                    fullAccount.setCreditLine(newCreditLine);
                }

                accountClient.updateAccount_XML(fullAccount);

                // 6. FEEDBACK Y REFRESCO
                // Mostramos un mensaje de información breve (opcional)
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Success");
                info.setHeaderText(null);
                info.setContentText("Account updated successfully.");
                info.showAndWait();

                // Recargar datos para asegurar que la tabla refleja el servidor
                loadAccountData();
                
                // Limpiar selección y formulario
                tbAccounts.getSelectionModel().clearSelection();
                clearForm();
            }
            
        }catch (javax.ws.rs.ClientErrorException e) {
        // Errores 4xx (Bad Request, Not Found, etc.)
        LOGGER.log(Level.WARNING, "Error del cliente al actualizar cuenta", e);
        showErrorAlert("Invalid data or account not found.\nPlease check and try again.");
        
        } catch (javax.ws.rs.InternalServerErrorException e) {
            // Error 500 del servidor
            LOGGER.log(Level.SEVERE, "Error del servidor al actualizar cuenta", e);
            showErrorAlert("Server error. Please try again later.");
        
        }catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating account", e);
            showErrorAlert("Error updating account: " + e.getMessage());
        }
    }
    
    /**
    * Activa el "Modo Creación": limpia el formulario, deshabilita controles
    * y cambia el botón Create a Save (verde).
    */
    private void activateCreationMode() {
       LOGGER.info("Activando modo creación de cuenta...");

       // 1. Cambiar estado
       isCreatingNewAccount = true;

       // 2. Deseleccionar tabla (si había algo seleccionado)
       tbAccounts.getSelectionModel().clearSelection();

       // 3. Limpiar y habilitar formulario
       clearForm();
       tfDescription.setDisable(false);
       cbType.setDisable(false);
       // tfCreditLine se habilitará automáticamente si seleccionan CREDIT (gracias al listener)
       tfBalance.setDisable(true); // Siempre deshabilitado (valor automático 0.0)
       tfDate.setDisable(true);    // Siempre deshabilitado (fecha automática)

       // 4. Deshabilitar tabla y otros botones
       tbAccounts.setDisable(true);
       btnModify.setDisable(true);
       btnDelete.setDisable(true);
       btnReport.setDisable(true);
       btnSearch.setDisable(true);
       btnHelp.setDisable(true);

       // 5. Cambiar el botón Create a Save (color verde)
       btnCreate.setText("SAVE");
       btnCreate.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");

       // 6. Dar foco al primer campo
       tfDescription.requestFocus();

       LOGGER.info("Modo creación activado. Formulario listo para crear cuenta.");
    }
    
    /**
     * Maneja el evento del botón Create/Save.
     * - Si NO estamos creando: activa el "Modo Creación"
     * - Si YA estamos creando: valida y guarda la nueva cuenta
     */
    private void handleCreateSaveAction() {
        if (!isCreatingNewAccount) {
            // MODO 1: Activar "Modo Creación"
            activateCreationMode();
        } else {
            // MODO 2: Guardar la nueva cuenta
            saveNewAccount();
        }
    }
    
    //Metodo para cerrar el Cliente REST Después de Usarlo
    public void cleanup() {
        if (accountClient != null) {
            try {
                accountClient.close();
                LOGGER.info("Cliente REST cerrado correctamente.");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al cerrar cliente REST", e);
            }
        }
    }

    /**
     * Valida y guarda la nueva cuenta en el servidor.
     */
    private void saveNewAccount() {
        try {
            LOGGER.info("Intentando guardar nueva cuenta...");

            // ===============================================
            // 1. VALIDACIÓN DE DATOS
            // ===============================================
            String description = tfDescription.getText().trim();
            AccountType type = cbType.getValue();
            String creditLineStr = tfCreditLine.getText().trim();

            // Validación: Descripción no vacía
            if (description.isEmpty()) {
                showErrorAlert("Error: The description cannot be empty.");
                tfDescription.requestFocus();
                return;
            }

            // Validación: Longitud máxima de descripción
            if (description.length() > 255) {
                showErrorAlert("Error: Description is too long (max 255 characters).");
                tfDescription.requestFocus();
                return;
            }

            // Validación: Tipo seleccionado
            if (type == null) {
                showErrorAlert("Error: You must select an account type.");
                cbType.requestFocus();
                return;
            }

            // Validación: Credit Line (solo si es CREDIT)
            Double creditLine = null;
            if (type == AccountType.CREDIT) {
                if (creditLineStr.isEmpty()) {
                    showErrorAlert("Error: Credit Line cannot be empty for CREDIT accounts.");
                    tfCreditLine.requestFocus();
                    return;
                }
                try {
                    creditLine = Double.parseDouble(creditLineStr);
                    if (creditLine <= 0) {
                        showErrorAlert("Error: Credit Line must be greater than 0.");
                        tfCreditLine.requestFocus();
                        return;
                    }
                } catch (NumberFormatException e) {
                    showErrorAlert("Error: Credit Line must be a valid number.");
                    tfCreditLine.requestFocus();
                    return;
                }
            }

            // ===============================================
            // 2. CONFIRMACIÓN DEL USUARIO
            // ===============================================
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Create Account");
            confirmAlert.setHeaderText("Creating New Account");
            confirmAlert.setContentText("Are you sure you want to create this account?");

            java.util.Optional<javafx.scene.control.ButtonType> result = confirmAlert.showAndWait();
            if (!result.isPresent() || result.get() != javafx.scene.control.ButtonType.OK) {
                LOGGER.info("Creación de cuenta cancelada por el usuario.");
                return; // El usuario canceló
            }

            // ===============================================
            // 3. CREAR EL OBJETO ACCOUNT
            // ===============================================
            Account newAccount = new Account();
            newAccount.setDescription(description);
            newAccount.setType(type);

            // Balance inicial: 0.0
            newAccount.setBalance(0.0);
            newAccount.setBeginBalance(0.0);

            // Fecha actual
            newAccount.setBeginBalanceTimestamp(new java.util.Date());

            // Credit Line (solo para CREDIT)
            if (type == AccountType.CREDIT) {
                newAccount.setCreditLine(creditLine);
            } else {
                newAccount.setCreditLine(null); // Asegurar que sea null para STANDARD
            }

            // ===============================================
            // 4. ASOCIAR AL CLIENTE ACTUAL (CRÍTICO)
            // ===============================================
            java.util.Set<model.Customer> customers = new java.util.HashSet<>();
            customers.add(this.user); // El cliente logueado
            newAccount.setCustomers(customers);

            LOGGER.info("Cuenta configurada. Asociada al cliente ID: " + user.getId());

            // ===============================================
            // 5. ENVIAR AL SERVIDOR
            // ===============================================
            accountClient.createAccount_XML(newAccount);

            LOGGER.info("Cuenta creada exitosamente en el servidor.");

            // ===============================================
            // 6. FEEDBACK Y ACTUALIZACIÓN UI
            // ===============================================
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Success");
            successAlert.setHeaderText(null);
            successAlert.setContentText("Account created successfully.");
            successAlert.showAndWait();

            // Recargar datos desde el servidor
            loadAccountData();

            // Desactivar modo creación
            deactivateCreationMode();

            LOGGER.info("Modo creación desactivado. UI restaurada.");

        } catch (javax.ws.rs.ClientErrorException e) {
            LOGGER.log(Level.WARNING, "Error del cliente al crear cuenta", e);
            showErrorAlert("Invalid data. Please check and try again.");

        } catch (javax.ws.rs.InternalServerErrorException e) {
            LOGGER.log(Level.SEVERE, "Error del servidor al crear cuenta", e);
            showErrorAlert("Server error. Please try again later.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error inesperado al crear cuenta", e);
            showErrorAlert("Error creating account: " + e.getMessage());
        }
    }    

    /**
     * Desactiva el "Modo Creación": restaura el estado normal de la UI.
     */
    private void deactivateCreationMode() {
        LOGGER.info("Desactivando modo creación...");

        // 1. Cambiar estado
        isCreatingNewAccount = false;

        // 2. Limpiar formulario
        clearForm();

        // 3. Habilitar tabla y botones
        tbAccounts.setDisable(false);
        btnModify.setDisable(true);  // Se habilitará si seleccionan una fila
        btnDelete.setDisable(true);  // Se habilitará si seleccionan una fila
        btnReport.setDisable(false);
        btnSearch.setDisable(false);
        btnHelp.setDisable(false);

        // 4. Restaurar botón Create a su estado original
        btnCreate.setText("Create");
        btnCreate.setStyle(""); // Limpiar estilo inline (vuelve al CSS por defecto)
        btnCreate.setDisable(false);

        LOGGER.info("Modo creación desactivado correctamente.");
    }

        /**
     * Maneja la cancelación del modo creación (ESC o clic en fondo gris).
     * Muestra confirmación al usuario si hay datos en el formulario.
     */
    private void handleCancelCreation() {
        LOGGER.info("Intento de cancelar creación detectado.");

        // Verificar si hay datos en el formulario
        boolean hasData = !tfDescription.getText().trim().isEmpty() || cbType.getValue() != null || !tfCreditLine.getText().trim().isEmpty();

        if (hasData) {
            // Mostrar confirmación
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Cancel Creation");
            confirmAlert.setHeaderText("Do you want to cancel without saving?");
            confirmAlert.setContentText("All data entered will be lost.");

            // Personalizar botones
            confirmAlert.getButtonTypes().setAll(
                javafx.scene.control.ButtonType.OK,     // Aceptar (cancelar y limpiar)
                javafx.scene.control.ButtonType.CANCEL  // Cancelar (seguir creando)
            );

            java.util.Optional<javafx.scene.control.ButtonType> result = confirmAlert.showAndWait();

            if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
                // Usuario confirmó: cancelar y limpiar
                LOGGER.info("Usuario confirmó cancelación. Limpiando formulario.");
                deactivateCreationMode();
            } else {
                // Usuario canceló la cancelación: seguir en modo creación
                LOGGER.info("Usuario decidió continuar con la creación.");
                tfDescription.requestFocus(); // Devolver foco al formulario
            }
        } else {
            // No hay datos: cancelar directamente sin confirmación
            LOGGER.info("No hay datos en el formulario. Cancelando sin confirmación.");
            deactivateCreationMode();
        }
    }

    /**
     * Muestra una alerta de error simple.
     * @param msg Mensaje a mostrar
     */
    private void showErrorAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Error en la aplicación");
        alert.setContentText(msg);
        alert.showAndWait();
    }
}