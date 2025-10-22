/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package UI;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 *
 * @author edu
 */
public class GestionUsuariosController {
    
    @FXML
    private Button LoginButton;
    
    @FXML
    private PasswordField PasswordField;
    
    @FXML
    private TextField EmailTextField;
    
    @FXML
    private Hyperlink GetPasswordLink;
    
    @FXML
    private Hyperlink SignUpLink;
    
    @FXML
    private Label LabelTooltipPassword;
    
    @FXML
    private Tooltip PasswordTooltip;
    
    @FXML
    private Label EmailText;
      
    @FXML
    private Label PasswordText;
      
    @FXML
    private Label LoginText;
      
    @FXML
    private Text AccountText;
      
    @FXML
    private Label Error_email;
    
    @FXML
    private Label Error_password;
    private static final Logger LOGGER=Logger.getLogger("SignUpSignIn.UI");
    

    
    public void init(Stage stage) {
        LOGGER.info("Initializing login stage.");
//Establecer titulo de la ventana
        stage.setTitle("User management");
        
        //La ventana no debe ser redimensionable
        stage.setResizable(false);
        
        
        LoginButton.setDisable(true);    }
    
    
    
}
