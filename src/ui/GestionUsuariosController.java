/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ui;

import java.util.logging.Logger;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

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
        LOGGER.info("Initializing SignUp");
        
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
