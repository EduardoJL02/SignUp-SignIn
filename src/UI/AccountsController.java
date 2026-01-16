package UI;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javax.ws.rs.core.GenericType;
import logic.AccountRESTClient;
import model.Account;
import model.AccountType;
import model.Customer;

//Controlador de Gestión de Cuentas.

public class AccountsController {

    // Logger
    private static final Logger LOGGER = Logger.getLogger("UI.AccountsController");

    // Elementos FXML
    @FXML
    private TableView<Account> tbAccounts;
    @FXML
    private TableColumn<Account, Long> tcId;
    @FXML
    private TableColumn<Account, String> tcDescription;
    @FXML
    private TableColumn<Account, AccountType> tcType;
    @FXML
    private TableColumn<Account, Double> tcBalance;
    @FXML
    private TableColumn<Account, Double> tcCreditLine;
    @FXML
    private TableColumn<Account, Double> tcBeginBalance;
    @FXML
    private TableColumn<Account, java.util.Date> tcDate;

    @FXML
    private TextField tfDescription;
    @FXML
    private ChoiceBox<AccountType> cbType;
    @FXML
    private TextField tfCreditLine;
    @FXML
    private TextField tfBalance;
    @FXML
    private TextField tfDate;

    @FXML
    private Button btnCreate;
    @FXML
    private Button btnModify;
    @FXML
    private Button btnDelete;
    @FXML
    private Button btnReport;
    @FXML
    private Button btnSearch;
    @FXML
    private Button btnHelp;
    
    @FXML
    private Label lblMessage;

    // Atributos Locales
    private Stage stage;
    private Customer user; // usuario logueado
    private AccountRESTClient client;
    private ObservableList<Account> accountsData;

    /**
     * Método principal para configurar y mostrar la ventana.
     * Recibe el 'root' cargado desde el FXML y el 'customer' que hizo login.
     * @param root El nodo raíz de la vista (cargado por el FXMLLoader).
     * @param customer El usuario que ha iniciado sesión.
     */
    public void setStage(Parent root, Customer customer) {
        try {
            LOGGER.info("Inicializando AccountsController...");

            // Guardar usuario recibido
            this.user = customer;

            // Configurar Escena y Stage
            Scene scene = new Scene(root);
            stage = new Stage();
            stage.setScene(scene);
            
            // Propiedades de la ventana
            stage.setTitle("Accounts Management");
            stage.setResizable(false);
            stage.initModality(Modality.APPLICATION_MODAL); // Bloquea ventanas anteriores

            // Inicializar Cliente REST y Lista de Datos
            client = new AccountRESTClient();
            accountsData = FXCollections.observableArrayList();
            tbAccounts.setItems(accountsData);

            // Definir el comportamiento al mostrar la ventana (Eventos)
            stage.setOnShowing(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent event) {
                    handleWindowShowing();
                }
            });
            
            // --- ACCIÓN BOTÓN CREATE ---
            btnCreate.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    // Lógica de "Toggle" basada en el texto del botón
                    if (btnCreate.getText().equalsIgnoreCase("Create")) {
                        // PASO 1: Habilitar modo creación
                        LOGGER.info("Iniciando modo creación de cuenta...");
                        
                        // Habilitar campos y limpiar
                        clearForm();
                        setFormVisible(true);
                        
                        // Pre-rellenar datos por defecto si quieres
                        tfBalance.setText("0.0");
                        
                        // Cambiar botón a modo "Guardar"
                        btnCreate.setText("Save");
                        
                        // Deshabilitar otros botones para evitar errores
                        btnModify.setDisable(true);
                        btnDelete.setDisable(true);
                        
                        // Foco a la descripción
                        tfDescription.requestFocus();
                        
                    } else {
                        // PASO 2: Guardar la cuenta (POST)
                        createAccount();
                    }
                }
            });

            // Configurar las columnas de la tabla (PropertyValueFactory)
            tcId.setCellValueFactory(new PropertyValueFactory<>("id"));
            tcDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
            tcType.setCellValueFactory(new PropertyValueFactory<>("type"));
            tcBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));
            tcCreditLine.setCellValueFactory(new PropertyValueFactory<>("creditLine"));
            tcBeginBalance.setCellValueFactory(new PropertyValueFactory<>("beginBalance"));
            tcDate.setCellValueFactory(new PropertyValueFactory<>("beginBalanceTimestamp"));
            // Cargar el ChoiceBox con los tipos de cuenta
            // FXCollections para convertir un array a lista observable
            cbType.setItems(FXCollections.observableArrayList(AccountType.values()));
            
            // Mostrar la ventana
            stage.show();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al inicializar el stage", e);
            showErrorAlert("Error initializing window: " + e.getMessage());
        }
    }

    /**
     * Configuracion del estado inicial y carga los datos.
     */
    private void handleWindowShowing() {
        LOGGER.info("Ventana mostrándose. Configurando estado inicial.");
        
        // Cargar tipos de cuenta en el combo si está vacío
        if (cbType.getItems().isEmpty()) {
            cbType.setItems(FXCollections.observableArrayList(AccountType.values()));
        }

        // Estado inicial: Todo reseteado
        resetUI();

        // Cargar datos
        loadData();
    }

    /**
     * Carga de datos filtrando directamente en el servidor por el ID del cliente.
     */
    private void loadData() {
        try {
            LOGGER.info("Cargando cuentas para el cliente ID: " + user.getId());
            
            // Limpiar la colección local para evitar duplicados visuales
            accountsData.clear();

            // Definir el tipo genérico para que Jersey entienda que es una List<Account>
            GenericType<List<Account>> genericType = new GenericType<List<Account>>() {};
            
            // Llamada al servidor
            List<Account> myAccounts = client.findAccountsByCustomerId_XML(genericType, String.valueOf(user.getId()));

            // Añadir a la tabla solo si hay datos
            if (myAccounts != null) {
                accountsData.addAll(myAccounts);
                LOGGER.info("Se han cargado " + myAccounts.size() + " cuentas.");
            }else{
                LOGGER.log(Level.SEVERE, "No existen cuentas asociadas a este cliente");
                showErrorAlert("No existe ninguna cuenta");
            }
            
            // Refrescar la tabla (ObservableList automático?)
            tbAccounts.setItems(accountsData);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar datos del servidor", e);
            showErrorAlert("Error loading your accounts. Please, try again later.\n" + e.getMessage());
        }
    }

    /**
     * Alerta de error simple.
     */
    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Operation Failed");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Habilita o deshabilita los campos del formulario.
     */
    private void setFormVisible(boolean visible) {
        // Campos de texto
        tfDescription.setDisable(!visible);
        tfCreditLine.setDisable(!visible);
        cbType.setDisable(!visible);
        
        // El balance y fecha los bloqueamos ya que suelen ser automáticos al crear
        tfBalance.setDisable(true); 
        tfDate.setDisable(true);
        
        // Si estamos editando, la tabla debe estar bloqueada para no cambiar de fila
        tbAccounts.setDisable(visible);
    }

    /**
     * Limpia los campos del formulario para una nueva entrada.
     */
    private void clearForm() {
        tfDescription.setText("");
        tfCreditLine.setText("");
        tfBalance.setText("0.0");
        tfDate.setText("");
        cbType.getSelectionModel().clearSelection();
        // Limpiar selección de tabla
        tbAccounts.getSelectionModel().clearSelection();
    }

    /**
     * Restaura la pantalla a su estado inicial (Solo lectura).
     */
    private void resetUI() {
        setFormVisible(false);
        clearForm();
        
        btnCreate.setText("Create");
        btnCreate.setDisable(false);
        
        // Modify y Delete desactivados hasta que se seleccione algo
        btnModify.setDisable(true);
        btnDelete.setDisable(true);
        
        lblMessage.setText("");
    }
    
    /**
     * Valida datos, crea el objeto y lo envía al servidor.
     */
    private void createAccount() {
        LOGGER.info("Intentando crear cuenta...");
        
        try {
            // --- 1. VALIDACIONES ---
            if (tfDescription.getText().trim().isEmpty()) {
                showErrorAlert("Description is required.");
                tfDescription.requestFocus();
                return;
            }
            
            if (cbType.getSelectionModel().getSelectedItem() == null) {
                showErrorAlert("Please select an Account Type.");
                cbType.requestFocus();
                return;
            }

            // Validar Credit Line si es necesario
            Double creditLine = 0.0;
            if (cbType.getSelectionModel().getSelectedItem() == AccountType.CREDIT) {
                if (tfCreditLine.getText().trim().isEmpty()) {
                    showErrorAlert("Credit Line is required for CREDIT accounts.");
                    return;
                }
                try {
                    creditLine = Double.parseDouble(tfCreditLine.getText());
                    if (creditLine < 0) {
                        showErrorAlert("Credit Line cannot be negative.");
                        return;
                    }
                } catch (NumberFormatException e) {
                    showErrorAlert("Credit Line must be a valid number.");
                    return;
                }
            }
            
            // --- 2. CREAR OBJETO MODELO ---
            Account newAccount = new Account();
            newAccount.setDescription(tfDescription.getText().trim());
            newAccount.setType(cbType.getSelectionModel().getSelectedItem());
            newAccount.setCreditLine(creditLine);
            newAccount.setBalance(0.0);
            newAccount.setBeginBalance(0.0);
            // La fecha la ponemos nosotros o el servidor. 
            // Para Java 8 Legacy usamos java.util.Date
            
            newAccount.setBeginBalanceTimestamp(new java.util.Date());
            
            // IMPORTANTE: Relación con el Cliente
            // Dependiendo de cómo lo espere el servidor XML. 
            // A veces basta con enviar el objeto Account limpio y el servidor lo asocia por la URL,
            // pero normalmente debemos setear el usuario si la relación es bidireccional.
            // newAccount.setCustomer(this.user); // Descomenta si tu modelo tiene setCustomer
            
            // --- 3. ENVÍO AL SERVIDOR ---
            // Usamos create_XML. Ojo: create_XML suele ser void.
            client.createAccount_XML(newAccount);
            
            // --- 4. ÉXITO ---
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Account created successfully.");
            alert.showAndWait();
            
            // Recargar datos y resetear interfaz
            loadData();
            resetUI();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating account", e);
            showErrorAlert("Error connecting to server: " + e.getMessage());
        }
    }
}