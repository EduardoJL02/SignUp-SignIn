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
import java.util.logging.Logger;
import javax.ws.rs.core.GenericType;

/**
 * Controlador para la ventana de Movimientos.
 * Esta clase conecta la tabla visual con el servidor REST.
 * Gestiona la carga de cuentas, visualización de movimientos y lógica de saldo.
 */
public class MovementController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(MovementController.class.getName());

    // --- ELEMENTOS DE LA UI (FXML) ---
    // Etiquetas de información superior
    @FXML private Label lblCustomerName;
    @FXML private Label lblUserId;
    @FXML private Label lblAccountId;
    @FXML private Label lblCreditLimit; // Muestra el límite si es cuenta crédito
    @FXML private ComboBox<Account> cbAccountSelector; // Selector de cuentas del usuario
    @FXML private TextField tfBalance; // Saldo total calculado
    @FXML private Label lblStatus; // Barra de estado inferior (mensajes de error/éxito)
    @FXML private Button btBack; 

    // --- TABLA Y COLUMNAS ---
    @FXML private TableView<Movement> tvMovements;
    @FXML private TableColumn<Movement, LocalDate> colDate;
    @FXML private TableColumn<Movement, String> colType; // Editable (ComboBox)
    @FXML private TableColumn<Movement, Double> colAmount; // Editable (TextField)
    @FXML private TableColumn<Movement, Movement> colBalance; // Calculado (No editable)

    // --- DATOS Y LÓGICA ---
    // 'masterData' es la lista que ve la tabla. Si cambio esto, la tabla cambia sola.
    private ObservableList<Movement> masterData = FXCollections.observableArrayList();
    private Customer currentCustomer; // El cliente que ha iniciado sesión
    
    // Clientes REST para hablar con el servidor
    private AccountRESTClient accountClient;
    private MovementRESTClient movementClient;

    // Flag para evitar bucles infinitos cuando cambio el ComboBox por código y no por clic
    private boolean isProgrammaticUpdate = false;
    
    // Formateador para que las fechas se vean como dd/MM/yyyy
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Instancio los clientes REST (IMPORTANTE: Sin esto da NullPointer)
        accountClient = new AccountRESTClient();
        movementClient = new MovementRESTClient();

        // 2. CONFIGURACIÓN DEL COMBOBOX DE CUENTAS
        // Defino cómo convertir un objeto Account a String para mostrarlo
        StringConverter<Account> converter = new StringConverter<Account>() {
            @Override
            public String toString(Account account) {
                if (account == null) return null;
                return account.getId() + " - " + account.getType(); // Ej: "1 - STANDARD"
            }
            @Override
            public Account fromString(String string) { return null; } // No necesito convertir texto a objeto aquí
        };
        
        cbAccountSelector.setConverter(converter);
        
        // Configuro las celdas de la lista desplegable y del botón cerrado
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

        // 3. LISTENER: ¿QUÉ PASA AL CAMBIAR DE CUENTA?
        cbAccountSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            // Solo actúo si hay valor nuevo y NO lo estoy cambiando yo por código
            if (newVal != null && !isProgrammaticUpdate) {
                // Actualizo etiquetas de la cabecera
                lblAccountId.setText(String.valueOf(newVal.getId()));
                
                // Lógica visual: Si es crédito muestro el límite, si no, "N/A"
                if (lblCreditLimit != null) {
                    String typeStr = (newVal.getType() != null) ? newVal.getType().toString().toUpperCase() : "";
                    if (typeStr.contains("CREDIT")) {
                        double limit = (newVal.getCreditLine() != null) ? newVal.getCreditLine() : 0.0;
                        lblCreditLimit.setText(String.format("%.2f €", limit));
                    } else {
                        lblCreditLimit.setText("N/A");
                    }
                }
                // Cargo los movimientos de esta nueva cuenta
                loadMovementsForAccount(newVal);
            }
        });

        // 4. Configuro las columnas de la tabla (factorías de celdas)
        setupColumns();

        // 5. Enlazo la lista observable con la tabla
        tvMovements.setItems(masterData);

        // 6. ATAJO DE TECLADO: ESC para salir
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
        
        // 7. Poner título a la ventana (detalle estético)
        Platform.runLater(() -> {
            if (lblUserId.getScene() != null) {
                Stage stage = (Stage) lblUserId.getScene().getWindow();
                stage.setTitle("Movements"); 
            }
        });
    }

    /**
     * Configuración de cómo se muestran y editan las columnas.
     * Entender CellValueFactory vs CellFactory.
     */
    private void setupColumns() {
        // A) COLUMNA FECHA
        colDate.setCellValueFactory(cellData -> {
            if(cellData.getValue().getTimestamp() != null){
                // Convierto Date (antiguo) a LocalDate (moderno) para JavaFX
                return new SimpleObjectProperty<>(cellData.getValue().getTimestamp().toInstant()
                                          .atZone(ZoneId.systemDefault()).toLocalDate());
            }
            return null;
        });
        // Formateo visual de la fecha
        colDate.setCellFactory(column -> new TableCell<Movement, LocalDate>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : dateFormatter.format(item));
            }
        });

        // B) COLUMNA TIPO (Editable con ComboBox)
        colType.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));
        colType.setCellFactory(ComboBoxTableCell.forTableColumn("Deposit", "Payment")); // Opciones fijas
        colType.setOnEditCommit(e -> {
            // Solo dejo editar si es una fila NUEVA (no persistida en BD)
            if (isNewRow(e.getRowValue())) {
                e.getRowValue().setDescription(e.getNewValue());
                // Truco de usabilidad: Salto a la siguiente columna (Amount) automáticamente
                tvMovements.getSelectionModel().select(e.getTablePosition().getRow(), colAmount);
            } else {
                tvMovements.refresh(); // Si es vieja, revierto cambios visuales
            }
        });

        // C) COLUMNA CANTIDAD (Editable con TextField)
        colAmount.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getAmount()));
        colAmount.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<Double>() {
            @Override
            public String toString(Double object) { return object == null ? "0.00" : object.toString(); }
            @Override
            public Double fromString(String string) {
                if (string == null || string.trim().isEmpty()) return 0.0;
                try { return Double.parseDouble(string.replace(",", ".")); } // Manejo decimales con coma o punto
                catch (NumberFormatException e) { return 0.0; }
            }
        }));
        
        // Al terminar de editar la cantidad...
        colAmount.setOnEditCommit(e -> {
            Movement m = e.getRowValue();
            if (isNewRow(m)) {
                Double val = e.getNewValue();
                // Lógica de negocio: Si dice "Payment" (pago), lo convierto a negativo
                if ("Payment".equalsIgnoreCase(m.getDescription()) && val > 0) val *= -1;
                m.setAmount(val);
                // Llamo al servidor para guardar
                createMovementOnServer(m);
            } else {
                tvMovements.refresh();
            }
        });

        // D) COLUMNA BALANCE (Calculada)
        // Paso el objeto entero 'Movement' para poder acceder al balance calculado
        colBalance.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        colBalance.setCellFactory(column -> new TableCell<Movement, Movement>() {
            @Override
            protected void updateItem(Movement item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : String.format("%.2f €", item.getBalance()));
            }
        });
    }

    // --- CARGA DE DATOS ---

    /**
     * Método llamado desde la ventana anterior (Login/Accounts) para pasarme el usuario.
     */
    public void setClientData(Customer customer) {
        this.currentCustomer = customer;
        lblCustomerName.setText((customer.getFirstName()!=null ? customer.getFirstName() : "Cliente") + " " + 
                                (customer.getLastName()!=null ? customer.getLastName() : ""));
        lblUserId.setText(String.valueOf(customer.getId()));
        
        // Cargo las cuentas de este cliente
        loadUserAccounts(true);
    }

    /**
     * Carga las cuentas del servidor.
     * Uso GenericType porque Jersey necesita saber qué tipo de lista devuelve el XML.
     */
    private void loadUserAccounts(boolean selectFirst) {
        try {
            // Defino el tipo para mapear la respuesta XML a una List<Account>
            GenericType<List<Account>> listType = new GenericType<List<Account>>() {};

            List<Account> accounts = accountClient.findAccountsByCustomerId_XML(listType, String.valueOf(currentCustomer.getId()));

            isProgrammaticUpdate = true; // Bloqueo el listener para no disparar eventos a lo loco
            
            Account current = cbAccountSelector.getValue();
            cbAccountSelector.setItems(FXCollections.observableArrayList(accounts));

            // Intento mantener la selección anterior si existía
            if (current != null) {
                for (Account a : accounts) {
                    if (a.getId().equals(current.getId())) {
                        cbAccountSelector.setValue(a);
                        break;
                    }
                }
            } 
            // Si no, selecciono la primera por defecto
            else if (selectFirst && !accounts.isEmpty()) {
                cbAccountSelector.getSelectionModel().selectFirst();
                loadMovementsForAccount(cbAccountSelector.getValue());
            }
            
            isProgrammaticUpdate = false; // Desbloqueo el listener

        } catch (Exception e) {
            lblStatus.setText("Error cargando cuentas: " + e.getMessage());
        }
    }

    /**
     * Descarga los movimientos de la cuenta seleccionada.
     */
    private void loadMovementsForAccount(Account account) {
        if (account == null) return;

        try {
            GenericType<List<Movement>> listType = new GenericType<List<Movement>>() {};
            List<Movement> movementsList = movementClient.findMovementByAccount_XML(listType, String.valueOf(account.getId()));

            masterData.clear();
            masterData.addAll(movementsList);

            // IMPORTANTE: Recalcular saldos parciales localmente
            recalculateLocalBalances();
            lblStatus.setText("Movimientos cargados: " + movementsList.size());

        } catch (Exception e) {
            lblStatus.setText("Error al cargar movimientos.");
            e.printStackTrace();
        }
    }

    /**
     * Algoritmo para calcular el saldo línea a línea.
     * Ordena por fecha y va sumando acumulativamente desde el saldo inicial de la cuenta.
     */
    private void recalculateLocalBalances() {
        // 1. Ordenar por fecha
        masterData.sort((m1, m2) -> {
            if (m1.getTimestamp() == null || m2.getTimestamp() == null) return 0;
            return m1.getTimestamp().compareTo(m2.getTimestamp());
        });

        // 2. Obtener saldo inicial de la cuenta
        double runningBalance = 0.0;
        Account selectedAccount = cbAccountSelector.getValue();
        if (selectedAccount != null && selectedAccount.getBeginBalance() != null) {
            runningBalance = selectedAccount.getBeginBalance();
        }

        // 3. Iterar y sumar
        for (Movement m : masterData) {
            runningBalance += m.getAmount();
            m.setBalance(runningBalance); // Guardo el saldo parcial en el objeto (transitorio)
        }

        // 4. Actualizar UI
        final double finalBalance = runningBalance;
        Platform.runLater(() -> {
            tfBalance.setText(String.format("%.2f €", finalBalance));
            tvMovements.refresh(); // Fuerzo repintado de la tabla para ver los balances nuevos
        });
    }

    // --- CRUD (Create, Remove) ---

    /**
     * Crea una fila vacía en la tabla para empezar a insertar.
     */
    @FXML
    void handleNewRow(ActionEvent event) {
        if (cbAccountSelector.getValue() == null) return;

        Movement newMov = new Movement();
        newMov.setTimestamp(new Date());
        newMov.setDescription("Deposit");
        newMov.setAmount(0.0);
        newMov.setBalance(0.0); // Temporal

        masterData.add(newMov);
        
        // Foco y edición automática para mejorar UX
        Platform.runLater(() -> {
            int targetIndex = tvMovements.getItems().indexOf(newMov);
            if (targetIndex >= 0) {
                tvMovements.getSelectionModel().clearSelection();
                tvMovements.getSelectionModel().select(targetIndex);
                tvMovements.scrollTo(targetIndex);
                
                // Pongo la celda "Type" en modo edición directamente
                tvMovements.edit(targetIndex, colType);
            }
        });
    }

    /**
     * Envía el movimiento al servidor. Contiene VALIDACIONES DE NEGOCIO.
     */
    private void createMovementOnServer(Movement mov) {
        try {
            Account acc = cbAccountSelector.getValue();
            double currentBal = 0.0;
            
            // Calculo el saldo que quedaría
            try {
                String txt = tfBalance.getText().replace(" €","").replace(",",".");
                currentBal = Double.parseDouble(txt) + mov.getAmount();
            } catch (Exception e) {
                double start = (acc.getBeginBalance() != null) ? acc.getBeginBalance() : 0.0;
                currentBal = start + mov.getAmount();
            }
            
            // VALIDACIÓN: ¿Tiene fondos suficientes?
            if (mov.getAmount() < 0) {
                String typeStr = (acc.getType() != null) ? acc.getType().toString().toUpperCase() : "";

                if (typeStr.contains("STANDARD")) { 
                    // Cuentas estándar no pueden tener saldo negativo
                    if (currentBal < 0) {
                        showError("Fondos insuficientes (Cuenta Standard).");
                        masterData.remove(mov); // Borro la fila provisional
                        tvMovements.refresh();
                        return;
                    }
                } 
                else if (typeStr.contains("CREDIT")) {
                    // Cuentas crédito tienen un límite negativo
                    double limit = (acc.getCreditLine() != null) ? acc.getCreditLine() : 0.0;
                    if (currentBal < -limit) {
                        showError("Límite de crédito excedido (" + limit + " €).");
                        masterData.remove(mov);
                        tvMovements.refresh();
                        return;
                    }
                }
            }

            // Si pasa validaciones, asigno datos finales y envío
            mov.setBalance(currentBal); 
            mov.setAccount(acc);
            movementClient.create_XML(mov, String.valueOf(acc.getId()));
            
            reloadEverything(); // Recargo para asegurar consistencia con servidor
            lblStatus.setText("Guardado exitosamente.");
            
        } catch (Exception e) {
            masterData.remove(mov); // Si falla, quito la fila
            lblStatus.setText("Error al guardar.");
            showError("Error técnico: " + e.getMessage());
        }
    }

    /**
     * Borra el último movimiento de la lista (Deshacer).
     */
    @FXML
    void handleUndoLastMovement(ActionEvent event) {
        if (masterData.isEmpty()) return;
        
        if (!showConfirmation("¿Estás seguro de que deseas eliminar el último movimiento?")) {
            return;
        }

        Movement last = masterData.get(masterData.size()-1); // Cojo el último
        try {
            // Borro en servidor
            movementClient.remove(String.valueOf(last.getId()));
            
            // Actualizo saldo de cuenta en servidor (lógica manual, depende del backend)
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

    @FXML
    void handleBack(ActionEvent event) {
        try {
            // Cierro esta ventana (Stage). Como es modal, se verá la de abajo (Accounts)
            Stage stage = (Stage) btBack.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            LOGGER.severe("Error al cerrar: " + e.getMessage());
        }
    }

    @FXML 
    void handleExit(ActionEvent event) { 
        if (showConfirmation("¿Seguro que quieres salir de la aplicación?")) {
            System.exit(0); // Mata todo el proceso Java
        }
    }

    // --- UTILIDADES ---

    /** Recarga cuentas y movimientos para refrescar la vista completa */
    private void reloadEverything() {
        Account current = cbAccountSelector.getValue();
        if(current == null) return;
        loadUserAccounts(false);
        // Restauro la selección
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
    
    // Helper para saber si una fila es nueva (ID nulo o 0) o viene de BD
    private boolean isNewRow(Movement m) { return m.getId() == null || m.getId() == 0; }
    
    /**
     * Permite preseleccionar una cuenta desde fuera antes de mostrar la ventana.
     */
    public void setPreselectedAccount(Account account) {
        if (account == null) return;

        for (Account a : cbAccountSelector.getItems()) {
            if (a.getId().equals(account.getId())) {
                cbAccountSelector.getSelectionModel().select(a); // Esto dispara el listener
                break;
            }
        }
    }
    
    // Helpers para Alertas
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