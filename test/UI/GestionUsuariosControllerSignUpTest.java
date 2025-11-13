package UI;

import javafx.stage.Stage;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import static org.testfx.api.FxAssert.verifyThat;
import org.testfx.framework.junit.ApplicationTest;
import static org.testfx.matcher.base.NodeMatchers.isDisabled;
import static org.testfx.matcher.base.NodeMatchers.isEnabled;
import static org.testfx.matcher.base.NodeMatchers.isFocused;
import static org.testfx.matcher.base.NodeMatchers.isVisible;
import signup.signin.SignUpSignIn;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GestionUsuariosControllerSignUpTest extends ApplicationTest {
    
    /**
     * Inicializa la aplicación con la ventana LOGIN.
     * TestFX llamará a este método automáticamente antes de cada test.
     */
    @Override
    public void start(Stage stage) throws Exception {
        new SignUpSignIn().start(stage);
    }

    /**
     * TEST 1: Verifica el estado inicial de la ventana Sign-Up.
     * 
     * VALIDACIONES:
     * - Link "Sign up here" está habilitado en Login
     * - Botón Login está deshabilitado (sin credenciales)
     * - Al hacer clic en Sign-Up, se abre la ventana modal
     * - Botón CREATE ACCOUNT está deshabilitado (campos vacíos)
     * - El foco inicial está en el campo First Name
     */
    @Test
    public void test1_InitialTests() {
        // 1. Verificar estado inicial en LOGIN
        verifyThat("#SignUpLink", isEnabled());
        verifyThat("#LoginButton", isDisabled());
        
        // 2. Navegar a Sign-Up
        clickOn("#SignUpLink");
        sleep(1000); // Esperar a que se abra la ventana modal
        
        // 3. Verificar estado inicial en SIGN-UP
        verifyThat("#btCreate", isDisabled());
        verifyThat("#tfFName", isFocused());
        
        // 4. IMPORTANTE: Cerrar la ventana Sign-Up para no interferir con otros tests
        clickOn("#btBack");
        sleep(500);
    }
    
    /**
     * TEST 2: Verifica el proceso completo de registro exitoso.
     * 
     * FLUJO:
     * 1. Navegar desde Login a Sign-Up
     * 2. Llenar todos los campos con datos válidos
     * 3. Verificar que el botón CREATE ACCOUNT se habilita
     * 4. Hacer clic en CREATE ACCOUNT
     * 5. Verificar que aparece el Alert de éxito
     * 6. Cerrar el Alert (esto también cierra la ventana Sign-Up)
     * 7. Verificar que volvemos a Login
     * 
     * IMPORTANTE: 
     * - Usa un email ÚNICO que NO exista en la base de datos
     * - Si falla, verifica que el servidor GlassFish esté activo
     */
    @Test
    public void test2_SignUpExitoso() {
        // 1. Navegar a Sign-Up desde Login
        clickOn("#SignUpLink");
        sleep(1000);
        
        // 2. Llenar formulario con datos válidos
        clickOn("#tfFName");
        write("Jorge");
        
        clickOn("#tfMName");
        write("G.");
        
        clickOn("#tfLName");
        write("Linares");
        
        clickOn("#tfAddress");
        write("Calle contubernio 45"); // Corrección: "streen" → "street"
        
        clickOn("#tfCity");
        write("Istanbul"); // Corrección: "Turkey" es un país, no una ciudad
        
        clickOn("#tfState");
        write("TR"); // Corrección: Usar código de 2 letras
        
        clickOn("#tfZip");
        write("34000"); // ZIP válido para Estambul (5 dígitos)
        
        clickOn("#tfPhone");
        write("905551234567"); // Formato internacional Turquía
        
        clickOn("#tfEmail");
        // IMPORTANTE: Cambiar el timestamp para que el email sea único en cada ejecución
        String uniqueEmail = "jorge.gonzalez." + System.currentTimeMillis() + "@gmail.com";
        write(uniqueEmail);
        
        clickOn("#tfPass");
        write("Abcd!1234"); // Cumple todos los requisitos
        
        clickOn("#tfRPass");
        write("Abcd!1234");
        
        // 3. Verificar que el botón CREATE ACCOUNT está habilitado
        verifyThat("#btCreate", isEnabled());
        
        // 4. Hacer clic en CREATE ACCOUNT
        clickOn("#btCreate");
        
        // 5. Esperar a que aparezca el Alert de éxito
        sleep(2000); // Tiempo para la petición REST
        
        // 6. Verificar que aparece el diálogo de éxito
        verifyThat(".dialog-pane", isVisible());
        
        // 7. Cerrar el diálogo (esto cierra automáticamente la ventana Sign-Up)
        clickOn("Aceptar");
        sleep(1000);
        
        // 8. Verificar que volvimos a Login
        verifyThat("#EmailTextField", isVisible());
        verifyThat("#LoginButton", isVisible());
    }
    
    @Test
    public void test3_EmailDuplicado() {
        clickOn("#SignUpLink");
        sleep(1000);
        
        clickOn("#tfFName");
        write("Pablo");
        
        clickOn("#tfMName");
        write("R.");
        
        clickOn("#tfLName");
        write("Rodriguez");
        
        clickOn("#tfAddress");
        write("C/ Arcipreste de Hita, 5");
        
        clickOn("#tfCity");
        write("Madrid");
        
        clickOn("#tfState");
        write("MD"); 
        
        clickOn("#tfZip");
        write("28220");
        
        clickOn("#tfPhone");
        write("692879385");
        
        clickOn("#tfEmail");
        
        write("rodrguezdelgado.pablo@gmail.com");
        
        clickOn("#tfPass");
        write("Qwerty*9876");
        
        clickOn("#tfRPass");
        write("Qwerty*9876");
        
        
        verifyThat("#btCreate", isEnabled());
        
        clickOn("#btCreate");
        
        
        sleep(2000);
        verifyThat(".dialog-pane", isVisible());
        
        clickOn("Aceptar");
        sleep(500);
        
        verifyThat("#tfFName", isVisible());
        verifyThat("#btCreate", isVisible());
        verifyThat("#btBack", isVisible());
        
        // 9. Cerrar manualmente la ventana Sign-Up
        clickOn("#btBack");
        sleep(500);
        
        // 6. Verificar que aparece el Alert de error
        verifyThat(".dialog-pane", isVisible());
        
        // 7. Cerrar el Alert
        clickOn("Aceptar");
        sleep(500);
        
        // 10. Verificar que volvimos a Login
        verifyThat("#EmailTextField", isVisible());
    }  
}