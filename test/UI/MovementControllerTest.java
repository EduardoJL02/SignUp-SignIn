package UI;

import javafx.scene.Node;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;
import javafx.scene.input.KeyCode;
import model.Movement;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;

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
     * Test 1: Operación READ.
     * Verifica que la tabla carga datos al entrar y que todos son objetos de tipo Movement.
     */
    @Ignore
    @Test
    public void test_ReadMovements() {
        navegarHastaMovements();
        
        // Obtenemos la tabla de forma segura
        TableView<Movement> tv = lookup("#tvMovements").queryAs(TableView.class);
        
        // 1. Verificamos que la tabla no esté vacía
        assertNotNull("La TableView no debería ser nula", tv);
        assertFalse("La tabla debería tener movimientos cargados (no estar vacía)", tv.getItems().isEmpty());

        // 2. Verificamos la integridad de los datos recorriendo la lista
        for (Object item : tv.getItems()) {
            assertTrue("Cada elemento de la tabla debe ser una instancia de Movement. Se encontró: " + item.getClass().getName(),
                       item instanceof Movement);
            
            // Opcional: Validar que campos críticos no sean nulos
            Movement m = (Movement) item;
            assertNotNull("El ID del movimiento no debería ser nulo", m.getId());
        }
    }

    /**
     * Test 2: Operación CREATE.
     * Crea un movimiento de ingreso y verifica el aumento en la tabla.
     */
    
    @Test
    public void test_CreateMovement() {
        navegarHastaMovements();
        
        TableView<Movement> tv = lookup("#tvMovements").queryAs(TableView.class);
        int totalInicial = tv.getItems().size();

        // 1. Crear la fila nueva
        clickOn("#btNewRow"); 
        WaitForAsyncUtils.waitForFxEvents();

        // 2. Obtener el índice de la nueva fila (la última)
        int lastRowIndex = tv.getItems().size() - 1;

        
        
        WaitForAsyncUtils.waitForFxEvents();

        // 4. Escribir en el TextField que acaba de aparecer
        // Al forzar la edición, la celda se convierte en un nodo ".text-field"
        // Le hacemos click para asegurar el foco del teclado
        clickOn(".text-field")
            .write("200.00")
            .push(KeyCode.ENTER); // Confirmar dato y disparar commit

        WaitForAsyncUtils.waitForFxEvents();

        // 5. Verificación
        assertEquals("La tabla debería haber crecido en 1", 
                     totalInicial + 1, tv.getItems().size());
        
        Movement lastCreated = tv.getItems().get(lastRowIndex);
        
        // Verificamos el valor
        assertEquals("El importe no se guardó correctamente", 
                     200.0, lastCreated.getAmount(), 0.01);
    }

    /**
     * Test 3: Operación DELETE (Undo Last).
     * Borra el último movimiento y verifica que la lista disminuye.
     */
    @Ignore
    @Test
    public void test_DeleteLastMovement() {
        navegarHastaMovements();
        TableView<Movement> tv = lookup("#tvMovements").queryAs(TableView.class);
        int totalAntes = tv.getItems().size();
        
        assertTrue("Debe haber datos para probar el borrado", totalAntes > 0);

        // 1. Pulsar el botón de deshacer (ID: btUndoLast)
        clickOn("#btUndoLast");
        
        // 2. Interactuar con el Alert de confirmación
        // TestFX busca el botón por el texto "Aceptar" o "OK" según el idioma del OS
        Node btnAceptar = lookup("Aceptar").query(); 
        if(btnAceptar == null) btnAceptar = lookup("OK").query();
        clickOn(btnAceptar);
        
        WaitForAsyncUtils.waitForFxEvents();

        // 3. Verificación
        assertEquals("La tabla debería tener un elemento menos tras el DELETE", 
                     totalAntes - 1, tv.getItems().size());
    }

    /**
     * Test 4: Validación de datos de entrada.
     * Verifica que el sistema no acepta texto en campos numéricos.
     */
    @Ignore
    @Test
    public void test_InputValidation() {
        navegarHastaMovements();
        clickOn((Node) lookup("#btNewRow").query());

        Node amountCell = lookup("#tvMovements .table-row-cell").nth(0).lookup(".table-cell").nth(2).query();
        clickOn(amountCell).doubleClickOn(amountCell).write("ERROR_TEXT");
        type(KeyCode.ENTER);

        // Debería aparecer un diálogo de error
        verifyThat("Error", isVisible());
        clickOn("Aceptar");
    }

    /**
     * Test 5: Flujo de actualización de balance.
     * Verifica que el balance de la cuenta cambia al añadir un movimiento.
     */
    @Ignore
    @Test
    public void test_BalanceUpdate() {
        navegarHastaMovements();
        TextField tfBalance = lookup("#tfBalance").queryAs(TextField.class);
        String balanceInicial = tfBalance.getText();

        clickOn((Node) lookup("#btNewRow").query());
        Node amountCell = lookup("#tvMovements .table-row-cell").nth(0).lookup(".table-cell").nth(2).query();
        clickOn(amountCell).write("50.00");
        type(KeyCode.ENTER);
        type(KeyCode.ENTER); // Confirmar commit

        WaitForAsyncUtils.waitForFxEvents();
        assertNotEquals("El balance debería haberse actualizado", balanceInicial, tfBalance.getText());
    }
    
    /**
     * Test 1: Verifica que al crear y editar un movimiento, el saldo de la cuenta se actualiza.
    
    @Ignore
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
    
    @Ignore
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
    
    @Ignore
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
        
        // Vuelve a la pantalla anterior
        clickOn((Node) lookup("#btBack").query());
    }*/
}