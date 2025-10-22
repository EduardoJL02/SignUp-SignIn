/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ui;

import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
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

    public void init(Stage stage) {
        LOGGER.info("Initializing SignUp");
        
        stage.setTitle("CREATE ACCOUNT");
        
        stage.setResizable(false);
        
        btBack.setDisable(true);
        btCreate.setDisable(true);
    }
    
}
