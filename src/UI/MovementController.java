/*
 * Controlador DEFINITIVO con Saldo Inicial (Begin Balance) y Documentación Completa.
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
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Controlador principal para la gestión de Movimientos Bancarios.
 * <p>
 * Esta clase maneja la interfaz de usuario (JavaFX) para visualizar, crear y eliminar movimientos.
 * Se comunica con el servidor a través de clientes REST y realiza cálculos locales para
 * mantener la coherencia visual del saldo (Balance) basándose en el saldo inicial de la cuenta.
 * </p>
 * * @author Pablo
 * @version 1.0 Final
 */
public class MovementController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(MovementController.class.getName());

    // --- ELEMENTOS FXML (Interfaz Gráfica) ---
    @FXML private Label lblCustomerName;     // Nombre del cliente en la cabecera
    @FXML private Label lblUserId;           // ID del cliente
    @FXML private Label lblAccountId;        // ID de la cuenta seleccionada
    @FXML private ComboBox<Account> cbAccountSelector; // Desplegable de cuentas
    @FXML private DatePicker dpFrom;         // Filtro fecha inicio
    @FXML private DatePicker dpTo;           // Filtro fecha fin
    @FXML private TextField tfBalance;       // Campo de texto con el saldo total final
    @FXML private Label lblStatus;           // Barra de estado inferior

    // --- TABLA Y COLUMNAS ---
    @FXML private TableView<Movement> tvMovements;
    @FXML private TableColumn<Movement, LocalDate> colDate;
    @FXML private TableColumn<Movement, String> colType;
    @FXML private TableColumn<Movement, Double> colAmount;
    @FXML private TableColumn<Movement, Movement> colBalance; // Columna especial para el saldo calculado

    // --- ESTRUCTURAS DE DATOS ---
    /** Lista observable que contiene todos los movimientos cargados. */
    private ObservableList<Movement> masterData = FXCollections.observableArrayList();
    /** Lista filtrada para aplicar búsquedas por fecha sin perder datos originales. */
    private FilteredList<Movement> filteredData;
    /** Cliente logueado actualmente. */
    private Customer currentCustomer;
    
    // --- CLIENTES REST (Comunicación con Servidor) ---
    private AccountRESTClient accountClient;
    private MovementRESTClient movementClient;

    /** Flag para evitar bucles infinitos en los listeners al actualizar datos programáticamente. */
    private boolean isProgrammaticUpdate = false;
    
    /** Formato de fecha para visualización en tabla (dd/MM/yyyy). */
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Método de inicialización de JavaFX.
     * Configura los convertidores del ComboBox, los listeners de selección y las columnas de la tabla.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Inicializar clientes REST
        accountClient = new AccountRESTClient();
        movementClient = new MovementRESTClient();

        // 1. CONFIGURAR COMBOBOX (Formato visual: "ID - TIPO")
        // Definimos un StringConverter para controlar qué texto se muestra en el desplegable.
        StringConverter<Account> converter = new StringConverter<Account>() {
            @Override
            public String toString(Account account) {
                if (account == null) return null;
                // Muestra solo ID y Tipo. El saldo se oculta aquí intencionalmente.
                return account.getId() + " - " + account.getType();
            }
            @Override
            public Account fromString(String string) { return null; }
        };
        
        cbAccountSelector.setConverter(converter);
        
        // Configuramos la "CellFactory" para aplicar el formato a la lista desplegable
        cbAccountSelector.setCellFactory(lv -> new ListCell<Account>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : converter.toString(item));
            }
        });
        // Configuramos el "ButtonCell" para aplicar el formato al elemento seleccionado (cerrado)
        cbAccountSelector.setButtonCell(new ListCell<Account>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : converter.toString(item));
            }
        });

        // 2. LISTENER DE SELECCIÓN DE CUENTA
        // Detecta cambios en el ComboBox para cargar los movimientos correspondientes.
        cbAccountSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            // Solo cargamos si hay valor y NO es una actualización interna del código (isProgrammaticUpdate)
            if (newVal != null && !isProgrammaticUpdate) {
                lblAccountId.setText(String.valueOf(newVal.getId()));
                loadMovementsForAccount(newVal);
            }
        });

        // 3. CONFIGURAR COLUMNAS DE LA TABLA
        setupColumns();

        // 4. INICIALIZAR FILTROS
        filteredData = new FilteredList<>(masterData, p -> true);
        tvMovements.setItems(filteredData);
    }

    /**
     * Configura el comportamiento de las columnas de la tabla:
     * formatos de fecha, edición de celdas y cálculo visual.
     */
    private void setupColumns() {
        // --- A) COLUMNA FECHA ---
        colDate.setCellValueFactory(cellData -> {
            if(cellData.getValue().getTimestamp() != null){
                // Convertir Date (java.util) a LocalDate (java.time)
                return new SimpleObjectProperty<>(cellData.getValue().getTimestamp().toInstant()
                                  .atZone(ZoneId.systemDefault()).toLocalDate());
            }
            return null;
        });
        // Formateador visual dd/MM/yyyy
        colDate.setCellFactory(column -> new TableCell<Movement, LocalDate>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : dateFormatter.format(item));
            }
        });

        // --- B) COLUMNA TIPO (Editable) ---
        // Usa la propiedad 'description' del modelo para guardar el tipo.
        colType.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));
        // Crea un ComboBox dentro de la celda con opciones "Deposit" y "Payment"
        colType.setCellFactory(ComboBoxTableCell.forTableColumn("Deposit", "Payment"));
        colType.setOnEditCommit(e -> {
            // Solo permite editar si es una fila nueva
            if (isNewRow(e.getRowValue())) {
                e.getRowValue().setDescription(e.getNewValue());
                // UX: Saltar a la siguiente columna
                tvMovements.getSelectionModel().select(e.getTablePosition().getRow(), colAmount);
            } else {
                tvMovements.refresh(); // Revertir cambios si no es nueva
            }
        });

        // --- C) COLUMNA CANTIDAD (Editable) ---
        colAmount.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getAmount()));
        colAmount.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colAmount.setOnEditCommit(e -> {
            Movement m = e.getRowValue();
            if (isNewRow(m)) {
                Double val = e.getNewValue();
                // Lógica de negocio: Si es "Payment", convertir a negativo automáticamente
                if ("Payment".equalsIgnoreCase(m.getDescription()) && val > 0) val *= -1;
                m.setAmount(val);
                // Trigger: Al confirmar la cantidad, se guarda en el servidor
                createMovementOnServer(m);
            } else {
                tvMovements.refresh();
            }
        });

        // --- D) COLUMNA BALANCE (Lectura) ---
        // Recibe el objeto Movement completo para leer siempre el valor 'balance' más actualizado
        colBalance.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        colBalance.setCellFactory(column -> new TableCell<Movement, Movement>() {
            @Override
            protected void updateItem(Movement item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // Muestra el saldo con formato de moneda
                    setText(String.format("%.2f €", item.getBalance()));
                }
            }
        });
    }

    // --- LÓGICA DE CARGA DE DATOS ---

    /**
     * Método de entrada llamado desde la ventana anterior (Login).
     * @param customer El cliente que ha iniciado sesión.
     */
    public void setClientData(Customer customer) {
        this.currentCustomer = customer;
        // Mostrar nombre y ID en la interfaz
        lblCustomerName.setText((customer.getFirstName()!=null ? customer.getFirstName() : "Cliente") + " " + 
                                (customer.getLastName()!=null ? customer.getLastName() : ""));
        lblUserId.setText(String.valueOf(customer.getId()));
        
        // Cargar cuentas del usuario
        loadUserAccounts(true);
    }

    /**
     * Carga las cuentas asociadas al cliente desde el servidor.
     * @param selectFirst Si es true, selecciona automáticamente la primera cuenta de la lista.
     */
    private void loadUserAccounts(boolean selectFirst) {
        try {
            // Llamada REST para obtener Array de cuentas
            Account[] accounts = accountClient.findAccountsByCustomerId_XML(Account[].class, String.valueOf(currentCustomer.getId()));
            
            // Pausar listener para evitar recargas innecesarias
            isProgrammaticUpdate = true;
            
            Account current = cbAccountSelector.getValue();
            cbAccountSelector.setItems(FXCollections.observableArrayList(accounts));
            
            // Intentar restaurar la selección anterior si existe
            if (current != null) {
                for(Account a : accounts) {
                    if(a.getId().equals(current.getId())) {
                        cbAccountSelector.setValue(a);
                        break;
                    }
                }
            } else if (selectFirst && accounts.length > 0) {
                // Si no había selección, seleccionar la primera
                cbAccountSelector.getSelectionModel().selectFirst();
                loadMovementsForAccount(cbAccountSelector.getValue());
            }
            
            isProgrammaticUpdate = false; // Reactivar listener
        } catch (Exception e) {
            lblStatus.setText("Error cargando cuentas.");
        }
    }

    /**
     * Carga los movimientos de una cuenta específica y recalcula los saldos.
     * @param account La cuenta seleccionada.
     */
    private void loadMovementsForAccount(Account account) {
        try {
            // Llamada REST para obtener Array de movimientos
            Movement[] movements = movementClient.findMovementByAccount_XML(Movement[].class, String.valueOf(account.getId()));
            
            masterData.clear();
            masterData.addAll(Arrays.asList(movements));
            
            // IMPORTANTE: Recalcular saldos visualmente usando BeginBalance
            recalculateLocalBalances();
            
            lblStatus.setText("Datos cargados correctamente.");
        } catch (Exception e) {
            lblStatus.setText("Error de conexión.");
            e.printStackTrace();
        }
    }

    /**
     * RECALCULA EL SALDO ACUMULADO FILA A FILA.
     * <p>
     * 1. Ordena los movimientos por fecha.
     * 2. Obtiene el 'beginBalance' (Saldo Inicial) de la cuenta.
     * 3. Suma secuencialmente cada movimiento.
     * 4. Actualiza la UI (Campo de texto total y Tabla).
     * </p>
     */
    private void recalculateLocalBalances() {
        // 1. Ordenar por fecha
        masterData.sort((m1, m2) -> {
            if (m1.getTimestamp() == null || m2.getTimestamp() == null) return 0;
            return m1.getTimestamp().compareTo(m2.getTimestamp());
        });

        // 2. Obtener Saldo Inicial (Begin Balance) desde la cuenta seleccionada
        double runningBalance = 0.0;
        Account selectedAccount = cbAccountSelector.getValue();
        
        if (selectedAccount != null && selectedAccount.getBeginBalance() != null) {
            runningBalance = selectedAccount.getBeginBalance();
        }

        // 3. Calcular acumulado sumando movimientos al saldo inicial
        for (Movement m : masterData) {
            runningBalance += m.getAmount();
            m.setBalance(runningBalance); // Actualizar el objeto visual
        }

        final double finalBalance = runningBalance;

        // 4. Actualizar UI en el hilo de JavaFX (Platform.runLater)
        // Esto asegura que el valor se pinte AL FINAL, sobreescribiendo datos desactualizados.
        Platform.runLater(() -> {
            tfBalance.setText(String.format("%.2f €", finalBalance));
            tvMovements.refresh(); // Obliga a la tabla a repintar la columna de Balance
        });
    }

    // --- OPERACIONES CRUD (Create, Read, Update, Delete) ---

    /**
     * Prepara una nueva fila vacía en la tabla para que el usuario la edite.
     */
    @FXML
    void handleNewRow(ActionEvent event) {
        if (cbAccountSelector.getValue() == null) return;

        Movement newMov = new Movement();
        newMov.setTimestamp(new Date()); // Fecha de hoy
        newMov.setDescription("Deposit");
        newMov.setAmount(0.0);
        
        // Establecer un saldo temporal visual (el actual) para que no salga 0
        try {
            String txt = tfBalance.getText().replace(" €","").replace(",",".");
            newMov.setBalance(Double.parseDouble(txt));
        } catch(Exception e) { newMov.setBalance(0.0); }

        masterData.add(newMov);
        
        // Enfocar la nueva fila
        int idx = masterData.size() - 1;
        tvMovements.getSelectionModel().select(idx);
        tvMovements.scrollTo(newMov);
        tvMovements.edit(idx, colType); // Poner foco en la edición de Tipo
    }

    /**
     * Envía el nuevo movimiento al servidor.
     * @param mov El movimiento a persistir.
     */
    private void createMovementOnServer(Movement mov) {
        try {
            Account acc = cbAccountSelector.getValue();
            
            // Calculamos el saldo final teórico para enviar al servidor
            double currentBal = 0.0;
            try {
                // Intentamos usar el saldo visual actual + importe
                String txt = tfBalance.getText().replace(" €","").replace(",",".");
                currentBal = Double.parseDouble(txt) + mov.getAmount();
            } catch (Exception e) {
                // Fallback: Saldo inicial + importe
                double start = (acc.getBeginBalance() != null) ? acc.getBeginBalance() : 0.0;
                currentBal = start + mov.getAmount();
            }
            
            mov.setBalance(currentBal); 
            mov.setAccount(acc);

            // POST REST
            movementClient.create_XML(mov, String.valueOf(acc.getId()));
            
            // Recargar todo para confirmar IDs y recalcular
            reloadEverything(); 
            lblStatus.setText("Guardado.");
        } catch (Exception e) {
            masterData.remove(mov); // Si falla, quitamos la fila visual
            lblStatus.setText("Error al guardar.");
            e.printStackTrace();
        }
    }

    /**
     * Elimina el último movimiento de la lista (Undo).
     */
    @FXML
    void handleUndoLastMovement(ActionEvent event) {
        if (masterData.isEmpty()) return;
        
        // Obtener el último movimiento cronológico
        Movement last = masterData.get(masterData.size()-1);
        try {
            // 1. Borrar movimiento (DELETE)
            movementClient.remove(String.valueOf(last.getId()));
            
            // 2. Actualizar cuenta (PUT) - Actualizamos el saldo total en la entidad Account
            Account acc = cbAccountSelector.getValue();
            acc.setBalance(acc.getBalance() - last.getAmount());
            accountClient.updateAccount_XML(acc);
            
            // 3. Recargar
            reloadEverything();
            lblStatus.setText("Deshecho.");
        } catch (Exception e) {
            lblStatus.setText("Error al deshacer.");
        }
    }

    /**
     * Helper para recargar cuentas y movimientos tras una modificación.
     * Mantiene la selección de la cuenta actual.
     */
    private void reloadEverything() {
        Account current = cbAccountSelector.getValue();
        if(current == null) return;
        
        // Recargar lista de cuentas (para obtener saldo actualizado)
        loadUserAccounts(false);
        
        // Buscar la cuenta seleccionada en la nueva lista
        for(Account a : cbAccountSelector.getItems()) {
            if(a.getId().equals(current.getId())) {
                isProgrammaticUpdate = true;
                cbAccountSelector.setValue(a);
                isProgrammaticUpdate = false;
                
                // Recargar movimientos
                loadMovementsForAccount(a);
                break;
            }
        }
    }

    // --- UTILIDADES ---

    /**
     * Filtra la tabla localmente por rango de fechas.
     */
    @FXML
    void handleSearchByDates(ActionEvent event) {
        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();
        filteredData.setPredicate(m -> {
            if (m.getTimestamp() == null) return false;
            LocalDate d = m.getTimestamp().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            // Lógica: (Fecha >= From) AND (Fecha <= To)
            return (from==null || !d.isBefore(from)) && (to==null || !d.isAfter(to));
        });
    }

    @FXML void handleExit(ActionEvent event) { System.exit(0); }
    
    /** Verifica si una fila es nueva (ID nulo o 0). */
    private boolean isNewRow(Movement m) { return m.getId() == null || m.getId() == 0; }
    
    /** Muestra un mensaje de error. */
    private void showError(String msg) { 
        Alert a = new Alert(Alert.AlertType.ERROR); 
        a.setContentText(msg); a.showAndWait(); 
    }
}