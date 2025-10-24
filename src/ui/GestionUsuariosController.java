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
