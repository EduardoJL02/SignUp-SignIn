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

        // Estado inicial de los botones
        btnCreate.setDisable(false);  
        btnModify.setDisable(true);   // Temporal
        btnDelete.setDisable(true);   //temporal
        btnSearch.setDisable(false);
        btnReport.setDisable(false);

        // Estado inicial de los campos (No editables o limpios)
        tfDescription.setText("");
        tfCreditLine.setText("");
        tfBalance.setText("");
        tfDate.setText("");
        cbType.getSelectionModel().clearSelection();
        
        lblMessage.setText("");

        // Cargar datos del servidor
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
}