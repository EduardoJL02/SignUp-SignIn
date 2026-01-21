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
 * @author Desarrollo proyecto CRUD
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

    // Bandera para controlar el estado de la interfaz
    private boolean creationMode = false;
    /**
     * Inicialización por defecto de JavaFX.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
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
            
            // 1. Deseleccionar al hacer clic en el fondo de la ventana (Root)
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
            
            // 1. Listener para el fondo (Root)
        root.setOnMouseClicked(new javafx.event.EventHandler<javafx.scene.input.MouseEvent>() {
            @Override
            public void handle(javafx.scene.input.MouseEvent event) {
                // Si estamos creando, pedimos confirmación para cancelar
                if (creationMode) {
                    cancelCreationWithConfirmation();
                } else {
                    // Si no, comportamiento normal de deselección
                    tbAccounts.getSelectionModel().clearSelection();
                }
            }
        });

        // 2. Listener para tecla ESCAPE
        scene.setOnKeyPressed(new javafx.event.EventHandler<javafx.scene.input.KeyEvent>() {
            @Override
            public void handle(javafx.scene.input.KeyEvent event) {
                if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    if (creationMode) {
                        cancelCreationWithConfirmation();
                    } else {
                        tbAccounts.getSelectionModel().clearSelection();
                    }
                }
            }
        });

        // --- CONFIGURACIÓN DE BOTONES ---

        // Botón CREATE
        btnCreate.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                if (creationMode) {
                    // SEGUNDO CLIC: Guardar
                    saveNewAccount();
                } else {
                    // PRIMER CLIC: Activar modo creación
                    enableCreationMode();
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

            // 7. Mostrar la ventana
            stage.show();
            
            LOGGER.info("Ventana de Cuentas iniciada correctamente.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al inicializar la ventana de cuentas", e);
            showErrorAlert("Error initializing window: " + e.getMessage());
        }
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
                if (newValue != null) {
                    // Si se selecciona una fila, rellenamos los campos
                    fillForm(newValue);
                    
                    // Habilitar botones de edición/borrado
                    btnModify.setDisable(false);
                    btnDelete.setDisable(false);
                    
                    // Deshabilitar botón de crear (para evitar crear duplicados mientras se edita)
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
        
        // 5. Fecha: Formatear a String
        if (account.getBeginBalanceTimestamp() != null) {
            // Usamos SimpleDateFormat para formatear la fecha
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
            tfDate.setText(formatter.format(account.getBeginBalanceTimestamp()));
        } else {
            tfDate.setText("");
        }
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
        tfCreditLine.setDisable(false); // Se habilitará o no según lo que seleccione en el combo al crear

        tfBalance.setText("");
        tfDate.setText("");
    }
    
    /**
     * Activa el modo de creación: limpia UI, bloquea tabla y cambia estilo del botón.
     */
    private void enableCreationMode() {
        // 1. Cambiar bandera
        creationMode = true;

        // 2. Limpiar formulario y selección
        tbAccounts.getSelectionModel().clearSelection();
        clearForm();

        // 3. UI Visual: Bloquear tabla y otros botones
        tbAccounts.setDisable(true);
        btnModify.setDisable(true);
        btnDelete.setDisable(true);
        btnReport.setDisable(true);
        btnSearch.setDisable(true);

        // 4. Configurar campos para crear
        tfDescription.setDisable(false);
        tfDescription.requestFocus(); // Foco al primer campo
        cbType.setDisable(false);
        
        // El campo CreditLine depende del combo, añadimos listener simple
        cbType.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<AccountType>() {
            @Override
            public void changed(ObservableValue<? extends AccountType> observable, AccountType oldValue, AccountType newValue) {
                if (newValue == AccountType.CREDIT) {
                    tfCreditLine.setDisable(false);
                } else {
                    tfCreditLine.setDisable(true);
                    tfCreditLine.setText(""); // Limpiar si cambia a Standard
                }
            }
        });

        // 5. Cambiar estilo del botón Create
        btnCreate.setText("Save");
        // Guardamos estilo original o aplicamos verde directamente
        btnCreate.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;"); 
    }

    /**
     * Lógica de guardado de nueva cuenta (Segundo clic).
     */
    private void saveNewAccount() {
        try {
            // 1. VALIDACIONES
            String description = tfDescription.getText().trim();
            AccountType type = cbType.getValue();
            String creditLineStr = tfCreditLine.getText().trim();

            if (description.isEmpty()) {
                showErrorAlert("Description is required.");
                tfDescription.requestFocus();
                return;
            }
            if (type == null) {
                showErrorAlert("Account Type is required.");
                cbType.requestFocus();
                return;
            }

            Double creditLine = 0.0;
            if (type == AccountType.CREDIT) {
                if (creditLineStr.isEmpty()) {
                    showErrorAlert("Credit Line is required for CREDIT accounts.");
                    tfCreditLine.requestFocus();
                    return;
                }
                try {
                    creditLine = Double.parseDouble(creditLineStr);
                    if (creditLine <= 0) {
                        showErrorAlert("Credit Line must be greater than 0.");
                        return;
                    }
                } catch (NumberFormatException e) {
                    showErrorAlert("Invalid number format for Credit Line.");
                    return;
                }
            }

            // 2. CONFIRMACIÓN DE GUARDADO
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Create Account");
            confirm.setHeaderText(null);
            confirm.setContentText("Are you sure you want to create this account?");
            
            java.util.Optional<javafx.scene.control.ButtonType> result = confirm.showAndWait();
            if (!result.isPresent() || result.get() != javafx.scene.control.ButtonType.OK) {
                return; // Cancelar guardado
            }

            // 3. CREAR OBJETO ACCOUNT
            Account newAccount = new Account();
            newAccount.setDescription(description);
            newAccount.setType(type);
            newAccount.setCreditLine(creditLine);
            newAccount.setBalance(0.0); // Saldo por defecto 0.0
            newAccount.setBeginBalance(0.0);
            newAccount.setBeginBalanceTimestamp(new java.util.Date()); // Fecha actual

            // --- ASOCIACIÓN CRÍTICA (CLIENTE - CUENTA) ---
            // Inicializamos el Set si es necesario y añadimos al usuario actual
            newAccount.setCustomers(new java.util.HashSet<Customer>());
            newAccount.getCustomers().add(this.user);

            // 4. LLAMADA AL SERVIDOR
            accountClient.createAccount_XML(newAccount);

            // 5. ÉXITO
            showInfoAlert("Account created successfully!");
            
            // 6. SALIR DEL MODO CREACIÓN Y RECARGAR
            resetCreationMode();
            loadAccountData();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating account", e);
            showErrorAlert("Error creating account: " + e.getMessage());
        }
    }

    /**
     * Intenta cancelar el modo creación pidiendo confirmación.
     */
    private void cancelCreationWithConfirmation() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cancel Creation");
        alert.setHeaderText("Cancel process");
        alert.setContentText("Do you want to cancel without saving?");

        java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            resetCreationMode();
        }
        // Si elige Cancelar, no hacemos nada (se queda en el formulario)
    }

    /**
     * Restaura la UI al estado inicial (fuera del modo creación).
     */
    private void resetCreationMode() {
        creationMode = false;
        
        // Limpiar formulario
        clearForm();
        
        // Restaurar botón Create
        btnCreate.setText("Create");
        btnCreate.setStyle(""); // Volver al estilo por defecto (CSS o nulo)

        // Habilitar tabla y botones
        tbAccounts.setDisable(false);
        // btnModify y Delete siguen deshabilitados hasta que selecciones una fila
        btnSearch.setDisable(false);
        btnReport.setDisable(false);
    }
    
    private void showInfoAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
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
                // IMPORTANTE: Solo actualizamos los campos permitidos por las reglas de negocio
                selectedAccount.setDescription(newDescription);
                if (selectedAccount.getType() == AccountType.CREDIT) {
                    selectedAccount.setCreditLine(newCreditLine);
                }

                // 5. LLAMADA AL SERVIDOR (REST PUT)
                accountClient.updateAccount_XML(selectedAccount);

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

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating account", e);
            showErrorAlert("Error updating account: " + e.getMessage());
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