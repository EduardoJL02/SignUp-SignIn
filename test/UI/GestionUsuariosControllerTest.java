package UI;

import static org.testfx.matcher.base.NodeMatchers.isDisabled; 
import static org.testfx.matcher.base.NodeMatchers.isEnabled; 
import static org.testfx.matcher.base.NodeMatchers.isVisible; 
import javafx.stage.Stage;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.testfx.framework.junit.ApplicationTest;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.control.TextInputControlMatchers.hasText;
import signup.signin.SignUpSignIn;

/** 
 * Entorno de pruebas para GestionUsuariosController (Sign-In)
 * 
 * REQUISITOS:
 * - GlassFish activo
 * - Base de datos con usuario de prueba
 * - Dependencias TestFX agregadas al proyecto
 * 
 * @author Eduardo
 */ 
@FixMethodOrder(MethodSorters.NAME_ASCENDING) public class GestionUsuariosControllerTest extends ApplicationTest{
    @Override
    public void start(Stage stage) throws Exception {
        new SignUpSignIn().start(stage);
    }

    /**
     * TEST 1: Verifica el estado inicial de la ventana LOGIN
     * - Campos vacíos
     * - Botón Login deshabilitado
     */
    @Test
    public void test1_InitialState() {
        verifyThat("#EmailTextField", hasText(""));
        verifyThat("#PasswordField",hasText(""));
        verifyThat("#LoginButton", isDisabled());
    }

    /**
     * TEST 2: Verifica que el botón Login permanece deshabilitado
     * cuando faltan datos o son inválidos
     */
    @Test
    public void test2_LoginisDisabled(){
        clickOn("#EmailTextField");
        write("usuario");
        verifyThat("#LoginButton", isDisabled());
        eraseText(7);
        clickOn("#PasswordField");
        write("password");
        verifyThat("#LoginButton", isDisabled());
        eraseText(8);
        verifyThat("#LoginButton", isDisabled());
    }

    /**
     * TEST 3: Verifica que el botón Login se habilita
     * con formato válido de email y contraseña >= 8 caracteres
     */
    @Test
    public void test3_LoginisEnabled(){
        clickOn("#EmailTextField");
        write("username@gmail.com");
        clickOn("#PasswordField");
        write("password");
        verifyThat("#LoginButton", isEnabled());
    }
    
    /**
     * TEST 4: Verifica manejo de credenciales incorrectas (401 Unauthorized)
     * 
     * COMPORTAMIENTO ESPERADO:
     * - Mostrar mensaje inline "Email o contraseña incorrectos"
     * - El mensaje aparece en el Label Error_password
     * - Los campos deben resaltarse con borde rojo
     * 
     * IMPORTANTE: Requiere servidor GlassFish activo
     */
    public void test4_NotAuthorizedException() {
        clickOn("#EmailTextField");
        write("eduardo@gmail.com");
        clickOn("#PasswordField");
        write("qwerty*9876");
        verifyThat("#LoginButton", isEnabled());
        
        // Hacer clic en Login
        clickOn("#LoginButton");
        
        // Verificar que aparece el mensaje de error inline en Error_password
        verifyThat("#Error_password", node -> {
            javafx.scene.control.Label errorLabel = (javafx.scene.control.Label) node;
            String errorText = errorLabel.getText();
            
            // El mensaje debe contener "incorrectos" según tu código
            return errorText != null && 
                   !errorText.isEmpty() && 
                   errorText.toLowerCase().contains("incorrectos");
        });
        
        // Verificar que los campos siguen habilitados después del error
        verifyThat("#EmailTextField", isEnabled());
        verifyThat("#PasswordField", isEnabled());
        verifyThat("#LoginButton", isEnabled());
        
        // OPCIONAL: Verificar que el campo Password tiene foco y texto seleccionado
        // (según tu código: PasswordField.requestFocus() y selectAll())
        javafx.scene.control.PasswordField passwordField = 
            lookup("#PasswordField").query();
        verifyThat(passwordField, javafx.scene.Node::isFocused);
    }

    /**
     * TEST 5: Verifica que el hyperlink "Sign up" navega correctamente
     * 
     * COMPORTAMIENTO ESPERADO:
     * - Al hacer clic debe abrir ventana de registro (SignUp)
     * - Si la ventana no está implementada, debe mostrar Alert informativo
     */
    @Test
    public void test5_RegisterisEnabled() {
        clickOn("#SignUpLink");
        
        // Esperar a que aparezca la ventana o el Alert
        sleep(1000);
        
        try {
            verifyThat("#tfFName", isVisible());
            verifyThat("#btCreate", isVisible());
            verifyThat("#btBack", isVisible());
            
            clickOn("#btBack");
                      
            // Esperar a que vuelva al Login
            sleep(1000);
            
            // Verificar que estamos de vuelta en Login
            verifyThat("#EmailTextField", isVisible());
            verifyThat("#LoginButton", isVisible());
            
        } catch (Exception e) {
            //Si muestra un Alert informativo
            verifyThat(".dialog-pane", isVisible());
            
            // Cerrar el diálogo
            clickOn("Aceptar");
        }
    }
    
    /**
    * TEST 6: Verifica login exitoso con credenciales válidas
    */
   @Test
   public void test6_SuccessfulLogin() {
       clickOn("#EmailTextField");
       write("awallace@gmail.com"); // ← Usuario real
       clickOn("#PasswordField");
       write("qwerty*9876"); // ← Password real

       verifyThat("#LoginButton", isEnabled());
       clickOn("#LoginButton");

       // Esperar navegación a PaginaPrincipal
       sleep(2000);

       // Verificar que se abrió la ventana principal
       verifyThat("#WelcomeLabel", isVisible());
       verifyThat("#CustomerNameLabel", isVisible());
       verifyThat("#LogoutButton", isVisible());
       clickOn("#LogoutButton");
       
       sleep(500);
            verifyThat(".dialog-pane", isVisible());
            
            // Cerrar el diálogo
            clickOn("Aceptar");

   }
}