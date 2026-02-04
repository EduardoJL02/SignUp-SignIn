package UI;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import javafx.util.converter.DoubleStringConverter;
import javax.ws.rs.core.GenericType;
import logic.AccountRESTClient;
import model.Account;
import model.AccountType;
import model.Customer;

/**
 * Controlador de Gestión de Cuentas.
 */
public class AccountsController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger("UI.AccountsController");

    private Stage stage;
    private AccountRESTClient accountClient;
    private ObservableList<Account> accountsData;
    
    // Cliente conectado (variable unificada para evitar confusiones)
    private Customer user;
    
    // Variable para controlar el estado del botón Create
    private boolean creationMode = false;
    
    // Variable para localizar la cuenta que se está creando
    private Account creatingAccount = null;

    @FXML private MenuBar menuBar;
    @FXML private TableView<Account> tbAccounts;
    @FXML private TableColumn<Account, Long> tcId;
    @FXML private TableColumn<Account, String> tcDescription;
    @FXML private TableColumn<Account, Double> tcBeginBalance;
    @FXML private TableColumn<Account, Double> tcBalance;
    @FXML private TableColumn<Account, Double> tcCreditLine;
    @FXML private TableColumn<Account, AccountType> tcType;
    @FXML private TableColumn<Account, Date> tcBalanceDate;
    @FXML private Button btnCreate;
    @FXML private Button btnModify;
    @FXML private Button btnDelete;
    @FXML private Button btnMovements;
    @FXML private Button btnCancel;

    /**
     * MÉTODO INITIALIZE: Se ejecuta automáticamente al cargar el FXML.
     * Es fundamental inicializar aquí el cliente REST para que funcione
     * tanto desde el Login como desde el botón "Volver".
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Instanciar cliente REST inmediatamente
        accountClient = new AccountRESTClient();
        
        // 2. Configurar las celdas y validaciones
        setupColumnFactories();
        configTableFormat();
        setupTableListeners();
    }

    /**
     * Recibe el usuario autenticado.
     * MODIFICADO: Ahora carga los datos automáticamente al recibir el usuario.
     * Esto permite que el botón "Volver" funcione correctamente.
     * @param user El cliente que ha iniciado sesión.
     */
    public void setCustomer(Customer user) {
        this.user = user;
        
        // Si tenemos un usuario, cargamos sus datos inmediatamente
        if (this.user != null) {
            loadAccountsData();
            
            // Si el stage ya existe (ej. volviendo atrás), actualizamos el título
            if (stage != null) {
                stage.setTitle("Mis Cuentas - " + user.getFirstName());
            }
        }
    }
    
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Inicializa el escenario con el cliente específico (Usado desde Login).
     * @param root Nodo raíz FXML.
     */
    public void initStage(Parent root) {
        LOGGER.info("Iniciando AccountsController para el cliente: " + user.getId() );

        Scene scene = new Scene(root);
        
        // Si stage no ha sido inyectado previamente
        if (stage == null) {
            stage = new Stage();
        }
        
        stage.setScene(scene);
        stage.setTitle("Mis Cuentas - " + user.getFirstName() + " " + user.getLastName());
        stage.setResizable(false);
        stage.initModality(Modality.APPLICATION_MODAL);

        // Configurar eventos de ventana
        stage.setOnShowing(this::handleWindowShowing);
        stage.setOnCloseRequest(event -> {
            event.consume();
            handleLogOutAction(null);
        });

        // NOTA: loadAccountsData ya se llama en setCustomer, pero aseguramos
        if (accountsData == null || accountsData.isEmpty()) {
            loadAccountsData();
        }

        stage.showAndWait();
    }

    // --- MÉTODOS DE CONFIGURACIÓN UI ---

    private void handleWindowShowing(WindowEvent event) {
        btnModify.setDisable(true);
        btnDelete.setDisable(true);
        btnCreate.setDisable(false);
        tbAccounts.setEditable(true);
        btnCancel.setDisable(true);
        btnCancel.setOpacity(0.0);
    }

    private void configTableFormat() {
        tcId.setCellValueFactory(new PropertyValueFactory<>("id"));
        tcDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        tcBeginBalance.setCellValueFactory(new PropertyValueFactory<>("beginBalance")); // Corregido typo "Begin balance"
        tcBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));
        tcCreditLine.setCellValueFactory(new PropertyValueFactory<>("creditLine"));
        tcType.setCellValueFactory(new PropertyValueFactory<>("type"));
        tcBalanceDate.setCellValueFactory(new PropertyValueFactory<>("beginBalanceTimestamp"));

        // Formato Fecha
        tcBalanceDate.setCellFactory(param -> new TableCell<Account, Date>() {
            private SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : format.format(item));
            }
        });
    }

    private void setupTableListeners() {
        // Listener de Selección
        tbAccounts.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                btnDelete.setDisable(false);
                btnModify.setDisable(true); // Se habilita al editar una celda
            } else {
                btnDelete.setDisable(true);
                btnModify.setDisable(true);
            }
        });

        // Listener de Clicks para modo creación
        tbAccounts.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (creationMode) {
                Node node = event.getPickResult().getIntersectedNode();
                while (node != null && node != tbAccounts && !(node instanceof TableRow)) {
                    node = node.getParent();
                }
                if (node instanceof TableRow) {
                    TableRow row = (TableRow) node;
                    Account rowAccount = (Account) row.getItem();
                    if (rowAccount == null || !rowAccount.equals(creatingAccount)) {
                        event.consume();
                    }
                } else {
                    event.consume();
                }
            }
        });
    }

    // --- LOGICA DE DATOS ---

    private void loadAccountsData() {
        if (user == null) return;
        
        try {
            GenericType<List<Account>> listType = new GenericType<List<Account>>() {};
            List<Account> accountsList = accountClient.findAccountsByCustomerId_XML(listType, String.valueOf(user.getId()));
            
            accountsData = FXCollections.observableArrayList(accountsList);
            tbAccounts.setItems(accountsData);
            tbAccounts.refresh();

        } catch (Exception ex) {
            showErrorAlert("Error al cargar las cuentas del servidor: " + ex.getMessage());
        }
    }

    private Long generateLocalId() {
        Long maxId = 0L;
        if (accountsData != null) {
            for (Account a : accountsData) {
                if (a.getId() > maxId) maxId = a.getId();
            }
        }
        return maxId + 1;
    }

    // --- CONFIGURACIÓN DE FACTORÍAS DE CELDAS ---

    private void setupColumnFactories() {
        // 1. Description
        tcDescription.setCellFactory(TextFieldTableCell.forTableColumn());
        tcDescription.setOnEditCommit(t -> {
            t.getRowValue().setDescription(t.getNewValue());
            btnModify.setDisable(false);
        });
        
        // 2. Begin Balance (Solo editable en creación)
        tcBeginBalance.setCellFactory(param -> new TextFieldTableCell<Account, Double>(new DoubleStringConverter()) {
            @Override
            public void startEdit() {
                Account rowItem = (Account) getTableRow().getItem();
                if (creationMode && rowItem != null && rowItem.equals(creatingAccount)) {
                    super.startEdit();
                }
            }
        });
        tcBeginBalance.setOnEditCommit(t -> {
            Account account = t.getRowValue();
            account.setBeginBalance(t.getNewValue());
            if (creationMode) {
                account.setBalance(t.getNewValue());
                tbAccounts.refresh();
            }
        });

        // 3. Type
        tcType.setCellFactory(ComboBoxTableCell.forTableColumn(AccountType.values()));
        tcType.setOnEditCommit(t -> {
            t.getRowValue().setType(t.getNewValue());
            tbAccounts.refresh();
            btnModify.setDisable(false);
        });

        // 4. Credit Line (Solo si es CREDIT)
        tcCreditLine.setCellFactory(param -> new TextFieldTableCell<Account, Double>(new DoubleStringConverter()) {
            @Override
            public void startEdit() {
                Account row = (Account) getTableRow().getItem();
                if (row != null && row.getType() == AccountType.CREDIT) {
                    super.startEdit();
                }
            }
        });
        tcCreditLine.setOnEditCommit(t -> {
            if (t.getNewValue() != null && t.getNewValue() < 0) {
                showErrorAlert("La línea de crédito no puede ser negativa.");
                tbAccounts.refresh();
            } else {
                t.getRowValue().setCreditLine(t.getNewValue());
                btnModify.setDisable(false);
            }
        });
    }
    
    // --- MANEJADORES DE ACCIONES (CRUD) ---

    @FXML
    private void handleCreateAction(ActionEvent event) {
        try {
            if (!creationMode) {
                // INICIAR MODO CREACIÓN
                creationMode = true;
                Account newAccount = new Account();
                newAccount.setId(generateLocalId());
                newAccount.setBalance(0.0);
                newAccount.setBeginBalance(0.0);
                newAccount.setCreditLine(0.0);
                newAccount.setBeginBalanceTimestamp(new Date());
                newAccount.setType(AccountType.STANDARD);
                newAccount.setDescription("Nueva Cuenta");
                newAccount.setCustomers(new HashSet<>());
                
                creatingAccount = newAccount;
                accountsData.add(newAccount);
                
                tbAccounts.getSelectionModel().select(newAccount);
                tbAccounts.scrollTo(newAccount);
                tbAccounts.edit(accountsData.size() - 1, tcDescription);
                
                btnCreate.setText("Save");
                btnModify.setDisable(true);
                btnDelete.setDisable(true);
                btnMovements.setDisable(true);
                btnCancel.setDisable(false);
                btnCancel.setOpacity(1.0);
                
            } else {
                // GUARDAR
                Account newAccount = tbAccounts.getSelectionModel().getSelectedItem();
                if (newAccount.getCustomers() == null) newAccount.setCustomers(new HashSet<>());
                newAccount.getCustomers().add(user); // Usamos 'user' unificado
                
                if (newAccount.getDescription().trim().isEmpty()) {
                    showErrorAlert("La descripción no puede estar vacía.");
                    return;
                }
                
                accountClient.createAccount_XML(newAccount);
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Cuenta Creada");
                alert.setContentText("La cuenta se ha creado correctamente.");
                alert.showAndWait();
                
                // Reset
                creationMode = false;
                creatingAccount = null;
                btnCreate.setText("Create");
                loadAccountsData();
                btnCancel.setDisable(true);
                btnCancel.setOpacity(0.0);
                btnMovements.setDisable(false);
            }
        } catch (Exception e) {
            LOGGER.severe("Error creando cuenta: " + e.getMessage());
            showErrorAlert("Error al crear la cuenta: " + e.getMessage());
            // En caso de error, reseteamos para no dejar datos corruptos
            creationMode = false;
            creatingAccount = null;
            btnCreate.setText("Create");
            loadAccountsData();
        }
    }

    @FXML
    private void handleModifyAction(ActionEvent event) {
        Account selectedAccount = tbAccounts.getSelectionModel().getSelectedItem();
        if (selectedAccount == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar modificación");
        alert.setHeaderText("Actualizar cuenta");
        alert.setContentText("¿Estás seguro de actualizar: " + selectedAccount.getDescription() + "?");

        if (alert.showAndWait().orElse(null) == javafx.scene.control.ButtonType.OK) {
            try {
                accountClient.updateAccount_XML(selectedAccount);
                
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Éxito");
                success.setContentText("Cuenta actualizada.");
                success.showAndWait();
                
                btnModify.setDisable(true);
                tbAccounts.refresh();
            } catch (Exception e) {
                showErrorAlert("Error al actualizar: " + e.getMessage());
                loadAccountsData();
            }
        }
    }

    @FXML
    private void handleDeleteAction(ActionEvent event) {
        Account selectedAccount = tbAccounts.getSelectionModel().getSelectedItem();
        if (selectedAccount == null) return;

        if (selectedAccount.getMovements() != null && !selectedAccount.getMovements().isEmpty()) {
            showErrorAlert("No se puede borrar una cuenta con movimientos.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar borrado");
        alert.setContentText("¿Eliminar definitivamente la cuenta " + selectedAccount.getDescription() + "?");

        if (alert.showAndWait().orElse(null) == javafx.scene.control.ButtonType.OK) {
            try {
                accountClient.removeAccount(String.valueOf(selectedAccount.getId()));
                accountsData.remove(selectedAccount);
                tbAccounts.getSelectionModel().clearSelection();
                
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Borrado");
                success.setContentText("Cuenta eliminada.");
                success.showAndWait();
            } catch (Exception e) {
                showErrorAlert("Error al borrar: " + e.getMessage());
                loadAccountsData();
            }
        }
    }

    @FXML
    private void handleCancelAction(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cancelar");
        alert.setHeaderText("¿Salir del modo creación?");
        
        if (alert.showAndWait().orElse(null) == javafx.scene.control.ButtonType.OK) {
            loadAccountsData();
            creationMode = false;
            creatingAccount = null;
            btnCreate.setText("Create");
            btnCancel.setDisable(true);
            btnCancel.setOpacity(0.0);
            btnCreate.setDisable(false);
            btnMovements.setDisable(false);
            tbAccounts.getSelectionModel().clearSelection();
        }
    }

    // --- NAVEGACIÓN ---

    @FXML
    private void handleMovementsAction(ActionEvent event) {
        Account selectedAccount = tbAccounts.getSelectionModel().getSelectedItem();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UI/FXMLDocumentMyMovements.fxml"));
            Parent root = loader.load();

            MovementController controller = loader.getController();
            controller.setClientData(this.user);
            
            // Usamos la misma Stage para navegar (Intercambio de escenas)
            // Esto es importante para que el botón Back funcione como esperas
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            
            // Opcional: Si quieres mantener el título o cambiarlo
            // currentStage.setTitle("Movimientos");
            
            Scene scene = new Scene(root);
            currentStage.setScene(scene);
            currentStage.show();

            // Pasamos la cuenta seleccionada después de mostrar (o antes, depende de la lógica de init)
            controller.setPreselectedAccount(selectedAccount);

        } catch (Exception e) {
            LOGGER.severe("Error abriendo movimientos: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("Error al abrir movimientos.");
        }
    }

    @FXML
    private void handleLogOutAction(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cerrar Sesión");
        alert.setContentText("¿Está seguro de que desea cerrar sesión?");

        if (alert.showAndWait().orElse(null) == javafx.scene.control.ButtonType.OK) {
            if (stage != null) stage.close();
        }
    }

    private void showErrorAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(msg);
        alert.showAndWait();
    }
}