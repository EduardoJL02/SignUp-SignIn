package UI;

import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;
import java.util.concurrent.TimeUnit;
import javafx.scene.input.KeyCode;

import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;
import static org.junit.Assert.assertNotEquals;

import signup.signin.SignUpSignIn;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MovementControllerTest extends ApplicationTest {

    private static final String USUARIO = "awallace@gmail.com";
    private static final String PASS = "qwerty*9876";

    @Override
    public void start(Stage stage) throws Exception {
        new SignUpSignIn().start(stage);
    }

    private void navegarHastaMovements() {
        // Casting a (Node) para evitar errores de compilación por ambigüedad
        clickOn((Node) lookup("#EmailTextField").query()).write(USUARIO);
        clickOn((Node) lookup("#PasswordField").query()).write(PASS);
        clickOn((Node) lookup("#LoginButton").query());

        verifyThat("#tbAccounts", isVisible());
        clickOn((Node) lookup(".table-row-cell").nth(0).query());
        clickOn((Node) lookup("#btnMovements").query());
        
        verifyThat("#tvMovements", isVisible());
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    public void test1_FlujoCompletoMovimiento() {
        navegarHastaMovements();
        
        // 1. Obtener balance inicial
        TextField tfBalance = lookup("#tfBalance").queryAs(TextField.class);
        String saldoInicial = tfBalance.getText();

        // 2. Crear nueva fila
        clickOn((Node) lookup("#btNewRow").query());
        WaitForAsyncUtils.waitForFxEvents();

        // 3. EDITAR TIPO: Seleccionar primero y luego cambiar
        Node celdaTipo = lookup("Deposit").query();
        clickOn(celdaTipo); 
        sleep(500, TimeUnit.MILLISECONDS); 
        clickOn("Payment"); 
        WaitForAsyncUtils.waitForFxEvents();

        // 4. EDITAR CANTIDAD: Localización por posición
        Node celdaAmount = lookup("#tvMovements .table-row-cell")
                            .nth(0) 
                            .lookup(".table-cell")
                            .nth(2) // Ajustado a la columna Amount
                            .query();

        // Secuencia de edición
        clickOn((Node) celdaAmount);
        sleep(500, TimeUnit.MILLISECONDS);
        doubleClickOn((Node) celdaAmount); 
        sleep(500, TimeUnit.MILLISECONDS);
        
        // Escribimos la cantidad
        write("150.00");
        
        // --- AQUÍ ESTÁ EL ENTER SOLICITADO ---
        // Pulsamos ENTER para validar la celda. 
        // A veces se requiere doble ENTER: uno para cerrar el editor y otro para confirmar la tabla.
        type(KeyCode.ENTER); 
        type(KeyCode.ENTER); 
        // -------------------------------------

        // 5. CONFIRMAR: Clic fuera para asegurar que el foco se pierda
        clickOn((Node) tfBalance); 

        // 6. Esperar a que el servidor REST procese y el saldo se actualice
        sleep(3, TimeUnit.SECONDS); 
        
        // 7. Verificar cambio de saldo
        assertNotEquals("El saldo debería haber cambiado tras el ENTER", 
                       saldoInicial, tfBalance.getText());

        // 6. SINCRONIZACIÓN: Esperar respuesta del servidor REST
        // Se aumenta el tiempo para evitar el AssertionFailedError
        sleep(4, TimeUnit.SECONDS); 
        
        // 7. VERIFICAR ACTUALIZACIÓN
        String saldoFinal = tfBalance.getText();
        assertNotEquals("Error: El saldo no cambió tras la edición. Inicial: " + saldoInicial, 
                       saldoInicial, saldoFinal);

        // 8. DESHACER Y VOLVER
        clickOn((Node) lookup("#btUndoLast").query());
        clickOn("Aceptar"); 
        sleep(2, TimeUnit.SECONDS);
        clickOn((Node) lookup("#btBack").query());
        verifyThat("#tbAccounts", isVisible());
    }
}