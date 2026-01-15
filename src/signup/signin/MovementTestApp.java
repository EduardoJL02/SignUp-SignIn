package signup.signin;

import UI.MovementController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import logic.CustomerRESTClient; // Necesitamos el cliente REST
import model.Customer;

public class MovementTestApp extends Application {

    private static final String MOVEMENTS_FXML_PATH = "/UI/FXMLDocumentMyMovements.fxml"; 

    @Override
    public void start(Stage stage) {
        try {
            Long customerId = 102263301L; 

            // 1. OBTENER DATOS REALES DEL CLIENTE DESDE LA BBDD
            CustomerRESTClient customerClient = new CustomerRESTClient();
            Customer realCustomer = null;
            
            try {
                // Buscamos el cliente por ID (formato XML)
                realCustomer = customerClient.find_XML(Customer.class, String.valueOf(customerId));
            } catch (Exception e) {
                // Si falla (ej. servidor apagado o ID no existe), creamos uno dummy para que no pete
                System.out.println("No se pudo descargar el cliente completo: " + e.getMessage());
                realCustomer = new Customer();
                realCustomer.setId(customerId);
                realCustomer.setFirstName("Offline");
                realCustomer.setLastName("User");
            }
            
            // 2. CARGAR VISTA
            FXMLLoader loader = new FXMLLoader(getClass().getResource(MOVEMENTS_FXML_PATH));
            Parent root = loader.load();

            // 3. PASAR EL CLIENTE COMPLETO AL CONTROLADOR
            MovementController controller = loader.getController();
            if (controller != null) {
                controller.setClientData(realCustomer);
            }

            Scene scene = new Scene(root);
            stage.setTitle("Bank App - Test ID: " + customerId);
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}