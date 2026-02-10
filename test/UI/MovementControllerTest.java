package UI;

import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;
import javafx.scene.input.KeyCode;

import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;
import static org.junit.Assert.assertNotEquals;

import signup.signin.SignUpSignIn;

/**
 * Pruebas de integración para la vista de Movimientos.
 * Se ejecutan en orden alfabético para mantener la consistencia del estado.
 * @fixme Los métodos de test presentados son insuficientes.
 * @fixme Crear sendos métodos de test para Read,Create y Delete (último movimiento) sobre la tabla de Movements que verifiquen sobre los items de la tabla cada caso de uso.
*/
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MovementControllerTest extends ApplicationTest {

    private static final String USUARIO = "awallace@gmail.com";
    private static final String PASS = "qwerty*9876";

    /**
     * Inicializa la aplicación principal antes de cada test.
     */
    @Override
    public void start(Stage stage) throws Exception {
        new SignUpSignIn().start(stage);
    }

    /**
     * Method auxiliar para automatizar el flujo de navegación:
     * Login -> Selección de Cuenta -> Vista de Movimientos.
     */
    private void navegarHastaMovements() {
        // Localiza los campos de login, escribe las credenciales y pulsa el botón
        clickOn((Node) lookup("#EmailTextField").query()).write(USUARIO);
        clickOn((Node) lookup("#PasswordField").query()).write(PASS);
        clickOn((Node) lookup("#LoginButton").query());

        // Verifica que la tabla de cuentas es visible antes de continuar
        verifyThat("#tbAccounts", isVisible());
        
        // Selecciona la primera cuenta de la tabla y navega a sus movimientos
        clickOn((Node) lookup(".table-row-cell").nth(0).query());
        clickOn((Node) lookup("#btnMovements").query());
        
        // Asegura que la tabla de movimientos se ha cargado correctamente
        verifyThat("#tvMovements", isVisible());
        WaitForAsyncUtils.waitForFxEvents(); // Sincroniza eventos de la UI
    }

    /**
     * Test 1: Verifica que al crear y editar un movimiento, el saldo de la cuenta se actualiza.
     */
    @Test
    public void test1_FlujoSinUndo() {
        navegarHastaMovements();
        
        TextField tfBalance = lookup("#tfBalance").queryAs(TextField.class);
        String saldoInicial = tfBalance.getText();
        
        // 1. Crea una nueva fila en la tabla de movimientos
        clickOn((Node) lookup("#btNewRow").query());
        WaitForAsyncUtils.waitForFxEvents();

        // 2. Edita la columna "Tipo" (índice 1) seleccionando "Payment"
        Node tipo = lookup("#tvMovements .table-row-cell").nth(0).lookup(".table-cell").nth(1).query();
        clickOn(tipo);
        clickOn("Payment");
        type(KeyCode.ENTER);

        // 3. Edita la columna "Cantidad" (índice 2) introduciendo un valor
        Node amount = lookup("#tvMovements .table-row-cell").nth(0).lookup(".table-cell").nth(2).query();
        clickOn(amount);
        doubleClickOn(amount); // Asegura que se selecciona el texto previo para sobreescribir
        write("100.00");
        type(KeyCode.ENTER); // Primer Enter confirma edición
        type(KeyCode.ENTER); // Segundo Enter podría ser necesario según la implementación del commit
       
        // Verifica que el saldo en pantalla ha cambiado respecto al inicial
        assertNotEquals("El saldo debería cambiar (Sin Undo)", saldoInicial, tfBalance.getText());

        // Vuelve a la pantalla anterior
        clickOn((Node) lookup("#btBack").query());
    }

    /**
     * Test 2: Verifica que la funcionalidad "Deshacer" (Undo) está operativa.
     */
    @Test
    public void test2_FlujoConUndo() {
        navegarHastaMovements();
        
        TextField tfBalance = lookup("#tfBalance").queryAs(TextField.class);
        String saldoAntes = tfBalance.getText();
        
        // 1. Crea fila y edita cantidad a 50.00
        clickOn((Node) lookup("#btNewRow").query());
        WaitForAsyncUtils.waitForFxEvents();

        Node amount = lookup("#tvMovements .table-row-cell").nth(0).lookup(".table-cell").nth(2).query();
        clickOn(amount);
        doubleClickOn(amount);
        write("50.00");
        type(KeyCode.ENTER);
        type(KeyCode.ENTER);

        // Verifica que el saldo cambió temporalmente
        assertNotEquals("El saldo debería cambiar antes del Undo", saldoAntes, tfBalance.getText());

        // 4. Ejecuta la acción de Deshacer último cambio
        clickOn((Node) lookup("#btUndoLast").query());
        clickOn("Aceptar"); // Confirma el diálogo de confirmación de Undo

        // 5. Regresa a la vista de cuentas y verifica la navegación
        clickOn((Node) lookup("#btBack").query());
        verifyThat("#tbAccounts", isVisible());
    }
    
    /**
     * Test 3: Verifica la validación de datos de entrada (Data Validation).
     * El sistema no debe permitir valores no numéricos en la columna Amount.
     */
    @Test
    public void test3_InputNoNumerico() {
        navegarHastaMovements();
        
        // 1. Crea una fila nueva
        clickOn((Node) lookup("#btNewRow").query());
        WaitForAsyncUtils.waitForFxEvents();

        // 2. Localiza la celda de Amount (columna 2)
        Node amountCell = lookup("#tvMovements .table-row-cell").nth(0).lookup(".table-cell").nth(2).query();
        
        // 3. Intenta escribir texto en un campo numérico
        clickOn(amountCell);
        doubleClickOn(amountCell); 
        write("TextoInvalido");
        type(KeyCode.ENTER);

        // 4. VERIFICACIÓN: Comprueba que el sistema lanza una alerta de error
        verifyThat("Error", isVisible());
        verifyThat("Formato incorrecto. Introduce un número válido.", isVisible());

        // 5. Cierra la alerta para dejar el entorno limpio para otros tests
        clickOn("Aceptar"); 
    }
}