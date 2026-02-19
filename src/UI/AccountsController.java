package UI;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import javax.ws.rs.core.GenericType;
import logic.AccountRESTClient;
import model.Account;
import model.AccountType;
import model.Customer;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.DefaultStringConverter;
import javafx.util.Callback;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.Alert;
import model.AccountType;
import javafx.scene.input.MouseEvent;
import javafx.scene.Node;
import javafx.scene.control.TableRow;

/**
 * Controlador de Gestión de Cuentas.
 * Recibe el Customer logueado y muestra sus cuentas.
 * @author Eduardo
 * @todo @fixme Hacer que la siguiente clase implemente las interfaces 
 * Initializable y MenuActionsHandler para que al pulsar en las acciones CRUD del 
 * menú Actions se ejecuten los métodos manejadores correspondientes a la vista 
 * que incluye el menú.
 * El método initialize debe llamar a setMenuActionsHandler() para establecer que este
 * controlador es el manejador de acciones del menú.
 */
public class AccountsController implements Initializable, MenuActionsHandler {

    /**
     * TODO: NO TOCAR La siguiente referencia debe llamarse así y tener este tipo.
     * JavaFX asigna automáticamente el campo menuIncludeController cuando usas fx:id="menuInclude".
     */
    @FXML
    private MenuController menuIncludeController;
    
    private static final Logger LOGGER = Logger.getLogger("UI.AccountsController");

    private Stage stage;
    private AccountRESTClient accountClient;
    private ObservableList<Account> accountsData;
    
    // Cliente conectado (recibido desde la ventana anterior)
    private Customer userCustomer;
    
    // Variable para controlar el estado del botón Create
    private boolean creationMode = false;
    
    // Variable para localizar la cuenta que se está creando
    private Account creatingAccount = null;

    @FXML
    private MenuBar menuBar;
    @FXML
    private TableView<Account> tbAccounts;
    @FXML
    private TableColumn<Account, Long> tcId;
    @FXML
    private TableColumn<Account, String> tcDescription;
    @FXML
    private TableColumn<Account, Double> tcBeginBalance;
    @FXML
    private TableColumn<Account, Double> tcBalance;
    @FXML
    private TableColumn<Account, Double> tcCreditLine;
    @FXML
    private TableColumn<Account, AccountType> tcType;
    @FXML
    private TableColumn<Account, Date> tcBalanceDate;
    @FXML
    private Button btnCreate;
    @FXML
    private Button btnModify;
    @FXML
    private Button btnDelete;
    @FXML
    private Button btnMovements;
    @FXML
    private Button btnCancel;
    @FXML
    private Button btnRefresh;
    
    @Override
    public void initialize(URL url, ResourceBundle rb){
        try{
            // Instanciar cliente REST
            accountClient = new AccountRESTClient();

            // Configuración de columnas (Visualización básica)
            tcId.setCellValueFactory(new PropertyValueFactory<>("id"));
            tcDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
            tcBeginBalance.setCellValueFactory(new PropertyValueFactory<>("beginBalance"));
            tcBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));
            tcCreditLine.setCellValueFactory(new PropertyValueFactory<>("creditLine"));
            tcType.setCellValueFactory(new PropertyValueFactory<>("type"));
            tcBalanceDate.setCellValueFactory(new PropertyValueFactory<>("beginBalanceTimestamp"));
            
            setupColumnFactories();
            
            setMenuActionsHandler();
            
        }catch (Exception e){
            LOGGER.log(Level.SEVERE, "Error crítico en initialize()", e);
        }
    }
    
    /**
     * Busca el ID más alto en la lista y le suma 1.
     * @return Nuevo ID local.
     */
    private Long generateLocalId() {
        Long maxId = 0L;
        if (accountsData != null) {
            for (Account a : accountsData) {
                if (a.getId() > maxId) {
                    maxId = a.getId();
                }
            }
        }
        return maxId + 1;
    }
    
    // Campo para guardar el usuario logueado
    private Customer user;

    /**
     * Recibe el usuario autenticado desde la ventana de Login.
     * @param user El cliente que ha iniciado sesión.
     */
    public void setCustomer(Customer user) {
        this.user = user;
    }
    
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setUser(Customer user) {
        this.user = user;
    }
    
    /**
     * Inicializa el escenario con el cliente específico.
     * @param root Nodo raíz FXML.
     * @param stage
     */
    public void initStage(Stage stage, Parent root) {
        this.userCustomer = user;
        LOGGER.info("Iniciando AccountsController para el cliente: " + user.getId() );

        this.stage = stage;
        
        Scene scene = new Scene(root);
        stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("My accounts - " + user.getFirstName() + " " + user.getLastName());
        stage.setResizable(false);
        stage.initModality(Modality.APPLICATION_MODAL);

        // Evento al mostrar ventana
        stage.setOnShowing(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                // Estado inicial de botones
                btnModify.setDisable(true);
                btnDelete.setDisable(true);
                btnCreate.setDisable(false);
                tbAccounts.setEditable(true);
                btnCancel.setDisable(true);
                btnCancel.setOpacity(0.0);
            }
        });
        
        tbAccounts.addEventFilter(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (creationMode) {
                    // Busamos qué fila se ha clicado
                    Node node = event.getPickResult().getIntersectedNode();
                    
                    // Navegar hacia arriba en la jerarquía visual hasta encontrar la TableRow
                    while (node != null && node != tbAccounts && !(node instanceof TableRow)) {
                        node = node.getParent();
                    }
                    
                    // Obtenemos item si hemos encontrado una fila...
                    if (node instanceof TableRow) {
                        TableRow row = (TableRow) node;
                        Account rowAccount = (Account) row.getItem();
                        
                        // Si la fila clicada NO es la que estamos creando, BLOQUEAMOS el evento
                        if (rowAccount == null || !rowAccount.equals(creatingAccount)) {
                            event.consume(); // El clic no hace nada
                        }
                    } else {
                        // Si clicamos en espacio vacío de la tabla, también bloqueamos para no perder foco
                        event.consume();
                    }
                }
            }
        });
        
        

        // Formatear la fecha (dd/MM/yyyy)
        tcBalanceDate.setCellFactory(new Callback<TableColumn<Account, Date>, TableCell<Account, Date>>() {
            @Override
            public TableCell<Account, Date> call(TableColumn<Account, Date> param) {
                return new TableCell<Account, Date>() {
                    private SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
                    @Override
                    protected void updateItem(Date item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(format.format(item));
                        }
                    }
                };
            }
        });

                
        // Listener para la selección de la tabla
        tbAccounts.getSelectionModel().selectedItemProperty().addListener(
            // Usamos clase anónima (Standard Java 8)
            new javafx.beans.value.ChangeListener<Account>() {
                @Override
                public void changed(javafx.beans.value.ObservableValue<? extends Account> observable, 
                                    Account oldValue, Account newValue) {
                    
                    // Si hay una fila seleccionada, habilitamos Delete
                    if (newValue != null) {
                        btnDelete.setDisable(false);
                        // Modify se mantiene deshabilitado hasta que se edita una celda
                        // (lógica de setupColumnFactories)
                        btnModify.setDisable(true); 
                    } else {
                        // Si no hay selección, se deshabilita todo
                        btnDelete.setDisable(true);
                        btnModify.setDisable(true);
                    }
                }
            }
        );
        
         // Manejador para el botón de cierre de la ventana
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                // Consumimos el evento para evitar que la ventana se cierre inmediatamente
                event.consume();
                
                // Llamamos a nuestro método de Log Out para pedir confirmación
                handleLogOutAction(null);
            }
        });

        
        // Cargar datos del servidor filtrando por este cliente
        loadAccountsData();

        stage.showAndWait();
    }

   
    
    private void loadAccountsData() {
        try {
        // Definimos el tipo exacto que queremos recibir: Una Lista de Accounts
        // Usamos una clase anónima interna para capturar el tipo genérico
        GenericType<List<Account>> listType = new GenericType<List<Account>>() {};

        // Llamamos al servidor usando el nuevo método que acepta listType
        List<Account> accountsList = accountClient.findAccountsByCustomerId_XML(listType, String.valueOf(userCustomer.getId()));

        // Convertimos la lista estándar de Java a una ObservableList para la tabla
        accountsData = FXCollections.observableArrayList(accountsList);

        // Asignamos los datos a la tabla
        tbAccounts.setItems(accountsData);

    } catch (Exception ex) {
        Logger.getLogger(AccountsController.class.getName()).log(Level.SEVERE, null, ex);
        
        // Mostrar alerta al usuario
        showErrorAlert("Error loading server accounts: " + ex.getMessage());
    }
    }

  
    
    /**
     * Configura las celdas para que sean editables y define sus validaciones.
     */
    private void setupColumnFactories() {
        // Columna DESCRIPTION (Texto simple)
        tcDescription.setCellFactory(TextFieldTableCell.forTableColumn());
        
        tcDescription.setOnEditCommit(new EventHandler<CellEditEvent<Account, String>>() {
            @Override
            public void handle(CellEditEvent<Account, String> t) {
                Account account = t.getRowValue();
                account.setDescription(t.getNewValue());
                // Habilitamos el botón Modify al haber cambios pendientes
                btnModify.setDisable(false);
                // Si estamos en modo creacion, el boton modify sigue deshabilitado
                if (creationMode){
                    btnModify.setDisable(true);
                }
            }
        });
        
        // Columna BEGIN BALANCE (Solo editable al crear)
        Callback<TableColumn<Account, Double>, TableCell<Account, Double>> beginBalanceCellFactory
                = new Callback<TableColumn<Account, Double>, TableCell<Account, Double>>() {
            @Override
            public TableCell<Account, Double> call(TableColumn<Account, Double> param) {
                return new TextFieldTableCell<Account, Double>(new DoubleStringConverter()) {
                    @Override
                    public void startEdit() {
                        // Solo editable si estamos en modo creación
                        Account rowItem = (Account) getTableRow().getItem();
                        
                        if (creationMode && rowItem != null && rowItem.equals(creatingAccount)) {
                            super.startEdit();
                        }
                        // Si creationMode es false, no hace nada (no entra en edición)
                    }
                };
            }
        };
        tcBeginBalance.setCellFactory(beginBalanceCellFactory);

        tcBeginBalance.setOnEditCommit(new EventHandler<CellEditEvent<Account, Double>>() {
            @Override
            public void handle(CellEditEvent<Account, Double> t) {
                Account account = t.getRowValue();
                Double newValue = t.getNewValue();
                
                // Actualizamos el BeginBalance
                account.setBeginBalance(newValue);
                
                // Al crear una cuenta, normalmente el balance inicial 
                // es igual al balance actual.
                if (creationMode) {
                    account.setBalance(newValue);
                    // Forzamos el refresco para que la columna "Balance" (no editable) 
                    // muestre también este valor visualmente.
                    tbAccounts.refresh();
                }
            }
        });

        // Columna TYPE (ComboBox)
        tcType.setCellFactory(ComboBoxTableCell.forTableColumn(AccountType.values()));
        
        tcType.setOnEditCommit(new EventHandler<CellEditEvent<Account, AccountType>>() {
            @Override
            public void handle(CellEditEvent<Account, AccountType> t) {
                Account account = t.getRowValue();
                AccountType newType = t.getNewValue();
                account.setType(newType);
                
                // Si cambia a STANDARD, el CreditLine debe ser 0.0
                
                // Si cambiamos el tipo, forzamos a la tabla a repintar para que la columna
                // CreditLine actualice su estado de "editable/no editable" visualmente si fuera necesario.
                tbAccounts.refresh();
                
                btnModify.setDisable(false);
            }
        });

        // Columna CREDIT LINE (Numérico, condicional)
        // Definimos una celda personalizada que solo permite editar si es CREDIT
        Callback<TableColumn<Account, Double>, TableCell<Account, Double>> creditLineCellFactory
                = new Callback<TableColumn<Account, Double>, TableCell<Account, Double>>() {
            @Override
            public TableCell<Account, Double> call(TableColumn<Account, Double> param) {
                // Usamos DoubleStringConverter para convertir de Texto a Double automáticamente
                return new TextFieldTableCell<Account, Double>(new DoubleStringConverter()) {
                    @Override
                    public void startEdit() {
                        // Obtener la cuenta de la fila actual
                        Account row = (Account) getTableRow().getItem();
                        
                        // Solo permitir editar si es tipo CREDIT
                        if (row != null && row.getType() == AccountType.CREDIT) {
                            super.startEdit();
                        }
                        // Si no es CREDIT, simplemente no hace nada (no entra en modo edición)
                    }
                };
            }
        };
        
        tcCreditLine.setCellFactory(creditLineCellFactory);
        
        tcCreditLine.setOnEditCommit(new EventHandler<CellEditEvent<Account, Double>>() {
            @Override
            public void handle(CellEditEvent<Account, Double> t) {
                Account account = t.getRowValue();
                Double newValue = t.getNewValue();
                
                // No permitir valores negativos
                if (newValue != null && newValue < 0) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Invalid value");
                    alert.setContentText("Line of credit cannot be negative.");
                    alert.showAndWait();
                    // Revertimos al valor antiguo refrescando la tabla
                    tbAccounts.refresh();
                } else {
                    // Si se edita la linea de credito, se establece el nuevo valor y habilitamos el boton modify
                    account.setCreditLine(newValue);
                    btnModify.setDisable(false);
                }
            }
        });
    }
    
    /**
     * Acción del botón Modify.
     * Envía los cambios de la cuenta seleccionada al servidor.
     * @param event El evento del botón.
     */
    @FXML
    private void handleModifyAction(ActionEvent event) {
        // Obtener la cuenta seleccionada
        Account selectedAccount = tbAccounts.getSelectionModel().getSelectedItem();

        // Verificación de seguridad (aunque el botón debería estar deshabilitado si no hay selección/cambios)
        if (selectedAccount == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No selection");
            alert.setHeaderText("No account selected");
            alert.setContentText("Please select an account to edit.");
            alert.showAndWait();
            return;
        }
        
        // Ventana de Confirmación
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm modification ");
        alert.setHeaderText("Update account");
        alert.setContentText("Are you sure you want to update the details of the selected account?\n\n"
                + "Description: " + selectedAccount.getDescription() + "\n"
                + "Credit line: " + selectedAccount.getCreditLine());

        // Capturar la respuesta del usuario
        java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            try {
                // Llamada al Servidor (REST PUT)
                // Enviamos el objeto Account modificado y su ID como String
                accountClient.updateAccount_XML(selectedAccount);

                // Feedback de éxito
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Success");
                successAlert.setHeaderText(null);
                successAlert.setContentText("The account has been successfully updated.");
                successAlert.showAndWait();

                // Resetear estado de la interfaz
                btnModify.setDisable(true); // Deshabilitamos el botón hasta el próximo cambio
                tbAccounts.refresh();       // Refrescamos la tabla para asegurar consistencia visual

            } catch (Exception e) {
                // Manejo de Errores
                LOGGER.severe("Error al modificar la cuenta: " + e.getMessage());
                
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Internal server error");
                errorAlert.setHeaderText("The account could not be updated.");
                errorAlert.setContentText("An error occurred while trying to save the changes.\n" 
                        + "The data will be reloaded to restore the original values.");
                errorAlert.showAndWait();

                // Si falla, recargamos los datos del servidor para deshacer 
                // los cambios que el usuario hizo en la tabla pero que no se guardaron.
                loadAccountsData();
                btnModify.setDisable(true);
            }
        }
    }
    
    @FXML
    private void handleCreateAction(ActionEvent event) {
        try {
            if (!creationMode) {
                loadAccountsData();
                // Añadir fila vacía (modo creación)
                creationMode = true;
                // Crear instancia con datos por defecto
                Account newAccount = new Account();
                newAccount.setId(generateLocalId()); // ID local
                newAccount.setBalance(0.0);
                newAccount.setBeginBalance(0.0);
                newAccount.setCreditLine(0.0);
                newAccount.setBeginBalanceTimestamp(new Date());
                newAccount.setType(AccountType.STANDARD);
                newAccount.setDescription("New Account"); // Texto inicial para que no sea null
                newAccount.setCustomers(new HashSet<>()); // Inicializar relación
                
                // Guardamos la referencia a la nueva cuenta
                creatingAccount = newAccount;
                
                // Añadir a la lista observable (se muestra en tabla automáticamente)
                accountsData.add(newAccount);
                
                // Seleccionar la nueva fila y hacer scroll hacia ella
                tbAccounts.getSelectionModel().select(newAccount);
                tbAccounts.scrollTo(newAccount);
                
                // Poner el foco en la celda de Descripción para editar directamente
                // (columna tcDescription editable)
                tbAccounts.edit(accountsData.size() - 1, tcDescription);
                
                // Cambiar estado del botón y bloquear otros controles
                btnCreate.setText("Save"); // Cambiamos texto visualmente
                btnModify.setDisable(true);
                btnDelete.setDisable(true);
                btnMovements.setDisable(true);
                
                // Aparecer y enseñar boton cancelar
                btnCancel.setDisable(false);
                btnCancel.setOpacity(1.0);
                
                // La tabla ya tiene el foco para escribir.
                
            } else {
                //Guardar en servidor (confirmar)
                
                // Obtener la cuenta que estamos creando (la seleccionada)
                Account newAccount = tbAccounts.getSelectionModel().getSelectedItem();
                
                // Validaciones básicas antes de enviar
                if (newAccount.getDescription() == null || newAccount.getDescription().trim().isEmpty()) {
                    showErrorAlert("Description cannot be empty");
                    return;
                }
                
                // Aseguramos que la cuenta tenga al cliente actual asociado antes de enviarla
                if (newAccount.getCustomers() == null) {
                    newAccount.setCustomers(new HashSet<>());
                }
                
                // Añadimos el usuario actual a la lista de dueños de la cuenta
                newAccount.getCustomers().add(userCustomer);                
                
                // Enviar al servidor
                LOGGER.info("Enviando nueva cuenta al servidor: " + newAccount.getId());
                accountClient.createAccount_XML(newAccount);
                
                // 4. Feedback al usuario
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Created Account");
                alert.setHeaderText(null);
                alert.setContentText("The account has been created successfully.");
                alert.showAndWait();
                
                // Resetear estado
                creationMode = false;
                creatingAccount = null;
                btnCreate.setText("Create");
                btnCancel.setDisable(true);
                btnCancel.setOpacity(0.0);
                btnMovements.setDisable(false);
                
                // Recargamos datos para asegurar que tenemos lo que hay en BBDD
                // y limpiamos cualquier estado "sucio" de la tabla.
                loadAccountsData();
                
                // Restaurar botones según selección (el listener de la tabla lo hará, 
                // pero forzamos el estado inicial correcto)
                btnModify.setDisable(true);
                btnDelete.setDisable(true);
            }
            
        } catch (Exception e) {
            LOGGER.severe("Error en proceso de creación: " + e.getMessage());
            showErrorAlert("Error creating the account " + e.getMessage());
            
            // Si falla al guardar, recargamos datos (borrando la fila local no guardada)
            creationMode = false;
            creatingAccount = null;
            btnCancel.setDisable(true);
            btnCancel.setOpacity(0.0);
            btnCreate.setText("Create");
            btnMovements.setDisable(false);
            loadAccountsData();
        }
    }
    
    @FXML
    private void handleCancelAction(ActionEvent event) {
        // Mostrar confirmación
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cancel creation");
        alert.setHeaderText("Do you want to exit from creation mode?");
        alert.setContentText("The data from the new account you were creating will be lost.");

        // Esperar respuesta 
        java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        
        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            try {
                // "Cancelar" significa volver al estado original.
                // La forma más segura es recargar los datos del servidor.
                // Esto borra la fila vacía local que habíamos añadido.
                loadAccountsData(); 
                
                 // Resetear estado
                creationMode = false;
                creatingAccount = null;
                btnCreate.setText("Create");
                
                // Restaurar estado de los botones
                btnCancel.setDisable(true);
                btnCancel.setOpacity(0.0); 
                
                btnCreate.setDisable(false); 
                btnModify.setDisable(true);
                btnDelete.setDisable(true);
                btnMovements.setDisable(false);
                
                // Limpiar selección de la tabla por seguridad
                tbAccounts.getSelectionModel().clearSelection();
                
            } catch (Exception e) {
                LOGGER.severe("Error al cancelar creación: " + e.getMessage());
                showErrorAlert("Error canceling creation: " + e.getMessage());
            }
        }
    }
    
    /**
     * Acción del botón Delete.
     * Borra la cuenta seleccionada si no tiene movimientos y el usuario confirma.
     * @param event Evento del botón.
     */
    @FXML
    private void handleDeleteAction(ActionEvent event) {
        // Obtener la cuenta seleccionada
        Account selectedAccount = tbAccounts.getSelectionModel().getSelectedItem();

        // Validación de seguridad (por si el botón no se deshabilitó correctamente)
        if (selectedAccount == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No selection");
            alert.setContentText("Please, select an account to delete.");
            alert.showAndWait();
            return;
        }

        // No borrar cuentas con movimientos
        // Verificamos si la lista de movimientos tiene datos
        if (selectedAccount.getMovements() != null && !selectedAccount.getMovements().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("It cannot be deleted");
            alert.setHeaderText("The account has associated transactions");
            alert.setContentText("For security reasons, accounts that already have a transaction history cannot be deleted.\n\n"
                    + "You must delete the moves first (if allowed).");
            alert.showAndWait();
            return;
        }

        // Ventana de Confirmación
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm deletion");
        alert.setHeaderText("Delete account");
        alert.setContentText("Are you sure you want to delete the account permanently?\n\n"
                + "ID: " + selectedAccount.getId() + "\n"
                + "Description: " + selectedAccount.getDescription() + "\n\n"
                + "This action cannot be undone.");

        // Capturar respuesta
        java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            try {
                // Llamada al Servidor (REST DELETE)
                // El método removeAccount espera un String con el ID
                accountClient.removeAccount(String.valueOf(selectedAccount.getId()));

                // Actualizar la interfaz (UI)
                // Borrar directamente de la lista observable (Más rápido, evita recarga)
                accountsData.remove(selectedAccount);
                
                // Limpiar selección y deshabilitar botones
                tbAccounts.getSelectionModel().clearSelection();
                btnDelete.setDisable(true);
                btnModify.setDisable(true);

                // Feedback
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Successful deletion");
                successAlert.setContentText("The account has been deleted.");
                successAlert.showAndWait();

            } catch (Exception e) {
                LOGGER.severe("Error al borrar la cuenta: " + e.getMessage());
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Internal server error");
                errorAlert.setContentText("The account could not be deleted. It is possibly being used by another process.");
                errorAlert.showAndWait();
                
                // Si falla, recargamos para asegurar que vemos lo que hay en el servidor
                loadAccountsData();
            }
        }
    }
    
    @FXML
    private void handleMovementsAction(ActionEvent event) {
        // Obtener la cuenta seleccionada en la tabla
        Account selectedAccount = tbAccounts.getSelectionModel().getSelectedItem();
        try {
            // Cargar el FXML de Movimientos
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UI/FXMLDocumentMyMovements.fxml"));
            Parent root = loader.load();

            // Obtener la controladora
            MovementController controller = loader.getController();

            // Pasar los datos
            controller.setClientData(this.user);

            // 4. Mostrar la ventana (Stage)
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(this.stage);
        stage.setScene(new Scene(root));
        stage.setTitle("Movements");
        
        controller.setPreselectedAccount(selectedAccount);
        stage.show();

        } catch (Exception e) {
            LOGGER.severe("Error abriendo movimientos: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Error opening the movements window.");
            alert.showAndWait();
        }
    }
    
    /**
     * Acción para refrescar la tabla manualmente.
     * Recarga los datos del servidor y limpia la selección.
     * @param event 
     */
    @FXML
    private void handleRefreshAction(ActionEvent event) {
        try {
            LOGGER.info("Refrescando datos de la tabla...");
            
            // Recargar los datos desde el servidor
            loadAccountsData();
            tbAccounts.refresh();
            
            // Resetear el estado de los botones CRUD
            // Al refrescar, se pierde la selección, así que desactivamos botones de acción
            tbAccounts.getSelectionModel().clearSelection();
            btnCreate.setText("Create"); // Asegurar que no estamos en modo edición
            btnModify.setDisable(true);
            btnDelete.setDisable(true);
            btnMovements.setDisable(false);
            
            // Asegurar que salimos de modo creación si estábamos en él
            creationMode = false;
            creatingAccount = null;
            btnCancel.setDisable(true);
            btnCancel.setOpacity(0.0);
            
        } catch (Exception e) {
            showErrorAlert("The table could not be refreshed: " + e.getMessage());
        }
    }
    
    /**
     * Método para manejar el cierre de sesión o salida de la ventana.
     * Pide confirmación al usuario antes de cerrar.
     * @param event Evento de acción (puede ser null si se llama manualmente).
     */
    @FXML
    private void handleLogOutAction(ActionEvent event) {
        // Mostrar ventana de confirmación
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Sign Out");
        alert.setHeaderText("Exit the application");
        alert.setContentText("Are you sure you want to log out and log back in?");

        // Esperar respuesta del usuario
        java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            // Cerrar la ventana actual (Stage)
            // Esto disparará el evento setOnHidden configurado en el Login, 
            // el cual se encargará de limpiar los campos y restaurar el estado inicial.
            
                stage.close(); 
                System.exit(0);
                }
            }
    
    /**
     * Muestra una alerta de error simple.
     * @param msg Mensaje a mostrar
     */
    private void showErrorAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Application error");
        alert.setContentText(msg);
        alert.showAndWait();
    }

    @Override
    public void onCreate() {
        handleCreateAction(null);
    }

    @Override
    public void onRefresh() {
        handleRefreshAction(null);
    }

    @Override
    public void onUpdate() {
        handleModifyAction(null);
    }

    @Override
    public void onDelete() {
        handleDeleteAction(null);
    }

    private void setMenuActionsHandler() {
        menuIncludeController.setMenuActionsHandler(this);
    }
}