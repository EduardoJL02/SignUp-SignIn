package UI;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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

/**
 * Controlador para la gestión de cuentas (Accounts).
 *
 * @author Desarrollo proyecto CRUD
 */
public class AccountsController implements Initializable {

    // Logger para trazas
    private static final Logger LOGGER = Logger.getLogger("UI.AccountsController");

    // Referencias a la UI (FXML)
    @FXML
    private TextField tfDescription;
    @FXML
    private ChoiceBox<AccountType> cbType;
    @FXML
    private TextField tfCreditLine;
    @FXML
    private TextField tfBalance;
    @FXML
    private TextField tfDate; // Creation Date (BeginBalanceTimestamp)

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

    // Botonera
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

    // Campos lógicos
    private Stage stage;
    private Customer user; // El usuario logueado
    private AccountRESTClient accountClient;
    private ObservableList<Account> accountsData;

    /**
     * Inicialización por defecto de JavaFX.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // No implementamos nada aquí porque preferimos hacerlo en setStage
        // para tener control total de cuándo inicia la ventana.
    }

    /**
     * Método principal para inicializar la ventana y recibir datos.
     *
     * @param root El nodo raíz cargado desde el FXML.
     * @param customer El cliente logueado que viene de la ventana anterior.
     */
    public void setStage(Parent root, Customer customer) {
        try {
            LOGGER.info("Inicializando ventana de Cuentas...");

            // 1. Crear la escena y el escenario (Stage)
            Scene scene = new Scene(root);
            stage = new Stage();
            stage.setScene(scene);

            // 2. Configurar propiedades de la ventana
            stage.setTitle("Accounts Management");
            stage.setResizable(false);
            stage.initModality(Modality.APPLICATION_MODAL); // Bloquea ventanas anteriores

            // 3. Guardar el usuario recibido
            this.user = customer;

            // 4. Inicializar componentes de UI
            // Cargar el Enum en el ChoiceBox
            cbType.setItems(FXCollections.observableArrayList(AccountType.values()));
            
            // La tabla NO debe ser editable
            tbAccounts.setEditable(false);

            // Configurar las columnas con los nombres de los atributos de Account
            tcId.setCellValueFactory(new PropertyValueFactory<>("id"));
            tcDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
            tcType.setCellValueFactory(new PropertyValueFactory<>("type"));
            tcBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));
            tcCreditLine.setCellValueFactory(new PropertyValueFactory<>("creditLine"));
            tcBeginBalance.setCellValueFactory(new PropertyValueFactory<>("beginBalance"));
            tcDate.setCellValueFactory(new PropertyValueFactory<>("beginBalanceTimestamp"));

            // 5. Instanciar el cliente REST
            accountClient = new AccountRESTClient();

            // 6. Cargar los datos del servidor
            loadAccountData();

            // 7. Mostrar la ventana
            stage.show();
            
            LOGGER.info("Ventana de Cuentas iniciada correctamente.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al inicializar la ventana de cuentas", e);
            showErrorAlert("Error initializing window: " + e.getMessage());
        }
    }

    /**
     * Carga los datos de las cuentas usando el cliente REST de forma síncrona.
     */
    private void loadAccountData() {
        try {
            LOGGER.info("Cargando cuentas para el cliente ID: " + user.getId());

            // PREPARAR GenericType para recibir List<Account>
            // Esto es necesario porque Java borra los tipos genéricos en tiempo de ejecución.
            GenericType<List<Account>> listType = new GenericType<List<Account>>() {};

            // LLAMADA AL SERVIDOR (Síncrona)
            // Usamos findAccountsByCustomerId_XML pasando el GenericType y el ID del usuario
            List<Account> accounts = accountClient.findAccountsByCustomerId_XML(listType, String.valueOf(user.getId()));

            // Convertir a ObservableList y setear en la tabla
            accountsData = FXCollections.observableArrayList(accounts);
            tbAccounts.setItems(accountsData);
            
            LOGGER.info("Se han cargado " + accountsData.size() + " cuentas.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar datos del servidor", e);
            showErrorAlert("No se pudieron cargar los datos: " + e.getMessage());
        }
    }

    /**
     * Muestra una alerta de error simple.
     * @param msg Mensaje a mostrar
     */
    private void showErrorAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Error en la aplicación");
        alert.setContentText(msg);
        alert.showAndWait();
    }
}