/*
 * Controlador DEFINITIVO v2.0:
 * - Muestra Límite de Crédito.
 * - Corrección visual de Scroll/Edición (layout fix).
 * - Validaciones de Negocio (Standard vs Credit).
 * - Sin filtro de fechas.
 * - Confirmaciones de seguridad.
 */
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
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Controlador principal para la gestión de Movimientos Bancarios en la interfaz gráfica.
 * <p>
 * Esta clase gestiona la interacción entre la vista (FXML) y la lógica de negocio
 * para las cuentas y movimientos. Incluye funcionalidades críticas como:
 * <ul>
 * <li>Visualización de límites de crédito dinámicos.</li>
 * <li>Edición de movimientos en línea dentro de la tabla.</li>
 * <li>Validación de reglas de negocio (saldos negativos y límites de crédito).</li>
 * <li>Manejo de concurrencia en la UI mediante {@link Platform#runLater}.</li>
 * </ul>
 *
 * @author Pablo
 * @version 2.0 Final Integration
 * @since 2.0
 */
public class MovementController implements Initializable {

    /** Logger para el registro de eventos y errores de la clase. */
    private static final Logger LOGGER = Logger.getLogger(MovementController.class.getName());

    // --- ELEMENTOS FXML ---

    /** Etiqueta para mostrar el nombre completo del cliente. */
    @FXML private Label lblCustomerName;
    
    /** Etiqueta para mostrar el ID del usuario actual. */
    @FXML private Label lblUserId;
    
    /** Etiqueta para mostrar el ID de la cuenta seleccionada. */
    @FXML private Label lblAccountId;
    
    /** * Etiqueta para mostrar el límite de crédito disponible.
     * Solo visible/relevante si la cuenta es de tipo CREDIT.
     */
    @FXML private Label lblCreditLimit;
    
    /** Selector desplegable para cambiar entre las cuentas del cliente. */
    @FXML private ComboBox<Account> cbAccountSelector;
    
    /** Campo de texto (no editable directamente por usuario) que muestra el saldo total calculado. */
    @FXML private TextField tfBalance;
    
    /** Etiqueta para feedback de estado (éxito, error, carga). */
    @FXML private Label lblStatus;

    // --- TABLA Y COLUMNAS ---
    
    /** Tabla principal que lista los movimientos de la cuenta. */
    @FXML private TableView<Movement> tvMovements;
    
    /** Columna de fecha, formatea {@link java.util.Date} a {@link java.time.LocalDate}. */
    @FXML private TableColumn<Movement, LocalDate> colDate;
    
    /** Columna de tipo de movimiento (Deposit/Payment), editable mediante ComboBox. */
    @FXML private TableColumn<Movement, String> colType;
    
    /** Columna de cantidad monetaria, editable como texto. */
    @FXML private TableColumn<Movement, Double> colAmount;
    
    /** Columna de saldo resultante tras el movimiento (calculado localmente). */
    @FXML private TableColumn<Movement, Movement> colBalance;

    // --- ESTRUCTURAS DE DATOS ---
    
    /** * Lista observable que contiene los datos mostrados en la tabla.
     * Mantiene la sincronización directa con la UI.
     */
    private ObservableList<Movement> masterData = FXCollections.observableArrayList();
    
    /** Referencia al cliente (Customer) que ha iniciado sesión o fue seleccionado. */
    private Customer currentCustomer;
    
    // --- CLIENTES REST ---
    
    /** Cliente REST para operaciones relacionadas con Cuentas (lectura/actualización). */
    private AccountRESTClient accountClient;
    
    /** Cliente REST para operaciones CRUD de Movimientos. */
    private MovementRESTClient movementClient;

    /** * Bandera para evitar disparar listeners de UI durante actualizaciones 
     * programáticas (ej. carga inicial de datos). 
     */
    private boolean isProgrammaticUpdate = false;
    
    /** Formateador de fecha estándar para la visualización en celdas. */
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Inicializa el controlador, configurando conversores, listeners y fábricas de celdas.
     * Se ejecuta automáticamente tras la carga del FXML.
     *
     * @param location  La ubicación utilizada para resolver rutas relativas.
     * @param resources Los recursos utilizados para localizar el objeto raíz.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        accountClient = new AccountRESTClient();
        movementClient = new MovementRESTClient();

        // 1. CONFIGURAR COMBOBOX (Visualización personalizada de Cuenta)
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
        // Detecta cambios en el combo para cargar movimientos y actualizar UI de crédito
        cbAccountSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !isProgrammaticUpdate) {
                lblAccountId.setText(String.valueOf(newVal.getId()));
                
                // --- LÓGICA DE VISUALIZACIÓN DE CRÉDITO ---
                if (lblCreditLimit != null) {
                    String typeStr = (newVal.getType() != null) ? newVal.getType().toString().toUpperCase() : "";
                    if (typeStr.contains("CREDIT")) {
                        double limit = (newVal.getCreditLine() != null) ? newVal.getCreditLine() : 0.0;
                        lblCreditLimit.setText(String.format("%.2f €", limit));
                    } else {
                        lblCreditLimit.setText("N/A"); // No aplica para Standard
                    }
                }
                // ------------------------------------------

                loadMovementsForAccount(newVal);
            }
        });

        // 3. CONFIGURAR COLUMNAS
        setupColumns();

        // 4. VINCULACIÓN DE DATOS (Directa)
        tvMovements.setItems(masterData);

        // 5. CONFIGURAR TECLA ESC (Salir)
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
    }

    /**
     * Configura las fábricas de celdas (CellFactory) y valores (CellValueFactory)
     * para las columnas de la tabla, incluyendo la lógica de edición.
     */
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

        // --- B) TIPO (ComboBox editable) ---
        colType.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));
        colType.setCellFactory(ComboBoxTableCell.forTableColumn("Deposit", "Payment"));
        colType.setOnEditCommit(e -> {
            // Solo permite editar si es una fila nueva no guardada
            if (isNewRow(e.getRowValue())) {
                e.getRowValue().setDescription(e.getNewValue());
                // Salta automáticamente a la siguiente columna lógica (Amount)
                tvMovements.getSelectionModel().select(e.getTablePosition().getRow(), colAmount);
            } else {
                tvMovements.refresh();
            }
        });

        // --- C) CANTIDAD (TextField con validación Numérica) ---
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
                    // Normaliza separadores decimales
                    return Double.parseDouble(string.replace(",", "."));
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            }
        }));
        
        // Commit de la edición: Dispara la creación en el servidor si es nueva fila
        colAmount.setOnEditCommit(e -> {
            Movement m = e.getRowValue();
            if (isNewRow(m)) {
                Double val = e.getNewValue();
                // Regla de negocio: Si es Payment, el valor debe ser negativo
                if ("Payment".equalsIgnoreCase(m.getDescription()) && val > 0) val *= -1;
                m.setAmount(val);
                createMovementOnServer(m);
            } else {
                tvMovements.refresh();
            }
        });

        // --- D) BALANCE (Calculado) ---
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
     * Establece el cliente actual y carga sus cuentas asociadas.
     * Este es el punto de entrada principal tras el login o navegación.
     *
     * @param customer Objeto cliente con ID y datos personales.
     */
    public void setClientData(Customer customer) {
        this.currentCustomer = customer;
        lblCustomerName.setText((customer.getFirstName()!=null ? customer.getFirstName() : "Cliente") + " " + 
                                (customer.getLastName()!=null ? customer.getLastName() : ""));
        lblUserId.setText(String.valueOf(customer.getId()));
        loadUserAccounts(true);
    }

    /**
     * Recupera las cuentas del servidor REST.
     *
     * @param selectFirst Si es true, selecciona automáticamente la primera cuenta encontrada.
     */
    private void loadUserAccounts(boolean selectFirst) {
        try {
            Account[] accounts = accountClient.findAccountsByCustomerId_XML(Account[].class, String.valueOf(currentCustomer.getId()));
            
            isProgrammaticUpdate = true;
            Account current = cbAccountSelector.getValue();
            cbAccountSelector.setItems(FXCollections.observableArrayList(accounts));
            
            // Intenta mantener la selección actual o seleccionar la primera
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

    /**
     * Carga los movimientos de una cuenta específica y recalcula saldos locales.
     *
     * @param account La cuenta de la cual obtener los movimientos.
     */
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

    /**
     * Ordena los movimientos cronológicamente y calcula el saldo acumulado (running balance)
     * para cada fila, basándose en el saldo inicial de la cuenta.
     */
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

    /**
     * Maneja la creación de una nueva fila en la tabla para ingresar un movimiento.
     * <p>
     * Utiliza un doble {@link Platform#runLater} para asegurar que el scroll
     * y el renderizado de la tabla hayan finalizado antes de activar el modo edición,
     * solucionando un bug visual donde el ComboBox aparecía en coordenadas incorrectas.
     *
     * @param event Evento del botón (no utilizado directamente).
     */
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

        // 1. Añadimos el dato a la lista maestra
        masterData.add(newMov);
        
        // 2. Primer runLater: Esperar a que la tabla "sepa" que tiene un dato nuevo
        Platform.runLater(() -> {
            // Buscamos el índice visual (útil si hubiera ordenación, aunque sea nueva)
            int targetIndex = tvMovements.getItems().indexOf(newMov);
            
            if (targetIndex >= 0) {
                tvMovements.getSelectionModel().clearSelection();
                tvMovements.getSelectionModel().select(targetIndex);
                tvMovements.requestFocus(); 
                tvMovements.scrollTo(targetIndex);
                
                // 3. Segundo runLater (ANIDADO): Esperamos a que el SCROLL termine
                final int rowToEdit = targetIndex;
                Platform.runLater(() -> {
                    // --- ARREGLO VISUAL (Layout Fix) ---
                    // Forzamos recalcular la posición de las celdas para que el combo
                    // no aparezca en la fila incorrecta.
                    tvMovements.layout(); 
                    
                    tvMovements.edit(rowToEdit, colType);
                });
            }
        });
    }

    /**
     * Envía el nuevo movimiento al servidor tras validar reglas de negocio.
     * <p>
     * <b>Reglas de Validación:</b>
     * <ul>
     * <li><b>Standard:</b> El saldo no puede ser negativo (fondos insuficientes).</li>
     * <li><b>Credit:</b> El saldo negativo no puede exceder el límite de crédito configurado.</li>
     * </ul>
     *
     * @param mov El objeto movimiento con los datos introducidos por el usuario.
     */
    private void createMovementOnServer(Movement mov) {
        try {
            Account acc = cbAccountSelector.getValue();
            double currentBal = 0.0;
            
            // Calcular saldo proyectado
            try {
                String txt = tfBalance.getText().replace(" €","").replace(",",".");
                currentBal = Double.parseDouble(txt) + mov.getAmount();
            } catch (Exception e) {
                double start = (acc.getBeginBalance() != null) ? acc.getBeginBalance() : 0.0;
                currentBal = start + mov.getAmount();
            }
            
            // --- VALIDACIÓN DE REGLAS DE NEGOCIO ---
            if (mov.getAmount() < 0) {
                String typeStr = (acc.getType() != null) ? acc.getType().toString().toUpperCase() : "";

                // CASO STANDARD: No bajar de 0
                if (typeStr.contains("STANDARD")) { 
                    if (currentBal < 0) {
                        showError("Fondos insuficientes (Cuenta Standard).");
                        masterData.remove(mov);
                        tvMovements.refresh();
                        return;
                    }
                } 
                // CASO CREDIT: No bajar del límite negativo
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

            // Preparar y enviar
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

    /**
     * Elimina el último movimiento realizado (LIFO) previa confirmación del usuario.
     * Actualiza el saldo de la cuenta en el servidor.
     *
     * @param event Evento del botón.
     */
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
            // Revertir el saldo de la cuenta
            acc.setBalance(acc.getBalance() - last.getAmount());
            accountClient.updateAccount_XML(acc);
            
            reloadEverything();
            lblStatus.setText("Deshecho.");
        } catch (Exception e) {
            lblStatus.setText("Error al deshacer.");
        }
    }

    /**
     * Cierra la aplicación de forma segura.
     *
     * @param event Evento del botón o tecla ESC.
     */
    @FXML 
    void handleExit(ActionEvent event) { 
        if (showConfirmation("¿Seguro que quieres salir de la aplicación?")) {
            System.exit(0); 
        }
    }

    // --- UTILIDADES ---

    /**
     * Recarga cuentas y movimientos para asegurar consistencia tras una operación de escritura.
     */
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
    
    /**
     * Verifica si un movimiento es nuevo (no persistido en BD).
     * @param m Movimiento a verificar.
     * @return true si el ID es nulo o 0.
     */
    private boolean isNewRow(Movement m) { return m.getId() == null || m.getId() == 0; }
    
    /**
     * Muestra una alerta modal de error.
     * @param msg Mensaje a mostrar.
     */
    private void showError(String msg) { 
        Alert a = new Alert(Alert.AlertType.ERROR); 
        a.setTitle("Error");
        a.setContentText(msg); 
        a.showAndWait(); 
    }

    /**
     * Muestra una alerta modal de confirmación (Aceptar/Cancelar).
     * @param msg Pregunta a confirmar.
     * @return true si el usuario pulsa OK.
     */
    private boolean showConfirmation(String msg) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmación");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}