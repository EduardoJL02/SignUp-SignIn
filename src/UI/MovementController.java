/*
 * Controlador DEFINITIVO: Saldo Inicial, Validaciones, Decimales Flexibles y Corrección de Scroll.
 */
package UI;

import javafx.application.Platform;
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
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Controlador principal para la gestión de Movimientos Bancarios.
 * <p>
 * Esta clase maneja la interfaz de usuario (JavaFX).
 * Versión corregida: Al crear nueva fila, el foco viaja correctamente a la nueva línea visual.
 * </p>
 * @author Pablo
 * @version 1.3 Scroll Fix
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
    @FXML private TextField tfBalance;
    @FXML private Label lblStatus;

    // --- TABLA Y COLUMNAS ---
    @FXML private TableView<Movement> tvMovements;
    @FXML private TableColumn<Movement, LocalDate> colDate;
    @FXML private TableColumn<Movement, String> colType;
    @FXML private TableColumn<Movement, Double> colAmount;
    @FXML private TableColumn<Movement, Movement> colBalance;

    // --- ESTRUCTURAS DE DATOS ---
    private ObservableList<Movement> masterData = FXCollections.observableArrayList();
    private FilteredList<Movement> filteredData;
    private Customer currentCustomer;
    
    // --- CLIENTES REST ---
    private AccountRESTClient accountClient;
    private MovementRESTClient movementClient;

    private boolean isProgrammaticUpdate = false;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        accountClient = new AccountRESTClient();
        movementClient = new MovementRESTClient();

        // 1. CONFIGURAR COMBOBOX
        StringConverter<Account> converter = new StringConverter<Account>() {
            @Override
            public String toString(Account account) {
                if (account == null) return null;
                return account.getId() + " - " + account.getType();
            }
            @Override
            public Account fromString(String string) { return null; }
        };
        
        cbAccountSelector.setConverter(converter);
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

        // 2. LISTENER DE SELECCIÓN
        cbAccountSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !isProgrammaticUpdate) {
                lblAccountId.setText(String.valueOf(newVal.getId()));
                loadMovementsForAccount(newVal);
            }
        });

        // 3. CONFIGURAR COLUMNAS
        setupColumns();

        // 4. INICIALIZAR FILTROS
        filteredData = new FilteredList<>(masterData, p -> true);
        tvMovements.setItems(filteredData);
    }

    private void setupColumns() {
        // --- A) FECHA ---
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
                setText((empty || item == null) ? null : dateFormatter.format(item));
            }
        });

        // --- B) TIPO ---
        colType.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));
        colType.setCellFactory(ComboBoxTableCell.forTableColumn("Deposit", "Payment"));
        colType.setOnEditCommit(e -> {
            if (isNewRow(e.getRowValue())) {
                e.getRowValue().setDescription(e.getNewValue());
                tvMovements.getSelectionModel().select(e.getTablePosition().getRow(), colAmount);
            } else {
                tvMovements.refresh();
            }
        });

        // --- C) CANTIDAD (Punto o Coma) ---
        colAmount.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getAmount()));
        colAmount.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<Double>() {
            @Override
            public String toString(Double object) {
                return object == null ? "0.00" : object.toString();
            }
            @Override
            public Double fromString(String string) {
                if (string == null || string.trim().isEmpty()) return 0.0;
                try {
                    return Double.parseDouble(string.replace(",", "."));
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            }
        }));
        colAmount.setOnEditCommit(e -> {
            Movement m = e.getRowValue();
            if (isNewRow(m)) {
                Double val = e.getNewValue();
                if ("Payment".equalsIgnoreCase(m.getDescription()) && val > 0) val *= -1;
                m.setAmount(val);
                createMovementOnServer(m);
            } else {
                tvMovements.refresh();
            }
        });

        // --- D) BALANCE ---
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

    public void setClientData(Customer customer) {
        this.currentCustomer = customer;
        lblCustomerName.setText((customer.getFirstName()!=null ? customer.getFirstName() : "Cliente") + " " + 
                                (customer.getLastName()!=null ? customer.getLastName() : ""));
        lblUserId.setText(String.valueOf(customer.getId()));
        loadUserAccounts(true);
    }

    private void loadUserAccounts(boolean selectFirst) {
        try {
            Account[] accounts = accountClient.findAccountsByCustomerId_XML(Account[].class, String.valueOf(currentCustomer.getId()));
            
            isProgrammaticUpdate = true;
            Account current = cbAccountSelector.getValue();
            cbAccountSelector.setItems(FXCollections.observableArrayList(accounts));
            
            if (current != null) {
                for(Account a : accounts) {
                    if(a.getId().equals(current.getId())) {
                        cbAccountSelector.setValue(a);
                        break;
                    }
                }
            } else if (selectFirst && accounts.length > 0) {
                cbAccountSelector.getSelectionModel().selectFirst();
                loadMovementsForAccount(cbAccountSelector.getValue());
            }
            isProgrammaticUpdate = false;
        } catch (Exception e) {
            lblStatus.setText("Error cargando cuentas.");
        }
    }

    private void loadMovementsForAccount(Account account) {
        try {
            Movement[] movements = movementClient.findMovementByAccount_XML(Movement[].class, String.valueOf(account.getId()));
            masterData.clear();
            masterData.addAll(Arrays.asList(movements));
            recalculateLocalBalances();
            lblStatus.setText("Datos cargados correctamente.");
        } catch (Exception e) {
            lblStatus.setText("Error de conexión.");
            e.printStackTrace();
        }
    }

    private void recalculateLocalBalances() {
        masterData.sort((m1, m2) -> {
            if (m1.getTimestamp() == null || m2.getTimestamp() == null) return 0;
            return m1.getTimestamp().compareTo(m2.getTimestamp());
        });

        double runningBalance = 0.0;
        Account selectedAccount = cbAccountSelector.getValue();
        
        if (selectedAccount != null && selectedAccount.getBeginBalance() != null) {
            runningBalance = selectedAccount.getBeginBalance();
        }

        for (Movement m : masterData) {
            runningBalance += m.getAmount();
            m.setBalance(runningBalance);
        }

        final double finalBalance = runningBalance;
        Platform.runLater(() -> {
            tfBalance.setText(String.format("%.2f €", finalBalance));
            tvMovements.refresh();
        });
    }

    // --- CRUD ---

    @FXML
    void handleNewRow(ActionEvent event) {
        if (cbAccountSelector.getValue() == null) return;

        Movement newMov = new Movement();
        newMov.setTimestamp(new Date());
        newMov.setDescription("Deposit");
        newMov.setAmount(0.0);
        
        try {
            String txt = tfBalance.getText().replace(" €","").replace(",",".");
            newMov.setBalance(Double.parseDouble(txt));
        } catch(Exception e) { newMov.setBalance(0.0); }

        // 1. Añadir el dato
        masterData.add(newMov);
        
        // 2. Primer runLater: Busca la fila, selecciónala y haz SCROLL
        Platform.runLater(() -> {
            tvMovements.requestFocus(); 
            
            // Buscar índice por referencia (seguro)
            int targetIndex = -1;
            ObservableList<Movement> items = tvMovements.getItems();
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i) == newMov) {
                    targetIndex = i;
                    break;
                }
            }
            
            if (targetIndex >= 0) {
                tvMovements.getSelectionModel().clearSelection();
                tvMovements.getSelectionModel().select(targetIndex);
                tvMovements.scrollTo(targetIndex);
                
                // 3. EL TRUCO: Segundo runLater anidado
                // Esperamos a que el Scroll termine visualmente para activar la edición
                final int rowToEdit = targetIndex;
                Platform.runLater(() -> {
                    tvMovements.edit(rowToEdit, colType);
                });
            }
        });
    }

    private void createMovementOnServer(Movement mov) {
        try {
            Account acc = cbAccountSelector.getValue();
            double currentBal = 0.0;
            try {
                String txt = tfBalance.getText().replace(" €","").replace(",",".");
                currentBal = Double.parseDouble(txt) + mov.getAmount();
            } catch (Exception e) {
                double start = (acc.getBeginBalance() != null) ? acc.getBeginBalance() : 0.0;
                currentBal = start + mov.getAmount();
            }
            
            // VALIDACIÓN
            if (mov.getAmount() < 0) {
                String typeStr = (acc.getType() != null) ? acc.getType().toString().toUpperCase() : "";

                if (typeStr.contains("STANDARD")) { 
                    if (currentBal < 0) {
                        showError("Fondos insuficientes (Cuenta Standard).");
                        masterData.remove(mov);
                        tvMovements.refresh();
                        return;
                    }
                } else if (typeStr.contains("CREDIT")) {
                    double limit = (acc.getCreditLine() != null) ? acc.getCreditLine() : 0.0;
                    if (currentBal < -limit) {
                        showError("Límite de crédito excedido (" + limit + " €).");
                        masterData.remove(mov);
                        tvMovements.refresh();
                        return;
                    }
                }
            }

            mov.setBalance(currentBal); 
            mov.setAccount(acc);
            movementClient.create_XML(mov, String.valueOf(acc.getId()));
            reloadEverything(); 
            lblStatus.setText("Guardado exitosamente.");
        } catch (Exception e) {
            masterData.remove(mov); 
            lblStatus.setText("Error al guardar.");
            e.printStackTrace();
            showError("Error técnico: " + e.getMessage());
        }
    }

    @FXML
    void handleUndoLastMovement(ActionEvent event) {
        if (masterData.isEmpty()) return;
        Movement last = masterData.get(masterData.size()-1);
        try {
            movementClient.remove(String.valueOf(last.getId()));
            Account acc = cbAccountSelector.getValue();
            acc.setBalance(acc.getBalance() - last.getAmount());
            accountClient.updateAccount_XML(acc);
            reloadEverything();
            lblStatus.setText("Deshecho.");
        } catch (Exception e) {
            lblStatus.setText("Error al deshacer.");
        }
    }

    private void reloadEverything() {
        Account current = cbAccountSelector.getValue();
        if(current == null) return;
        loadUserAccounts(false);
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

    @FXML
    void handleSearchByDates(ActionEvent event) {
        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();
        filteredData.setPredicate(m -> {
            if (m.getTimestamp() == null) return false;
            LocalDate d = m.getTimestamp().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            return (from==null || !d.isBefore(from)) && (to==null || !d.isAfter(to));
        });
    }

    @FXML void handleExit(ActionEvent event) { System.exit(0); }
    
    private boolean isNewRow(Movement m) { return m.getId() == null || m.getId() == 0; }
    
    private void showError(String msg) { 
        Alert a = new Alert(Alert.AlertType.ERROR); 
        a.setTitle("Error");
        a.setContentText(msg); 
        a.showAndWait(); 
    }
}