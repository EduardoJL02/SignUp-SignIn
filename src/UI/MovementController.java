/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package UI;

/**
 *
 * @author pablo
 */

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

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class MovementController implements Initializable {

    // --- ELEMENTOS FXML ---
    @FXML private Label lblCustomerName;
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
    @FXML private TableColumn<Movement, String> colType;
    @FXML private TableColumn<Movement, String> colDesc;
    @FXML private TableColumn<Movement, Double> colAmount;

    // --- DATOS ---
    private ObservableList<Movement> masterData = FXCollections.observableArrayList();
    private FilteredList<Movement> filteredData;
    private User currentUser;

    // Formato de fecha solicitado
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Configurar Selector de Cuentas para que muestre ID y Saldo
        cbAccountSelector.setConverter(new StringConverter<Account>() {
            @Override
            public String toString(Account account) {
                if (account == null) return null;
                return account.getId() + " - " + account.getType() + " (" + account.getBalance() + "€)";
            }
            @Override
            public Account fromString(String string) {
                return cbAccountSelector.getItems().stream()
                        .filter(ap -> ap.getId().equals(string)).findFirst().orElse(null);
            }
        });

        // Listener: Cuando cambia la cuenta, cargar movimientos
        cbAccountSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadMovementsForAccount(newVal);
            }
        });

        // 2. CONFIGURACIÓN DE COLUMNAS (EDICIÓN EN LÍNEA)

        // A) Columna FECHA: Usa un CellFactory personalizado para mostrar un DatePicker
        colDate.setCellValueFactory(cellData -> cellData.getValue().dateProperty());
        colDate.setCellFactory(column -> new DateEditingCell()); 
        colDate.setOnEditCommit(e -> e.getRowValue().setDate(e.getNewValue()));

        // B) Columna TIPO: ChoiceBox/ComboBox con "Deposit" y "Payment"
        colType.setCellValueFactory(cellData -> cellData.getValue().typeProperty());
        colType.setCellFactory(ComboBoxTableCell.forTableColumn("Deposit", "Payment"));
        colType.setOnEditCommit(e -> {
            e.getRowValue().setType(e.getNewValue());
            recalculateBalance(); // Recalcular si el tipo afecta al signo (opcional)
        });

        // C) Columna DESCRIPCIÓN: Texto simple
        colDesc.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
        colDesc.setCellFactory(TextFieldTableCell.forTableColumn());
        colDesc.setOnEditCommit(e -> e.getRowValue().setDescription(e.getNewValue()));

        // D) Columna AMOUNT: Solo números y actualiza saldo
        colAmount.setCellValueFactory(cellData -> cellData.getValue().amountProperty().asObject());
        colAmount.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colAmount.setOnEditCommit(e -> {
            if (e.getNewValue() != null) {
                e.getRowValue().setAmount(e.getNewValue());
                recalculateBalance(); // ACTUALIZA EL BALANCE AL EDITAR
                lblStatus.setText("Amount updated. Balance recalculated.");
            }
        });

        // Inicializar lista filtrada
        filteredData = new FilteredList<>(masterData, p -> true);
        tvMovements.setItems(filteredData);
    }

    /**
     * MÉTODO CLAVE: Llamado desde el Login para pasar el usuario.
     */
    public void setClientData(User user) {
        this.currentUser = user;
        lblCustomerName.setText("CUSTOMER#: " + user.getId() + " " + user.getName());

        // MOCK: Cargar cuentas simuladas (Aquí harías la llamada a la API GET /customers/{id}/accounts)
        ObservableList<Account> mockAccounts = FXCollections.observableArrayList(
            new Account("IBAN-ES99-0001", "STANDARD", 1500.00),
            new Account("IBAN-ES99-0002", "CREDIT", -200.50)
        );
        cbAccountSelector.setItems(mockAccounts);
        
        if (!mockAccounts.isEmpty()) {
            cbAccountSelector.getSelectionModel().selectFirst();
        }
    }

    // --- MÉTODOS DE ACCIÓN FXML ---

    @FXML
    void handleNewRow(ActionEvent event) {
        if (cbAccountSelector.getValue() == null) {
            showAlert("Error", "Please select an account first.");
            return;
        }

        // CREAR MOVIMIENTO: Fecha sistema, Tipo vacío (o default), Monto 0
        Movement newMov = new Movement(
                LocalDate.now(), 
                "Deposit", 
                "New Transaction (Edit me)", 
                0.0
        );

        // Añadir a la lista maestra (al final)
        masterData.add(newMov);
        
        // Seleccionar la nueva fila y hacer scroll hacia ella
        tvMovements.getSelectionModel().select(newMov);
        tvMovements.scrollTo(newMov);
        
        lblStatus.setText("New row added. Double click cells to edit.");
    }

    @FXML
    void handleUndoLastMovement(ActionEvent event) {
        if (masterData.isEmpty()) {
            showAlert("Info", "No movements to undo.");
            return;
        }

        // LÓGICA: Eliminar el ÚLTIMO creado (el último de la lista), no por fecha.
        int lastIndex = masterData.size() - 1;
        Movement removed = masterData.remove(lastIndex);
        
        recalculateBalance();
        lblStatus.setText("Undo successful. Removed movement: " + removed.getDescription());
        
        // NOTA: Aquí llamarías a DELETE /api/movements/{id} si tuviera ID real
    }

    @FXML
    void handleSearchByDates(ActionEvent event) {
        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();

        filteredData.setPredicate(movement -> {
            // Caso 1: Sin filtros -> Mostrar todo
            if (from == null && to == null) return true;

            // Caso 2: Solo Desde
            if (from != null && to == null) return !movement.getDate().isBefore(from);

            // Caso 3: Solo Hasta
            if (from == null && to != null) return !movement.getDate().isAfter(to);

            // Caso 4: Rango
            return !movement.getDate().isBefore(from) && !movement.getDate().isAfter(to);
        });
        
        lblStatus.setText("Filter applied.");
    }

    @FXML
    void handleExit(ActionEvent event) {
        System.exit(0);
    }

    // --- MÉTODOS AUXILIARES ---

    private void loadMovementsForAccount(Account account) {
        // MOCK: Aquí llamarías a GET /api/accounts/{id}/movements
        masterData.clear();
        
        // Simulamos datos iniciales
        masterData.add(new Movement(LocalDate.of(2023, 1, 15), "Deposit", "Initial Deposit", account.getBalance()));
        
        recalculateBalance();
        lblStatus.setText("Loaded movements for account: " + account.getId());
    }

    private void recalculateBalance() {
        double total = 0.0;
        for (Movement m : masterData) {
            // Asumimos lógica simple: Deposit suma, Payment resta
            if ("Payment".equalsIgnoreCase(m.getType())) {
                total -= m.getAmount();
            } else {
                total += m.getAmount();
            }
        }
        tfBalance.setText(String.format("%.2f €", total));
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // --- CLASES INTERNAS (MODELOS) PARA QUE EL CÓDIGO FUNCIONE ---
    // En tu proyecto real, estas clases deben ir en paquetes separados (com.tubanco.model)

    // Clase auxiliar para celda DatePicker personalizada
    class DateEditingCell extends TableCell<Movement, LocalDate> {
        private DatePicker datePicker;

        private void createDatePicker() {
            datePicker = new DatePicker(getItem());
            datePicker.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
            datePicker.setOnAction((e) -> {
                commitEdit(datePicker.getValue());
            });
        }

        @Override
        public void startEdit() {
            super.startEdit();
            if (datePicker == null) createDatePicker();
            datePicker.setValue(getItem());
            setGraphic(datePicker);
            setText(null);
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem().format(dateFormatter));
            setGraphic(null);
        }

        @Override
        public void updateItem(LocalDate item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    if (datePicker != null) datePicker.setValue(item);
                    setGraphic(datePicker);
                    setText(null);
                } else {
                    setText(item.format(dateFormatter));
                    setGraphic(null);
                }
            }
        }
    }

    // Modelo Usuario simple
    public static class User {
        private String id;
        private String name;
        public User(String id, String name) { this.id = id; this.name = name; }
        public String getId() { return id; }
        public String getName() { return name; }
    }

    // Modelo Cuenta simple
    public static class Account {
        private String id;
        private String type;
        private double balance;
        public Account(String id, String type, double balance) {
            this.id = id; this.type = type; this.balance = balance;
        }
        public String getId() { return id; }
        public String getType() { return type; }
        public double getBalance() { return balance; }
        @Override public String toString() { return id; }
    }

    // Modelo Movimiento (con Properties para JavaFX)
    public static class Movement {
        private final ObjectProperty<LocalDate> date;
        private final StringProperty type;
        private final StringProperty description;
        private final DoubleProperty amount;

        public Movement(LocalDate date, String type, String description, double amount) {
            this.date = new SimpleObjectProperty<>(date);
            this.type = new SimpleStringProperty(type);
            this.description = new SimpleStringProperty(description);
            this.amount = new SimpleDoubleProperty(amount);
        }

        public ObjectProperty<LocalDate> dateProperty() { return date; }
        public LocalDate getDate() { return date.get(); }
        public void setDate(LocalDate date) { this.date.set(date); }

        public StringProperty typeProperty() { return type; }
        public String getType() { return type.get(); }
        public void setType(String type) { this.type.set(type); }

        public StringProperty descriptionProperty() { return description; }
        public String getDescription() { return description.get(); }
        public void setDescription(String description) { this.description.set(description); }

        public DoubleProperty amountProperty() { return amount; }
        public double getAmount() { return amount.get(); }
        public void setAmount(double amount) { this.amount.set(amount); }
    }
}