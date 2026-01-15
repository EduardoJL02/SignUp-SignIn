/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package UI;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;
import logic.AccountRESTClient;
import logic.MovementRESTClient;
import model.Account;
import model.Customer;
import model.Movement;

import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador final para la gestión de Movimientos Bancarios.
 * Integra: CRUD REST, Recálculo visual de saldos y manejo de Arrays para JAX-RS.
 */
public class MovementController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(MovementController.class.getName());

    // --- ELEMENTOS FXML ---
    @FXML private Label lblCustomerName;
    @FXML private Label lblUserId;
    @FXML private Label lblAccountId;
    @FXML private ComboBox<Account> cbAccountSelector;
    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;
    @FXML private Button btSearch;
    @FXML private Button btNewRow;
    @FXML private Button btUndoLast;
    @FXML private TextField tfBalance;
    @FXML private Label lblStatus;

    // Tabla y Columnas
    @FXML private TableView<Movement> tvMovements;
    @FXML private TableColumn<Movement, LocalDate> colDate;
    @FXML private TableColumn<Movement, String> colType;   // Mapeado a description
    @FXML private TableColumn<Movement, Double> colAmount;
    @FXML private TableColumn<Movement, Double> colBalance; // Nueva columna

    // --- DATOS ---
    private ObservableList<Movement> masterData = FXCollections.observableArrayList();
    private FilteredList<Movement> filteredData;
    private Customer currentCustomer;
    
    // Clientes REST
    private AccountRESTClient accountClient;
    private MovementRESTClient movementClient;

    // Formato visual
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        accountClient = new AccountRESTClient();
        movementClient = new MovementRESTClient();

        // 1. Configurar Selector de Cuentas (Muestra ID y Saldo)
        cbAccountSelector.setConverter(new StringConverter<Account>() {
            @Override
            public String toString(Account account) {
                if (account == null) return null;
                return account.getId() + " (" + String.format("%.2f", account.getBalance()) + "€)";
            }
            @Override
            public Account fromString(String string) { return null; }
        });

        // Listener: Al cambiar de cuenta, cargar movimientos
        cbAccountSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                lblAccountId.setText(String.valueOf(newVal.getId()));
                loadMovementsForAccount(newVal);
                // El saldo visual se actualizará tras cargar los movimientos
            }
        });

        // 2. Configurar Columnas (Edición y Formato)
        setupColumns();

        // 3. Inicializar Filtros
        filteredData = new FilteredList<>(masterData, p -> true);
        tvMovements.setItems(filteredData);
    }

    private void setupColumns() {
        // A) FECHA (Solo lectura - dd/MM/yyyy)
        colDate.setCellValueFactory(cellData -> {
            if(cellData.getValue().getTimestamp() != null){
                return new SimpleObjectProperty<>(cellData.getValue().getTimestamp().toInstant()
                                  .atZone(ZoneId.systemDefault()).toLocalDate());
            }
            return null;
        });
        colDate.setCellFactory(column -> new TableCell<Movement, LocalDate>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(dateFormatter.format(item));
            }
        });
        colDate.setEditable(false); // No editable por usuario

        // B) TIPO (Editable - ComboBox)
        colType.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));
        colType.setCellFactory(ComboBoxTableCell.forTableColumn("Deposit", "Payment"));
        
        colType.setOnEditCommit(e -> {
            Movement m = e.getRowValue();
            if (isNewRow(m)) {
                m.setDescription(e.getNewValue());
                // Salta a la siguiente columna para facilitar el flujo
                tvMovements.getSelectionModel().select(e.getTablePosition().getRow(), colAmount);
            } else {
                tvMovements.refresh();
                showAlert("Acción no permitida", "No puedes editar el tipo de movimientos antiguos.");
            }
        });

        // C) AMOUNT (Editable - Trigger de Guardado)
        colAmount.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getAmount()));
        colAmount.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        
        colAmount.setOnEditCommit(e -> {
            Movement m = e.getRowValue();
            if (isNewRow(m)) {
                Double val = e.getNewValue();
                
                // Lógica: Si es "Payment", lo convertimos a negativo
                if ("Payment".equalsIgnoreCase(m.getDescription()) && val > 0) {
                    val = val * -1;
                }
                
                m.setAmount(val);
                createMovementOnServer(m); // GUARDAR EN BBDD
            } else {
                tvMovements.refresh();
                showAlert("Acción no permitida", "No puedes editar importes antiguos.");
            }
        });

        // D) BALANCE (Solo Lectura - Formato Moneda)
        colBalance.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getBalance()));
        colBalance.setCellFactory(column -> new TableCell<Movement, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(String.format("%.2f €", item));
            }
        });
    }

    // --- MÉTODOS DE CARGA DE DATOS ---

    public void setClientData(Customer customer) {
        this.currentCustomer = customer;
        
        // Evitamos textos "null" en la interfaz
        String name = (customer.getFirstName() != null) ? customer.getFirstName() : "Cliente";
        String last = (customer.getLastName() != null) ? customer.getLastName() : "";
        
        lblCustomerName.setText(name + " " + last);
        lblUserId.setText(String.valueOf(customer.getId()));
        
        loadUserAccounts(true); // Cargar cuentas y seleccionar la primera
    }

    private void loadUserAccounts(boolean selectFirst) {
        try {
            // USAMOS ARRAYS PARA EVITAR ERROR GENERIC TYPE
            Account[] accountsArray = accountClient.findAccountsByCustomerId_XML(
                    Account[].class, 
                    String.valueOf(currentCustomer.getId())
            );
            List<Account> accounts = Arrays.asList(accountsArray);
            
            // Intentar mantener la selección actual al recargar
            Account currentSelection = cbAccountSelector.getValue();
            
            cbAccountSelector.setItems(FXCollections.observableArrayList(accounts));
            
            if (selectFirst && !accounts.isEmpty() && currentSelection == null) {
                cbAccountSelector.getSelectionModel().selectFirst();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error cargando cuentas", e);
            lblStatus.setText("Error cargando cuentas.");
        }
    }

    private void loadMovementsForAccount(Account account) {
        try {
            // USAMOS ARRAYS PARA EVITAR ERROR GENERIC TYPE
            Movement[] movementsArray = movementClient.findMovementByAccount_XML(
                    Movement[].class, 
                    String.valueOf(account.getId())
            );
            
            masterData.clear();
            masterData.addAll(Arrays.asList(movementsArray));
            
            // IMPORTANTE: Recalcular saldos visualmente para corregir incoherencias de BBDD
            recalculateLocalBalances(account);
            
            lblStatus.setText("Datos actualizados.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error cargando movimientos", e);
            lblStatus.setText("Error de conexión con el servidor.");
        }
    }

    /**
     * Recalcula el saldo acumulado fila a fila basándose en los importes.
     * Esto corrige visualmente si la BBDD tiene un saldo total desfasado.
     */
    private void recalculateLocalBalances(Account account) {
        double runningBalance = 0.0;
        
        // Si usamos saldo inicial de la cuenta
        if (account.getBeginBalance() != null) {
            runningBalance = account.getBeginBalance();
        }

        // Ordenar cronológicamente
        masterData.sort((m1, m2) -> {
            if (m1.getTimestamp() == null || m2.getTimestamp() == null) return 0;
            return m1.getTimestamp().compareTo(m2.getTimestamp());
        });

        // Calcular acumulado
        for (Movement m : masterData) {
            runningBalance += m.getAmount();
            m.setBalance(runningBalance); 
        }

        // Actualizar etiqueta inferior y tabla
        updateBalanceDisplay(runningBalance);
        tvMovements.refresh();
    }

    // --- ACCIONES CRUD ---

    @FXML
    void handleNewRow(ActionEvent event) {
        if (cbAccountSelector.getValue() == null) {
            showAlert("Info", "Selecciona una cuenta primero.");
            return;
        }

        // Crear fila vacía para edición
        Movement newMov = new Movement();
        newMov.setTimestamp(new Date()); // Fecha de hoy automática
        newMov.setDescription("Deposit"); // Valor por defecto
        newMov.setAmount(0.0);
        // Balance temporal visual
        newMov.setBalance(cbAccountSelector.getValue().getBalance());

        masterData.add(newMov);
        
        // Seleccionar y hacer scroll
        int index = masterData.size() - 1;
        tvMovements.getSelectionModel().select(index);
        tvMovements.scrollTo(newMov);
        
        // Poner foco en la columna Tipo
        tvMovements.edit(index, colType);
        
        lblStatus.setText("Nueva fila: Selecciona Tipo -> Cantidad -> Enter.");
    }

    private void createMovementOnServer(Movement mov) {
        try {
            Account selectedAccount = cbAccountSelector.getValue();
            
            // Calculamos el nuevo saldo total que tendrá la cuenta
            Double newBalance = selectedAccount.getBalance() + mov.getAmount();
            
            mov.setBalance(newBalance);
            mov.setAccount(selectedAccount);

            // POST al servidor
            movementClient.create_XML(mov, String.valueOf(selectedAccount.getId()));
            
            // Recargar todo para refrescar la interfaz
            reloadCurrentAccountData(); 
            
            lblStatus.setText("Movimiento guardado correctamente.");
            
        } catch (Exception e) {
            LOGGER.severe("Error creando movimiento: " + e.getMessage());
            masterData.remove(mov); // Si falla, quitamos la línea
            showAlert("Error", "No se pudo conectar con el servidor.");
        }
    }

    @FXML
    void handleUndoLastMovement(ActionEvent event) {
        if (masterData.isEmpty()) {
            showAlert("Info", "No hay movimientos para deshacer.");
            return;
        }
        
        // Obtener último (cronológico)
        Movement last = masterData.get(masterData.size() - 1);
        
        try {
            // 1. Borrar movimiento
            movementClient.remove(String.valueOf(last.getId()));
            
            // 2. Actualizar saldo de la cuenta manualmente
            Account acc = cbAccountSelector.getValue();
            Double restoredBalance = acc.getBalance() - last.getAmount();
            acc.setBalance(restoredBalance);
            
            accountClient.updateAccount_XML(acc);
            
            // 3. Recargar interfaz
            reloadCurrentAccountData();
            
            lblStatus.setText("Undo completado. Saldo restaurado.");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error undo", e);
            showAlert("Error", "Falló la operación Undo.");
        }
    }

    /**
     * Método CRÍTICO: Fuerza la recarga completa de datos desde el servidor
     * y actualiza la visualización.
     */
    private void reloadCurrentAccountData() {
        Account currentSelection = cbAccountSelector.getValue();
        if (currentSelection == null) return;
        
        String currentId = String.valueOf(currentSelection.getId());

        // 1. Descargar cuentas de nuevo (para tener el saldo oficial actualizado)
        loadUserAccounts(false);

        // 2. Buscar y re-seleccionar la cuenta actual
        for (Account a : cbAccountSelector.getItems()) {
            if (String.valueOf(a.getId()).equals(currentId)) {
                cbAccountSelector.setValue(a);
                
                // 3. Forzar descarga de movimientos y recálculo
                loadMovementsForAccount(a);
                break;
            }
        }
    }

    // --- FILTROS Y UTILS ---

    @FXML
    void handleSearchByDates(ActionEvent event) {
        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();

        filteredData.setPredicate(m -> {
            if (m.getTimestamp() == null) return false;
            LocalDate date = m.getTimestamp().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            
            if (from != null && date.isBefore(from)) return false;
            if (to != null && date.isAfter(to)) return false;
            return true;
        });
        lblStatus.setText("Filtro aplicado.");
    }

    @FXML void handleExit(ActionEvent event) { System.exit(0); }

    private void updateBalanceDisplay(Double bal) { 
        tfBalance.setText(String.format("%.2f €", bal)); 
    }
    
    private boolean isNewRow(Movement m) { 
        return m.getId() == null || m.getId() == 0; 
    }
    
    private void showAlert(String t, String c) { 
        Alert a = new Alert(Alert.AlertType.INFORMATION); 
        a.setTitle(t); a.setHeaderText(null); a.setContentText(c); a.showAndWait(); 
    }
}