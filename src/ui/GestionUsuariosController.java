/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ui;

import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;
import logic.CustomerRESTClient;
import model.Customer;

/**
 *
 * @author pablo
 */
public class GestionUsuariosController {
    @FXML
    private TextField tfFName;
    
    @FXML
    private TextField tfMName;
    
    @FXML
    private TextField tfLName;
    
    @FXML
    private TextField tfAddress;
    
    @FXML
    private TextField tfCity;
    
    @FXML
    private TextField tfState;
    
    @FXML
    private TextField tfZip;
    
    @FXML
    private TextField tfPhone;
    
    @FXML
    private TextField tfEmail;
    
    @FXML
    private TextField tfPass;
    
    @FXML
    private TextField tfRPass;
    
    @FXML
    private Button btBack;
    
    @FXML
    private Button btCreate;
    
    private static final Logger LOGGER=Logger.getLogger("ui");

    public void init(Stage stage, Parent root) {
    try {
        // --- CORREGIDO ---
        LOGGER.log(Level.INFO, "Initializing SignUp");

        Scene scene = new Scene(root);
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(scene);
        stage.setTitle("CREATE ACCOUNT");
        stage.setResizable(false);
        
        btBack.setDisable(true);
        btCreate.setDisable(true);
        
        //Asociar manejadores a eventos
        btCreate.setOnAction(this::handleBtCreateOnAction);
        
        //Asociacion de manejadores a properties
        tfFName.textProperty().addListener(this::handleTfFNameTextChange);
        tfFName.focusedProperty().addListener(this::handleTfFNameFocusChange);
        
        //Mostrar la ventana
        stage.show();

    } catch (Exception e) {
        
        // --- CORREGIDO ---
        // Usamos Level.SEVERE para errores graves
        LOGGER.log(Level.SEVERE, "Error fatal al inicializar la ventana SignUp", e);
        
        // Es buena idea mostrar un error al usuario
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error de Aplicación");
        alert.setHeaderText("No se pudo cargar la ventana de registro.");
        alert.setContentText("Ocurrió un error inesperado: " + e.getMessage());
        alert.showAndWait();
    }
}
   
    /**
     * 
     * @param event 
     */
    private void handleBtCreateOnAction(ActionEvent event){
    try{
        //Crear un objeto customer
        Customer customer = new Customer();

        //Establecer propiedades del objeto a partir de los valores de los campos
        //*** CORRECCIÓN 1: USAR .getText() ***
        customer.setFirstName(tfFName.getText()); 
        customer.setMiddleInitial(tfMName.getText()); 
        customer.setLastName(tfLName.getText()); 
        // ... Asignar el resto de campos (Address, City, Email, Pass) ...

        CustomerRESTClient client = new CustomerRESTClient();
        //*** CORRECCIÓN 2: Un solo argumento ***
        client.create_XML(customer); 
        client.close();
            
        //Indicar al usuario que se ha registrado
        //*** CORRECCIÓN 3: Uso correcto de Alert ***
        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
        successAlert.setTitle("Registro Completo");
        successAlert.setHeaderText("¡Usuario Creado!");
        successAlert.setContentText("La cuenta se ha registrado correctamente.");
        successAlert.showAndWait();
            
        // Cerrar la ventana actual (opcional, pero recomendable tras un registro exitoso)
        ((Stage) btCreate.getScene().getWindow()).close();
            
    }catch(ForbiddenException e){
        LOGGER.log(Level.WARNING, "Email ya registrado.", e);
        new Alert(Alert.AlertType.WARNING, "Este email ya está en uso.").showAndWait();
    }catch(InternalServerErrorException e){
        LOGGER.log(Level.SEVERE, "Error interno del servidor al crear usuario.", e);
        new Alert(Alert.AlertType.ERROR, "Error de servidor. Intente más tarde.").showAndWait();
    }
}
    /**
     * 
     * @param observable
     * @param oldValue
     * @param newValue 
     */
    private void handleTfFNameTextChange(ObservableValue observable, String oldValue, String newValue){
        
    }
    /**
     * 
     * @param observable
     * @param oldValue
     * @param newValue 
     */
    private void handleTfFNameFocusChange(ObservableValue observable, Boolean oldValue, Boolean newValue){
        if(!newValue){
            
        }//new es la ganancia (true) y old la perdida (false)
    }
}

//Una ventana modal es la que hace que no puedas manejar la pantalla anterior
