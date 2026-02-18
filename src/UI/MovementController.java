package UI;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import logic.AccountRESTClient;
import logic.MovementRESTClient;
import model.Account;
import model.Customer;
import model.Movement;

import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.GenericType;

/**
 * Controlador para la ventana de Movimientos.
 * Esta clase conecta la tabla visual con el servidor REST.
 * Gestiona la carga de cuentas, visualización de movimientos y lógica de saldo.
 */
public class MovementController implements Initializable, HelpProvider {
    
    @Override
    public String getHelpFile() {
        return HelpProvider.HELP_MOVEMENTS; // → "movements.html"
    }

    @Override
    public String getWindowTitle() {
        return "Ayuda - Movimientos";
    }

    // Logger para registrar eventos o errores técnicos
    private static final Logger LOGGER = Logger.getLogger(MovementController.class.getName());

    // --- ELEMENTOS DE LA UI (Vínculo con FXML) ---
    @FXML private Label lblCustomerName;
    @FXML private Label lblUserId;
    @FXML private Label lblAccountId;
    @FXML private Label lblCreditLimit; // Muestra el límite si es cuenta crédito
    @FXML private ComboBox<Account> cbAccountSelector; // Selector de cuentas del usuario
    @FXML private TextField tfBalance; // Saldo total calculado (Solo lectura en UI)
    @FXML private Label lblStatus; // Barra de estado para feedback al usuario
    @FXML private Button btBack; 

    // --- CONFIGURACIÓN DE LA TABLA ---
    @FXML private TableView<Movement> tvMovements;
    @FXML private TableColumn<Movement, LocalDate> colDate;
    @FXML private TableColumn<Movement, String> colType; // Editable (Ingreso/Pago)
    @FXML private TableColumn<Movement, Double> colAmount; // Editable (Importe)
    @FXML private TableColumn<Movement, Movement> colBalance; // Columna calculada localmente
    
    @FXML
    private MenuController menuController;

    // --- DATOS Y LÓGICA DE CONTROL ---
    // Lista observable: cualquier cambio aquí actualiza la TableView automáticamente
    private ObservableList<Movement> masterData = FXCollections.observableArrayList();
    private Customer currentCustomer; // Usuario que tiene la sesión activa
    
    // Clientes para realizar peticiones HTTP a la API REST
    private AccountRESTClient accountClient;
    private MovementRESTClient movementClient;

    // Flag para ignorar eventos del ComboBox cuando cambiamos la selección por código
    private boolean isProgrammaticUpdate = false;
    
    // Formateador estándar de fechas para la visualización en tabla
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Método de inicialización de la controladora (ciclo de vida de JavaFX).
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try{
        // 1. Instancia de los conectores REST para evitar NullPointerException
        accountClient = new AccountRESTClient();
        movementClient = new MovementRESTClient();

        // 2. CONFIGURACIÓN DEL COMBOBOX DE CUENTAS
        // Define cómo mostrar el objeto Account en el texto del combo
        StringConverter<Account> converter = new StringConverter<Account>() {
            @Override
            public String toString(Account account) {
                if (account == null) return null;
                return account.getId() + " - " + account.getType(); // Ej: "1 - STANDARD"
            }
            @Override
            public Account fromString(String string) { return null; } 
        };
        
        cbAccountSelector.setConverter(converter);
        
        // Personalización de las celdas del desplegable para usar el convertidor anterior
        cbAccountSelector.setCellFactory(lv -> new ListCell<Account>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : converter.toString(item));
            }
        });
        cbAccountSelector.setButtonCell(new ListCell<Account>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : converter.toString(item));
            }
        });

        // 3. EVENTO DE CAMBIO DE SELECCIÓN EN CUENTAS
        cbAccountSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !isProgrammaticUpdate) {
                lblAccountId.setText(String.valueOf(newVal.getId()));
                
                // Lógica visual para límites de crédito
                if (lblCreditLimit != null) {
                    String typeStr = (newVal.getType() != null) ? newVal.getType().toString().toUpperCase() : "";
                    if (typeStr.contains("CREDIT")) {
                        double limit = (newVal.getCreditLine() != null) ? newVal.getCreditLine() : 0.0;
                        lblCreditLimit.setText(String.format("%.2f €", limit));
                    } else {
                        lblCreditLimit.setText("N/A");
                    }
                }
                // Al cambiar la cuenta, cargamos sus movimientos específicos
                loadMovementsForAccount(newVal);
            }
        });

        // 4. Configuración de factorías de celdas (CellFactories)
        setupColumns();

        // 5. Vinculación de la lista maestra con el control visual
        tvMovements.setItems(masterData);

        // 6. ATAJO DE TECLADO: Cerrar con tecla ESCAPE
        Platform.runLater(() -> {
            if (tvMovements.getScene() != null) {
                tvMovements.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        handleExit(null);
                        event.consume();
                    }
                });
            }
        });
        
        // 7. Configuración estética del título de la ventana
        Platform.runLater(() -> {
            if (lblUserId.getScene() != null) {
                Stage stage = (Stage) lblUserId.getScene().getWindow();
                stage.setTitle("Movements"); 
            }
        });
        
        if (menuController != null) {
                menuController.setActiveController(this);
            }
    }
    catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al inicializar MovementController", e);
        }
    }

    /**
     * Configuración técnica de las columnas: visualización, formateo y edición.
     */
    private void setupColumns() {
        // A) COLUMNA FECHA: Transforma Date (Server) a LocalDate (UI)
        colDate.setCellValueFactory(cellData -> {
            if(cellData.getValue().getTimestamp() != null){
                return new SimpleObjectProperty<>(cellData.getValue().getTimestamp().toInstant()
                                          .atZone(ZoneId.systemDefault()).toLocalDate());
            }
            return null;
        });
        // Formatea la fecha para el usuario
        colDate.setCellFactory(column -> new TableCell<Movement, LocalDate>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : dateFormatter.format(item));
            }
        });

        // B) COLUMNA TIPO: Menú desplegable editable dentro de la tabla
        colType.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));
        colType.setCellFactory(ComboBoxTableCell.forTableColumn("Deposit", "Payment"));
        colType.setOnEditCommit(e -> {
            // Solo se permite editar el tipo si el movimiento aún no existe en la base de datos (ID 0)
            if (isNewRow(e.getRowValue())) {
                e.getRowValue().setDescription(e.getNewValue());
                // UX: Salta automáticamente a la celda Amount para agilizar la entrada
                tvMovements.getSelectionModel().select(e.getTablePosition().getRow(), colAmount);
            } else {
                tvMovements.refresh(); // Revierte el cambio si es un dato persistido
            }
        });

        // C) COLUMNA CANTIDAD: Campo de texto editable con validación de números
        colAmount.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getAmount()));
        colAmount.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<Double>() {
            @Override
            public String toString(Double object) { return object == null ? "0.00" : object.toString(); }

            @Override
            public Double fromString(String string) {
                if (string == null || string.trim().isEmpty()) return 0.0;
                try { 
                    return Double.parseDouble(string.replace(",", ".")); 
                } catch (NumberFormatException e) { 
                    Platform.runLater(() -> showError("Formato incorrecto. Introduce un número válido."));
                    return 0.0; 
                }
            }
        }));
        
        // Acción al confirmar la edición del importe
        colAmount.setOnEditCommit(e -> {
            Movement m = e.getRowValue();
            if (isNewRow(m)) {
                Double val = e.getNewValue();
                // REGLA DE NEGOCIO: Si el tipo es Pago, el importe se fuerza a negativo
                if ("Payment".equalsIgnoreCase(m.getDescription()) && val > 0) val *= -1;
                m.setAmount(val);
                // Intento de guardado en el servidor
                createMovementOnServer(m);
            } else {
                tvMovements.refresh();
            }
        });

        // D) COLUMNA BALANCE: Muestra el saldo acumulativo (no se edita, se calcula)
        colBalance.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        colBalance.setCellFactory(column -> new TableCell<Movement, Movement>() {
            @Override
            protected void updateItem(Movement item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : String.format("%.2f €", item.getBalance()));
            }
        });
    }

    // --- MÉTODOS DE CARGA DE DATOS ---

    /**
     * Inicializa los datos del cliente desde la ventana de navegación anterior.
     */
    public void setClientData(Customer customer) {
        this.currentCustomer = customer;
        lblCustomerName.setText((customer.getFirstName()!=null ? customer.getFirstName() : "Cliente") + " " + 
                                (customer.getLastName()!=null ? customer.getLastName() : ""));
        lblUserId.setText(String.valueOf(customer.getId()));
        
        loadUserAccounts(true); // Carga inicial de cuentas
    }

    /**
     * Recupera las cuentas bancarias asociadas al cliente desde el servicio REST.
     */
    private void loadUserAccounts(boolean selectFirst) {
        try {
            GenericType<List<Account>> listType = new GenericType<List<Account>>() {};
            List<Account> accounts = accountClient.findAccountsByCustomerId_XML(listType, String.valueOf(currentCustomer.getId()));

            isProgrammaticUpdate = true; // Evitamos disparar el listener durante el refresco
            
            Account current = cbAccountSelector.getValue();
            cbAccountSelector.setItems(FXCollections.observableArrayList(accounts));

            // Mantenemos la selección previa si es posible
            if (current != null) {
                for (Account a : accounts) {
                    if (a.getId().equals(current.getId())) {
                        cbAccountSelector.setValue(a);
                        break;
                    }
                }
            } 
            // Si es la carga inicial, seleccionamos la primera cuenta disponible
            else if (selectFirst && !accounts.isEmpty()) {
                cbAccountSelector.getSelectionModel().selectFirst();
                loadMovementsForAccount(cbAccountSelector.getValue());
            }
            
            isProgrammaticUpdate = false;

        } catch (Exception e) {
            lblStatus.setText("Error cargando cuentas: " + e.getMessage());
        }
    }

    /**
     * Descarga y muestra los movimientos financieros de una cuenta seleccionada.
     */
    private void loadMovementsForAccount(Account account) {
        if (account == null) return;

        try {
            GenericType<List<Movement>> listType = new GenericType<List<Movement>>() {};
            List<Movement> movementsList = movementClient.findMovementByAccount_XML(listType, String.valueOf(account.getId()));

            masterData.clear();
            masterData.addAll(movementsList);

            // IMPORTANTE: Calculamos los saldos línea a línea localmente
            recalculateLocalBalances();
            lblStatus.setText("Movimientos cargados: " + movementsList.size());

        } catch (Exception e) {
            lblStatus.setText("Error al cargar movimientos.");
            e.printStackTrace();
        }
    }

    /**
     * Algoritmo de cálculo de saldos parciales.
     * Ordena cronológicamente y acumula importes sobre el saldo inicial de la cuenta.
     */
    private void recalculateLocalBalances() {
        // 1. Ordenación por fecha (Timestamp)
        masterData.sort((m1, m2) -> {
            if (m1.getTimestamp() == null || m2.getTimestamp() == null) return 0;
            return m1.getTimestamp().compareTo(m2.getTimestamp());
        });

        // 2. Determinar punto de partida (Balance de apertura)
        double runningBalance = 0.0;
        Account selectedAccount = cbAccountSelector.getValue();
        if (selectedAccount != null && selectedAccount.getBeginBalance() != null) {
            runningBalance = selectedAccount.getBeginBalance();
        }

        // 3. Iteración para cálculo acumulativo
        for (Movement m : masterData) {
            runningBalance += m.getAmount();
            m.setBalance(runningBalance); // Se guarda en el objeto para que la tabla lo muestre
        }

        // 4. Actualización de la UI en el hilo principal
        final double finalBalance = runningBalance;
        Platform.runLater(() -> {
            tfBalance.setText(String.format("%.2f €", finalBalance));
            tvMovements.refresh(); // Forzamos el repintado para actualizar la columna Balance
        });
    }

    // --- ACCIONES CRUD (Create, Remove) ---

    /**
     * Inserta una fila vacía en la tabla para permitir al usuario crear un nuevo movimiento.
     */
    @FXML
    void handleNewRow(ActionEvent event) {
        if (cbAccountSelector.getValue() == null) return;

        Movement newMov = new Movement();
        newMov.setTimestamp(new Date());
        newMov.setDescription("Deposit");
        newMov.setAmount(0.0);
        
        // Cálculo preventivo del saldo para la nueva fila
        try {
            String txt = tfBalance.getText().replace(" €","").replace(",",".");
            newMov.setBalance(Double.parseDouble(txt));
        } catch(Exception e) { newMov.setBalance(0.0); }

        masterData.add(newMov);
        
        // Foco automático en la nueva fila para iniciar edición
        Platform.runLater(() -> {
            int targetIndex = tvMovements.getItems().indexOf(newMov);
            
            if (targetIndex >= 0) {
                tvMovements.getSelectionModel().clearSelection();
                tvMovements.getSelectionModel().select(targetIndex);
                tvMovements.requestFocus(); 
                tvMovements.scrollTo(targetIndex);
                
                final int rowToEdit = targetIndex;
                Platform.runLater(() -> {
                    tvMovements.layout(); 
                    tvMovements.edit(rowToEdit, colType);
                });
            }
        });
    }

    /**
     * Persiste un movimiento en el servidor previa validación de saldo.
     */
    private void createMovementOnServer(Movement mov) {
        try {
            Account acc = cbAccountSelector.getValue();
            double currentBal = 0.0;
            
            // Predicción del nuevo saldo
            try {
                String txt = tfBalance.getText().replace(" €","").replace(",",".");
                currentBal = Double.parseDouble(txt) + mov.getAmount();
            } catch (Exception e) {
                double start = (acc.getBeginBalance() != null) ? acc.getBeginBalance() : 0.0;
                currentBal = start + mov.getAmount();
            }
            
            // VALIDACIÓN BANCARIA: Fondos suficientes según el tipo de cuenta
            if (mov.getAmount() < 0) {
                String typeStr = (acc.getType() != null) ? acc.getType().toString().toUpperCase() : "";

                // Cuenta Standard: No permite saldo negativo
                if (typeStr.contains("STANDARD")) { 
                    if (currentBal < 0) {
                        showError("Fondos insuficientes (Cuenta Standard).");
                        masterData.remove(mov);
                        tvMovements.refresh();
                        return;
                    }
                } 
                // Cuenta Crédito: Permite negativo hasta el límite de la línea de crédito
                else if (typeStr.contains("CREDIT")) {
                    double limit = (acc.getCreditLine() != null) ? acc.getCreditLine() : 0.0;
                    if (currentBal < -limit) {
                        showError("Límite de crédito excedido (" + limit + " €).");
                        masterData.remove(mov);
                        tvMovements.refresh();
                        return;
                    }
                }
            }

            // Si las validaciones pasan, se envía al servidor REST
            mov.setBalance(currentBal); 
            mov.setAccount(acc);
            movementClient.create_XML(mov, String.valueOf(acc.getId()));
            
            reloadEverything(); // Sincronización final con el estado del servidor
            lblStatus.setText("Guardado exitosamente.");
            
        } catch (Exception e) {
            masterData.remove(mov); // Reversión en UI si falla la red
            lblStatus.setText("Error al guardar.");
            showError("Error técnico: " + e.getMessage());
        }
    }

    /**
     * Elimina el último movimiento registrado (Función Deshacer).
     */
    @FXML
    void handleUndoLastMovement(ActionEvent event) {
        if (masterData.isEmpty()) return;
        
        if (!showConfirmation("¿Estás seguro de que deseas eliminar el último movimiento?")) {
            return;
        }

        Movement last = masterData.get(masterData.size()-1); 
        try {
            // Borrado físico en el servidor
            movementClient.remove(String.valueOf(last.getId()));
            
            // Actualización del balance de la cuenta tras el borrado
            Account acc = cbAccountSelector.getValue();
            acc.setBalance(acc.getBalance() - last.getAmount());
            accountClient.updateAccount_XML(acc);
            
            reloadEverything();
            lblStatus.setText("Deshecho.");
        } catch (Exception e) {
            lblStatus.setText("Error al deshacer.");
        }
    }

    // --- NAVEGACIÓN Y CIERRE ---

    /** Cierra la ventana actual para volver a la anterior */
    @FXML
    void handleBack(ActionEvent event) {
        try {
            Stage stage = (Stage) btBack.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            LOGGER.severe("Error al cerrar: " + e.getMessage());
        }
    }

    /** Cierra completamente la aplicación Java */
    @FXML 
    void handleExit(ActionEvent event) { 
        if (showConfirmation("¿Seguro que quieres salir de la aplicación?")) {
            System.exit(0); 
        }
    }

    /** Refresca la información de cuentas y movimientos para mantener la consistencia */
    private void reloadEverything() {
        Account current = cbAccountSelector.getValue();
        if(current == null) return;
        loadUserAccounts(false);
        // Restauración de la selección del usuario tras el refresco de datos
        for(Account a : cbAccountSelector.getItems()) {
            if(a.getId().equals(current.getId())) {
                isProgrammaticUpdate = true;
                cbAccountSelector.setValue(a);
                isProgrammaticUpdate = false;
                loadMovementsForAccount(a);
                break;
            }
        }
    }
    
    // Identifica si un objeto es nuevo (ID nulo o 0) o ya existe en la base de datos
    private boolean isNewRow(Movement m) { return m.getId() == null || m.getId() == 0; }
    
    /** Método público para seleccionar una cuenta específica desde un controlador externo */
    public void setPreselectedAccount(Account account) {
        if (account == null) return;

        for (Account a : cbAccountSelector.getItems()) {
            if (a.getId().equals(account.getId())) {
                cbAccountSelector.getSelectionModel().select(a); 
                break;
            }
        }
    }
    
    // --- HELPERS PARA DIÁLOGOS DE USUARIO ---
    
    private void showError(String msg) { 
        Alert a = new Alert(Alert.AlertType.ERROR); 
        a.setTitle("Error");
        a.setContentText(msg); 
        a.showAndWait(); 
    }

    private boolean showConfirmation(String msg) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmación");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}