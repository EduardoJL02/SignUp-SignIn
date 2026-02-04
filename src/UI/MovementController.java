/*
 * Controlador DEFINITIVO v2.1:
 * - Botón "Volver" añadido.
 */
package UI;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader; // IMPORT AÑADIDO
import javafx.fxml.Initializable;
import javafx.scene.Node; // IMPORT AÑADIDO
import javafx.scene.Parent; // IMPORT AÑADIDO
import javafx.scene.Scene; // IMPORT AÑADIDO
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage; // IMPORT AÑADIDO
import javafx.util.StringConverter;
import logic.AccountRESTClient;
import logic.MovementRESTClient;
import model.Account;
import model.Customer;
import model.Movement;

import java.io.IOException; // IMPORT AÑADIDO
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import javax.ws.rs.core.GenericType;

/**
 * Controlador principal para la gestión de Movimientos Bancarios.
 */
public class MovementController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(MovementController.class.getName());

    // --- ELEMENTOS FXML ---
    @FXML private Label lblCustomerName;
    @FXML private Label lblUserId;
    @FXML private Label lblAccountId;
    @FXML private Label lblCreditLimit;
    @FXML private ComboBox<Account> cbAccountSelector;
    @FXML private TextField tfBalance;
    @FXML private Label lblStatus;
    @FXML private Button btBack; // Declaración del nuevo botón (opcional pero recomendada)

    // --- TABLA Y COLUMNAS ---
    @FXML private TableView<Movement> tvMovements;
    @FXML private TableColumn<Movement, LocalDate> colDate;
    @FXML private TableColumn<Movement, String> colType;
    @FXML private TableColumn<Movement, Double> colAmount;
    @FXML private TableColumn<Movement, Movement> colBalance;

    // --- ESTRUCTURAS DE DATOS ---
    private ObservableList<Movement> masterData = FXCollections.observableArrayList();
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

        // 2. LISTENER DE SELECCIÓN DE CUENTA
        cbAccountSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !isProgrammaticUpdate) {
                lblAccountId.setText(String.valueOf(newVal.getId()));
                
                if (lblCreditLimit != null) {
                    String typeStr = (newVal.getType() != null) ? newVal.getType().toString().toUpperCase() : "";
                    if (typeStr.contains("CREDIT")) {
                        double limit = (newVal.getCreditLine() != null) ? newVal.getCreditLine() : 0.0;
                        lblCreditLimit.setText(String.format("%.2f €", limit));
                    } else {
                        lblCreditLimit.setText("N/A");
                    }
                }
                loadMovementsForAccount(newVal);
            }
        });

        // 3. CONFIGURAR COLUMNAS
        setupColumns();

        // 4. VINCULACIÓN DE DATOS
        tvMovements.setItems(masterData);

        // 5. CONFIGURAR TECLA ESC
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
        
        Platform.runLater(() -> {
        if (lblUserId.getScene() != null) {
            Stage stage = (Stage) lblUserId.getScene().getWindow();
            stage.setTitle("Movements"); // <--- IMPORTANTE
        }
    });
    }

    private void setupColumns() {
        // A) FECHA
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

        // B) TIPO
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

        // C) CANTIDAD
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

        // D) BALANCE
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

//    private void loadUserAccounts(boolean selectFirst) {
//        try {
//            Account[] accounts = accountClient.findAccountsByCustomerId_XML(Account[].class, String.valueOf(currentCustomer.getId()));
//            
//            isProgrammaticUpdate = true;
//            Account current = cbAccountSelector.getValue();
//            cbAccountSelector.setItems(FXCollections.observableArrayList(accounts));
//            
//            if (current != null) {
//                for(Account a : accounts) {
//                    if(a.getId().equals(current.getId())) {
//                        cbAccountSelector.setValue(a);
//                        break;
//                    }
//                }
//            } else if (selectFirst && accounts.length > 0) {
//                cbAccountSelector.getSelectionModel().selectFirst();
//                loadMovementsForAccount(cbAccountSelector.getValue());
//            }
//            isProgrammaticUpdate = false;
//        } catch (Exception e) {
//            lblStatus.setText("Error cargando cuentas.");
//        }
//    }
    
    private void loadUserAccounts(boolean selectFirst) {
        try {
            // 1. Definimos el GenericType para recibir una lista
            GenericType<List<Account>> listType = new GenericType<List<Account>>() {};

            // 2. Llamamos al servicio (ahora devuelve List<Account>, no array)
            List<Account> accounts = accountClient.findAccountsByCustomerId_XML(listType, String.valueOf(currentCustomer.getId()));

            isProgrammaticUpdate = true;
            Account current = cbAccountSelector.getValue();
            
            // 3. Pasamos la lista directamente al ObservableList
            cbAccountSelector.setItems(FXCollections.observableArrayList(accounts));

            // 4. Lógica de recuperación de selección
            if (current != null) {
                // El bucle for-each funciona igual para Listas que para Arrays
                for (Account a : accounts) {
                    if (a.getId().equals(current.getId())) {
                        cbAccountSelector.setValue(a);
                        break;
                    }
                }
            } 
            // CAMBIO AQUÍ: Las listas no tienen .length, usamos !isEmpty() o .size() > 0
            else if (selectFirst && !accounts.isEmpty()) {
                cbAccountSelector.getSelectionModel().selectFirst();
                // Aseguramos que se carguen los movimientos de la cuenta seleccionada por defecto
                loadMovementsForAccount(cbAccountSelector.getValue());
            }
            
            isProgrammaticUpdate = false;

        } catch (Exception e) {
            // Es buena práctica mostrar el error exacto en el log o label
            lblStatus.setText("Error cargando cuentas: " + e.getMessage());
            // Opcional: Mostrar Alert si prefieres ser más intrusivo con el error
            // showErrorAlert("No se pudieron cargar las cuentas.");
        }
    }

//    private void loadMovementsForAccount(Account account) {
//        try {
//            Movement[] movements = movementClient.findMovementByAccount_XML(Movement[].class, String.valueOf(account.getId()));
//            masterData.clear();
//            masterData.addAll(Arrays.asList(movements));
//            recalculateLocalBalances();
//            lblStatus.setText("Datos cargados correctamente.");
//        } catch (Exception e) {
//            lblStatus.setText("Error de conexión.");
//            e.printStackTrace();
//        }
//    }

    private void loadMovementsForAccount(Account account) {
        // Validación básica: si no hay cuenta seleccionada, no hacemos nada
        if (account == null) {
            return;
        }

        try {
            // 1. Definimos el tipo genérico: Lista de Movimientos
            GenericType<List<Movement>> listType = new GenericType<List<Movement>>() {};

            // 2. Llamamos al servidor usando el nuevo método del cliente
            List<Movement> movementsList = movementClient.findMovementByAccount_XML(listType, String.valueOf(account.getId()));

            // 3. Actualizamos la lista observable (masterData)
            masterData.clear();
            
            // Ahora podemos añadir la lista directamente, sin usar Arrays.asList()
            masterData.addAll(movementsList);

            // 4. Recalculamos saldos y actualizamos la UI
            recalculateLocalBalances();
            lblStatus.setText("Movimientos cargados: " + movementsList.size());

        } catch (Exception e) {
            // Manejo de errores simple y visual
            lblStatus.setText("Error al cargar movimientos.");
            
            // Opcional: Imprimir traza para depuración en consola
            e.printStackTrace(); 
            
            // Si prefieres ser más explícito con el usuario, descomenta la alerta:
            // showErrorAlert("No se pudieron descargar los movimientos del servidor.");
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

        masterData.add(newMov);
        
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
            
            if (mov.getAmount() < 0) {
                String typeStr = (acc.getType() != null) ? acc.getType().toString().toUpperCase() : "";

                if (typeStr.contains("STANDARD")) { 
                    if (currentBal < 0) {
                        showError("Fondos insuficientes (Cuenta Standard).");
                        masterData.remove(mov);
                        tvMovements.refresh();
                        return;
                    }
                } 
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
        
        if (!showConfirmation("¿Estás seguro de que deseas eliminar el último movimiento?")) {
            return;
        }

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

    /**
     * Navega de vuelta a la pantalla de Cuentas.
     * @param event Evento del botón.
     */
    @FXML
    void handleBack(ActionEvent event) {
        try {
            // 1. Cargar el FXML de la ventana de cuentas
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXMLAccounts.fxml"));
            Parent root = loader.load();

            // 2. Obtener el controlador de esa vista (AccountController)
            // IMPORTANTE: Asegúrate de que 'AccountController' es el nombre exacto de tu clase controladora de cuentas
            AccountsController controller = loader.getController();

            // 3. Pasarle el cliente actual para restaurar el estado
            // Debes tener un método setCustomer(Customer c) o similar en AccountController
            if (this.currentCustomer != null) {
                controller.setCustomer(this.currentCustomer);
            }

            // 4. Cambiar la escena
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            LOGGER.severe("Error al volver a la pantalla de cuentas: " + e.getMessage());
            e.printStackTrace();
            showError("No se pudo cargar la pantalla de cuentas.");
        }
    }

    @FXML 
    void handleExit(ActionEvent event) { 
        if (showConfirmation("¿Seguro que quieres salir de la aplicación?")) {
            System.exit(0); 
        }
    }

    // --- UTILIDADES ---

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
    
    private boolean isNewRow(Movement m) { return m.getId() == null || m.getId() == 0; }
    
    /**
     * Método para recibir una cuenta desde la ventana anterior.
     * Busca la cuenta en el ComboBox y la selecciona si existe.
     * @param account La cuenta seleccionada en la ventana anterior.
     */
    public void setPreselectedAccount(Account account) {
        if (account == null) {
            return;
        }

        // Recorremos los items del ComboBox para encontrar la coincidencia por ID
        ObservableList<Account> items = cbAccountSelector.getItems();
        
        for (Account a : items) {
            // Comparamos los IDs porque los objetos podrían ser instancias diferentes en memoria
            if (a.getId().equals(account.getId())) {
                // Seleccionamos la cuenta en el combo. 
                // Esto debería disparar el Listener del ComboBox y cargar los movimientos automáticamente.
                cbAccountSelector.getSelectionModel().select(a);
                break; // Salimos del bucle una vez encontrada
            }
        }
    }
    
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