/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package signup.signin;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import UI.GestionUsuariosControllerSignUp;

/**
 *
 * @author Pablo
 */
public class SignUpSignIn extends Application {
    
    @Override
    public void start(Stage stage) throws Exception {
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/UI/FXMLDocumentSignUp.fxml"));
        Parent root = (Parent)loader.load();
        
        GestionUsuariosControllerSignUp controller=loader.getController();
        
        // ⚠️ Nota: El método init() dentro del controlador ahora maneja la creación
        // del NUEVO Stage (myStage) y establece el 'stage' de start() como su propietario.
        // El 'stage' de start() no se usa directamente para mostrar la interfaz.
        controller.init(stage, root);
        
        // Opcional: Si este Stage no se usa para mostrar nada (es solo el propietario),
        // puedes mantenerlo oculto o cerrarlo después de la inicialización,
        // aunque es más limpio dejarlo abierto como propietario de la aplicación.
        // stage.hide(); 
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}