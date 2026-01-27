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
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.Alert;
import model.AccountType;
import javafx.scene.input.MouseEvent;
import javafx.scene.Node;
import javafx.scene.control.TableRow;

/**
 * Controlador de Gestión de Cuentas.
 * Recibe un Customer desde la ventana principal y muestra sus cuentas.
 */
public class AccountsController {

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
     * @param customer Cliente dueño de la sesión.
     */
    public void initStage(Parent root) {
        this.userCustomer = user;
        LOGGER.info("Iniciando AccountsController para el cliente: " + user.getId() );

        Scene scene = new Scene(root);
        stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("Mis Cuentas - " + user.getFirstName() + " " + user.getLastName());
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
                    
                    // Si hemos encontrado una fila...
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
        
        // Instanciar cliente REST
        accountClient = new AccountRESTClient();

        // Configuración de columnas (Visualización básica)
        tcId.setCellValueFactory(new PropertyValueFactory<>("id"));
        tcDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        tcBeginBalance.setCellValueFactory(new PropertyValueFactory<>("Begin balance"));
        tcBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));
        tcCreditLine.setCellValueFactory(new PropertyValueFactory<>("creditLine"));
        tcType.setCellValueFactory(new PropertyValueFactory<>("type"));
        tcBalanceDate.setCellValueFactory(new PropertyValueFactory<>("beginBalanceTimestamp"));

        // Formatear la fecha para que se vea bonita (dd/MM/yyyy)
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

        setupColumnFactories();
        
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
                        // Nota: Modify se mantiene deshabilitado hasta que se edita una celda
                        // (lógica que ya pusimos en setupColumnFactories)
                        btnModify.setDisable(true); 
                    } else {
                        // Si no hay selección, deshabilitamos todo
                        btnDelete.setDisable(true);
                        btnModify.setDisable(true);
                    }
                }
            }
        );
        
        
        // Cargar datos del servidor filtrando por este cliente
        loadAccountsData();

        stage.showAndWait();
    }

   
    
    private void loadAccountsData() {
        try {
            LOGGER.info("Cargando cuentas para el cliente ID: " + userCustomer.getId());

            // PREPARAR GenericType para recibir List<Account>
            // Esto es necesario porque Java borra los tipos genéricos en tiempo de ejecución.
            //GenericType<List<Account>> listType = new GenericType<List<Account>>() {};

            // LLAMADA AL SERVIDOR (Síncrona)
            // Usamos findAccountsByCustomerId_XML pasando el GenericType y el ID del usuario
            Account[] accounts = accountClient.findAccountsByCustomerId_XML(Account[].class, String.valueOf(userCustomer.getId()));

            // Convertir a ObservableList y setear en la tabla
            accountsData = FXCollections.observableArrayList(accounts);
            tbAccounts.setItems(accountsData);
            
            LOGGER.info("Se han cargado " + accountsData.size() + " cuentas.");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar datos del servidor", e);
        }
    }

  
    
    /**
     * Configura las celdas para que sean editables y define sus validaciones.
     */
    private void setupColumnFactories() {
        // --- 1. Columna DESCRIPTION (Texto simple) ---
        tcDescription.setCellFactory(TextFieldTableCell.forTableColumn());
        
        tcDescription.setOnEditCommit(new EventHandler<CellEditEvent<Account, String>>() {
            @Override
            public void handle(CellEditEvent<Account, String> t) {
                Account account = t.getRowValue();
                account.setDescription(t.getNewValue());
                // Habilitamos el botón Modify al haber cambios pendientes
                btnModify.setDisable(false);
            }
        });
        
        // --- 4. Columna BEGIN BALANCE (Solo editable al crear) ---
        Callback<TableColumn<Account, Double>, TableCell<Account, Double>> beginBalanceCellFactory
                = new Callback<TableColumn<Account, Double>, TableCell<Account, Double>>() {
            @Override
            public TableCell<Account, Double> call(TableColumn<Account, Double> param) {
                return new TextFieldTableCell<Account, Double>(new DoubleStringConverter()) {
                    @Override
                    public void startEdit() {
                        // REGLA DE NEGOCIO: Solo editable si estamos en modo creación
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
                
                // Opcional: Al crear una cuenta, normalmente el balance inicial 
                // es igual al balance actual.
                if (creationMode) {
                    account.setBalance(newValue);
                    // Forzamos el refresco para que la columna "Balance" (no editable) 
                    // muestre también este valor visualmente.
                    tbAccounts.refresh();
                }
            }
        });

        // --- 2. Columna TYPE (ComboBox) ---
        tcType.setCellFactory(ComboBoxTableCell.forTableColumn(AccountType.values()));
        
        tcType.setOnEditCommit(new EventHandler<CellEditEvent<Account, AccountType>>() {
            @Override
            public void handle(CellEditEvent<Account, AccountType> t) {
                Account account = t.getRowValue();
                AccountType newType = t.getNewValue();
                account.setType(newType);
                
                // Regla de Negocio: Si cambia a STANDARD, el CreditLine debería ser 0 o null?
                // Por ahora solo permitimos el cambio.
                
                // Si cambiamos el tipo, forzamos a la tabla a repintar para que la columna
                // CreditLine actualice su estado de "editable/no editable" visualmente si fuera necesario.
                tbAccounts.refresh();
                
                btnModify.setDisable(false);
            }
        });

        // --- 3. Columna CREDIT LINE (Numérico, condicional) ---
        // Definimos una celda personalizada que solo permite editar si es CREDIT
        Callback<TableColumn<Account, Double>, TableCell<Account, Double>> creditLineCellFactory
                = new Callback<TableColumn<Account, Double>, TableCell<Account, Double>>() {
            @Override
            public TableCell<Account, Double> call(TableColumn<Account, Double> param) {
                // Usamos DoubleStringConverter para convertir de Texto a Double automáticamente
                return new TextFieldTableCell<Account, Double>(new DoubleStringConverter()) {
                    @Override
                    public void startEdit() {
                        // OBTENER LA CUENTA DE LA FILA ACTUAL
                        Account row = (Account) getTableRow().getItem();
                        
                        // VALIDACIÓN: Solo permitir editar si es tipo CREDIT
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
                
                // Validación extra: No permitir negativos (Opcional según reglas)
                if (newValue != null && newValue < 0) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Valor inválido");
                    alert.setContentText("La línea de crédito no puede ser negativa.");
                    alert.showAndWait();
                    // Revertimos al valor antiguo refrescando la tabla
                    tbAccounts.refresh();
                } else {
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
        // 1. Obtener la cuenta seleccionada
        Account selectedAccount = tbAccounts.getSelectionModel().getSelectedItem();

        // Verificación de seguridad (aunque el botón debería estar deshabilitado si no hay selección/cambios)
        if (selectedAccount == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Ninguna selección");
            alert.setHeaderText("No hay cuenta seleccionada");
            alert.setContentText("Por favor, selecciona una cuenta para modificar.");
            alert.showAndWait();
            return;
        }

        // 2. Ventana de Confirmación
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar modificación");
        alert.setHeaderText("Actualizar cuenta");
        alert.setContentText("¿Estás seguro de que quieres actualizar los datos de la cuenta seleccionada?\n\n"
                + "Descripción: " + selectedAccount.getDescription() + "\n"
                + "Línea de Crédito: " + selectedAccount.getCreditLine());

        // Capturar la respuesta del usuario (Sin lambdas)
        java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            try {
                // 3. Llamada al Servidor (REST PUT)
                // Enviamos el objeto Account modificado y su ID como String
                accountClient.updateAccount_XML(selectedAccount);

                // 4. Feedback de éxito
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Éxito");
                successAlert.setHeaderText(null);
                successAlert.setContentText("La cuenta se ha actualizado correctamente.");
                successAlert.showAndWait();

                // 5. Resetear estado de la interfaz
                btnModify.setDisable(true); // Deshabilitamos el botón hasta el próximo cambio
                tbAccounts.refresh();       // Refrescamos la tabla para asegurar consistencia visual

            } catch (Exception e) {
                // Manejo de Errores
                LOGGER.severe("Error al modificar la cuenta: " + e.getMessage());
                
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error de Servidor");
                errorAlert.setHeaderText("No se pudo actualizar la cuenta");
                errorAlert.setContentText("Ha ocurrido un error al intentar guardar los cambios.\n" 
                        + "Los datos se recargarán para restaurar los valores originales.");
                errorAlert.showAndWait();

                // IMPORTANTE: Si falla, recargamos los datos del servidor para deshacer 
                // los cambios que el usuario hizo en la tabla pero que no se guardaron.
                loadAccountsData();
                btnModify.setDisable(true);
            }
        } else {
            // Si el usuario cancela, podríamos optar por revertir cambios o dejarlos ahí.
            // Por simplicidad, los dejamos pendientes por si quiere darle a Modify de nuevo.
        }
    }
    
    @FXML
    private void handleCreateAction(ActionEvent event) {
        try {
            if (!creationMode) {
                // --- PASO 1: AÑADIR FILA VACÍA (MODO CREACIÓN) ---
                creationMode = true;
                // 1. Crear instancia con datos por defecto
                Account newAccount = new Account();
                newAccount.setId(generateLocalId()); // Requisito PDF: ID local
                newAccount.setBalance(0.0);
                newAccount.setBeginBalance(0.0);
                newAccount.setCreditLine(0.0);
                newAccount.setBeginBalanceTimestamp(new Date());
                newAccount.setType(AccountType.STANDARD);
                newAccount.setDescription("Nueva Cuenta"); // Texto inicial para que no sea null
                newAccount.setCustomers(new HashSet<>()); // Inicializar relación
                
                // GUARDAMOS LA REFERENCIA A LA CUENTA NUEVA
                creatingAccount = newAccount;
                
                // 2. Añadir a la lista observable (se muestra en tabla automáticamente)
                accountsData.add(newAccount);
                
                // 3. Seleccionar la nueva fila y hacer scroll hacia ella
                tbAccounts.getSelectionModel().select(newAccount);
                tbAccounts.scrollTo(newAccount);
                
                // 4. Poner el foco en la celda de Descripción para editar directamente
                // (Requiere que la columna tcDescription sea editable)
                tbAccounts.edit(accountsData.size() - 1, tcDescription);
                
                // 5. Cambiar estado del botón y bloquear otros controles
                creationMode = true;
                btnCreate.setText("Guardar"); // Cambiamos texto visualmente
                btnModify.setDisable(true);
                btnDelete.setDisable(true);
                
                // Nota: La tabla ya tiene el foco para escribir.
                
            } else {
                // --- PASO 2: GUARDAR EN SERVIDOR (CONFIRMAR) ---
                
                // 1. Obtener la cuenta que estamos creando (la seleccionada)
                Account newAccount = tbAccounts.getSelectionModel().getSelectedItem();
                
                // Aseguramos que la cuenta tenga al cliente actual asociado antes de enviarla
                if (newAccount.getCustomers() == null) {
                    newAccount.setCustomers(new HashSet<>());
                }
                
                // Añadimos el usuario actual a la lista de dueños de la cuenta
                newAccount.getCustomers().add(userCustomer);
                
                LOGGER.info("Enviando nueva cuenta al servidor con cliente asociado: " + userCustomer.getId());
                accountClient.createAccount_XML(newAccount);

                // 2. Validaciones básicas antes de enviar
                if (newAccount.getDescription().trim().isEmpty()) {
                    showErrorAlert("La descripción no puede estar vacía.");
                    return;
                }
                
                // 3. Enviar al servidor
                LOGGER.info("Enviando nueva cuenta al servidor: " + newAccount.getId());
                accountClient.createAccount_XML(newAccount);
                
                // 4. Feedback al usuario
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Cuenta Creada");
                alert.setHeaderText(null);
                alert.setContentText("La cuenta se ha creado correctamente.");
                alert.showAndWait();
                
                // 5. Resetear estado
                creationMode = false;
                creatingAccount = null;
                btnCreate.setText("Create");
                
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
            showErrorAlert("Error al crear la cuenta: " + e.getMessage());
            
            // Si falla al guardar, ¿queremos borrar la fila o dejarla para que reintente?
            // Por simplicidad, recargamos datos (borrando la fila local no guardada)
            creationMode = false;
            creatingAccount = null;
            btnCreate.setText("Create");
            loadAccountsData();
        }
    }
    
    /**
     * Acción del botón Delete.
     * Borra la cuenta seleccionada si no tiene movimientos y el usuario confirma.
     * @param event Evento del botón.
     */
    @FXML
    private void handleDeleteAction(ActionEvent event) {
        // 1. Obtener la cuenta seleccionada
        Account selectedAccount = tbAccounts.getSelectionModel().getSelectedItem();

        // Validación de seguridad (por si el botón no se deshabilitó correctamente)
        if (selectedAccount == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Ninguna selección");
            alert.setContentText("Por favor, selecciona una cuenta para borrar.");
            alert.showAndWait();
            return;
        }

        // 2. REGLA DE NEGOCIO: No borrar cuentas con movimientos
        // Verificamos si la lista de movimientos tiene datos
        if (selectedAccount.getMovements() != null && !selectedAccount.getMovements().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("No se puede borrar");
            alert.setHeaderText("La cuenta tiene movimientos asociados");
            alert.setContentText("Por seguridad, no se pueden eliminar cuentas que ya tienen historial de transacciones.\n\n"
                    + "Debes borrar los movimientos primero (si está permitido).");
            alert.showAndWait();
            return;
        }

        // 3. Ventana de Confirmación
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar borrado");
        alert.setHeaderText("Eliminar cuenta");
        alert.setContentText("¿Estás seguro de que quieres eliminar la cuenta definitivamente?\n\n"
                + "ID: " + selectedAccount.getId() + "\n"
                + "Descripción: " + selectedAccount.getDescription() + "\n\n"
                + "Esta acción no se puede deshacer.");

        // Capturar respuesta
        java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            try {
                // 4. Llamada al Servidor (REST DELETE)
                // El método removeAccount espera un String con el ID
                accountClient.removeAccount(String.valueOf(selectedAccount.getId()));

                // 5. Actualizar la interfaz (UI)
                // Opción A: Borrar directamente de la lista observable (Más rápido, evita recarga)
                accountsData.remove(selectedAccount);
                
                // Limpiar selección y deshabilitar botones
                tbAccounts.getSelectionModel().clearSelection();
                btnDelete.setDisable(true);
                btnModify.setDisable(true);

                // Feedback
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Borrado exitoso");
                successAlert.setContentText("La cuenta ha sido eliminada.");
                successAlert.showAndWait();

            } catch (Exception e) {
                LOGGER.severe("Error al borrar la cuenta: " + e.getMessage());
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error de Servidor");
                errorAlert.setContentText("No se pudo borrar la cuenta. Posiblemente esté siendo usada por otro proceso.");
                errorAlert.showAndWait();
                
                // Si falla, recargamos para asegurar que vemos lo que hay en el servidor
                loadAccountsData();
            }
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