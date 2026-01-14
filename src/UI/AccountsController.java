package UI;

import java.util.List;
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

            // 1. Guardar el usuario recibido
            this.user = customer;

            // 2. Configurar la Escena y el Stage
            Scene scene = new Scene(root);
            stage = new Stage();
            stage.setScene(scene);
            
            // Propiedades de la ventana
            stage.setTitle("Accounts Management");
            stage.setResizable(false);
            stage.initModality(Modality.APPLICATION_MODAL); // Bloquea ventanas anteriores

            // 3. Inicializar Cliente REST y Lista de Datos
            client = new AccountRESTClient();
            accountsData = FXCollections.observableArrayList();
            tbAccounts.setItems(accountsData);

            // 4. Definir el comportamiento al mostrar la ventana (Eventos)
            stage.setOnShowing(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent event) {
                    handleWindowShowing();
                }
            });

            // 5. Configurar las columnas de la tabla (PropertyValueFactory)
            // Nota: Los nombres de strings deben coincidir EXACTAMENTE con los atributos del modelo Account
            tcId.setCellValueFactory(new PropertyValueFactory<>("id"));
            tcDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
            tcType.setCellValueFactory(new PropertyValueFactory<>("type"));
            tcBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));
            tcCreditLine.setCellValueFactory(new PropertyValueFactory<>("creditLine"));
            tcBeginBalance.setCellValueFactory(new PropertyValueFactory<>("beginBalance"));
            tcDate.setCellValueFactory(new PropertyValueFactory<>("creationDate"));

            // 6. Cargar el ChoiceBox con los tipos de cuenta
            // Usamos FXCollections para convertir un array a lista observable
            cbType.setItems(FXCollections.observableArrayList(AccountType.values()));

            // 7. Mostrar la ventana
            stage.show();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al inicializar el stage", e);
            showErrorAlert("Error initializing window: " + e.getMessage());
        }
    }

    /**
     * Lógica que se ejecuta cuando la ventana se está mostrando.
     * Configura el estado inicial y carga los datos.
     */
    private void handleWindowShowing() {
        LOGGER.info("Ventana mostrándose. Configurando estado inicial.");

        // 1. Estado inicial de los botones
        btnCreate.setDisable(false);  // Se puede crear
        btnModify.setDisable(true);   // Deshabilitado hasta seleccionar fila
        btnDelete.setDisable(true);   // Deshabilitado hasta seleccionar fila
        btnSearch.setDisable(false);
        btnReport.setDisable(false);

        // 2. Estado inicial de los campos (No editables o limpios)
        tfDescription.setText("");
        tfCreditLine.setText("");
        tfBalance.setText("");
        tfDate.setText("");
        cbType.getSelectionModel().clearSelection();
        
        lblMessage.setText("");

        // 3. Cargar datos del servidor
        loadData();
    }

    /**
     * Conecta con el servidor y obtiene las cuentas DEL USUARIO.
     */
    private void loadData() {
        try {
            LOGGER.info("Cargando cuentas para el cliente ID: " + user.getId());
            
            // Limpiar la tabla antes de cargar
            accountsData.clear();

            // Usamos GenericType para recuperar una Lista de Cuentas
            GenericType<List<Account>> genericType = new GenericType<List<Account>>() {};
            
            // LLAMADA CORREGIDA:
            // 1. Usamos findAccountsByCustomerId_XML (no findAll)
            // 2. Pasamos 'genericType' (ahora soportado por el cliente REST)
            // 3. Pasamos el ID del usuario logueado convertido a String
            List<Account> customerAccounts = client.findAccountsByCustomerId_XML(genericType, user.getId().toString());

            // Añadimos los datos a la lista observable de la tabla
            if (customerAccounts != null) {
                accountsData.addAll(customerAccounts);
            }
            
            tbAccounts.setItems(accountsData); // Refrescar tabla
            LOGGER.info("Cuentas cargadas: " + accountsData.size());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar datos del servidor", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error de Conexión");
            alert.setHeaderText("No se pudieron cargar las cuentas");
            alert.setContentText("Detalle: " + e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Método auxiliar para verificar si una cuenta pertenece a un cliente.
     * Itera sobre la colección de clientes de la cuenta.
     */
    private boolean isAccountOwner(Account account, Customer customer) {
        // Validación de nulos
        if (account == null || customer == null) return false;
        
        // Asumiendo que Account tiene una lista de customers
        // Ajustar 'getCustomers()' según el nombre real en tu modelo Account.java
        // Podría ser getCustomer() si es OneToMany
        /* Si Account.java tiene: private List<Customer> customers;
        */
        /*
        for (Customer c : account.getCustomers()) {
            if (c.getId().equals(customer.getId())) {
                return true;
            }
        }
        */
        
        // OPCIÓN B: Si la lógica es "Customer tiene lista de Accounts" y no al revés,
        // la lógica en loadData debería haber sido diferente.
        // Pero basándonos en "consultar accounts_id", asumimos que la cuenta sabe su dueño.
        
        // OPCIÓN C (Común en estos proyectos): Comprobar IDs simples si la entidad no está llena
        // return account.getCustomer().getId().equals(customer.getId());
        
        // POR AHORA: Devolvemos true a todo para probar la carga visual si no tienes la relación clara,
        // pero aquí es donde debes poner tu condición real.
        return true;
    }

    /**
     * Muestra una alerta de error simple.
     */
    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Operation Failed");
        alert.setContentText(message);
        alert.showAndWait();
    }
}