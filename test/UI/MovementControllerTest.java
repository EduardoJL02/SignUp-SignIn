package UI;

import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import static org.testfx.api.FxAssert.verifyThat;
import org.testfx.framework.junit.ApplicationTest;
import static org.testfx.matcher.base.NodeMatchers.isDisabled;
import static org.testfx.matcher.base.NodeMatchers.isEnabled;
import static org.testfx.matcher.base.NodeMatchers.isVisible;
import static org.testfx.matcher.control.LabeledMatchers.hasText;
// Asegúrate de importar tu clase principal donde arranca la app
import signup.signin.SignUpSignIn; 

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MovementControllerTest extends ApplicationTest {
    
    // Credenciales proporcionadas
    private static final String USUARIO = "awallace@gmail.com";
    private static final String PASS = "qwerty*9876";

    @Override
    public void start(Stage stage) throws Exception {
        // Arrancamos la aplicación desde el principio (Login)
        new SignUpSignIn().start(stage);
    }

    /**
     * Método auxiliar para realizar el Login y llegar a la pantalla de Movimientos.
     * Se reutiliza en cada test para situarnos en la pantalla correcta.
     */
    private void llegarAMovements() {
        // --- 1. LOGIN ---
        // Ajusta estos IDs (#) según tu FXML de Login
        clickOn("#EmailTextField"); 
        write(USUARIO);
        
        clickOn("#tfPassword"); // O el ID que tengas para la contraseña
        write(PASS);
        
        clickOn("#LoginButton"); // Botón de entrar
        
        // Esperamos a que cargue la ventana de Cuentas (puede tardar por el servidor)
        sleep(2000);
        verifyThat("#tbAccounts", isVisible()); // Confirmamos que estamos en Accounts

        // --- 2. SELECCIÓN DE CUENTA ---
        // Seleccionamos la tabla y pulsamos Abajo + Enter para elegir la primera fila
        clickOn("#tbAccounts");
        type(KeyCode.DOWN);
        type(KeyCode.ENTER);
        
        // --- 3. ABRIR MOVIMIENTOS ---
        verifyThat("#btnMovements", isEnabled());
        clickOn("#btnMovements");
        
        // Esperamos a que abra la ventana modal
        sleep(1000);
    }

    @Test
    public void test1_VerificarCargaDeDatosReales() {
        // Ejecutamos la navegación
        llegarAMovements();
        
        // Verificamos que se ha cargado la ventana de Movimientos
        verifyThat("#tvMovements", isVisible());
        
        // Verificamos que los datos del usuario "awallace" se ven en las etiquetas
        // (Ajusta el texto esperado si el nombre en BBDD es diferente, ej: "Alfred Wallace")
        verifyThat("#lblCustomerName", isVisible()); 
        
        // Verificamos que el selector de cuentas está activo
        verifyThat("#cbAccountSelector", isEnabled());
        
        // Salir para limpiar
        clickOn("#btBack");
    }

    @Test
    public void test2_CrearNuevoMovimiento() {
        llegarAMovements();
        
        // Verificamos botón de nueva fila
        verifyThat("#btNewRow", isEnabled());
        
        // Creamos la fila
        clickOn("#btNewRow");
        sleep(500);
        
        // Escribimos una cantidad en la celda seleccionada (la columna Amount)
        write("10.50");
        type(KeyCode.ENTER);
        
        sleep(1000);
        
        // Verificamos mensaje de estado (si tu controlador lo actualiza al guardar)
        verifyThat("#lblStatus", isVisible());
        
        clickOn("#btBack");
    }

    @Test
    public void test3_VolverAtrasFunciona() {
        llegarAMovements();
        
        verifyThat("#btBack", isVisible());
        
        // Pulsamos volver
        clickOn("#btBack");
        sleep(500);
        
        // Verificamos que hemos vuelto a la pantalla de Cuentas
        // (El botón de movimientos debe estar visible de nuevo)
        verifyThat("#btnMovements", isVisible());
        verifyThat("#tbAccounts", isVisible());
        
        // Opcional: Cerrar sesión para dejar limpio
        // clickOn("#btnLogOut"); 
    }
}