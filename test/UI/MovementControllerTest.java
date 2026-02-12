package UI;

import javafx.scene.Node;
import javafx.scene.control.TableView;
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
import static org.junit.Assert.assertNotNull;
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
     Test 1: Valido que la tabla tiene movimimientos
     */
    @Test
    @Ignore
    public void test1_ReadMovements() {
        navegarHastaMovements();
        TableView<Movement> tv = lookup("#tvMovements").queryAs(TableView.class);
        assertNotNull("La tabla no debe ser nula", tv);
        assertFalse("La tabla debe tener datos", tv.getItems().isEmpty());
    }

    /**
     * Test 2: CREATE Depósito.
     */
    @Test
    @Ignore
    public void test2_CreateDeposit() {
        navegarHastaMovements();
        TableView<Movement> tv = lookup("#tvMovements").queryAs(TableView.class);
        int totalInicial = tv.getItems().size();

        // 1. Crear fila (El controlador la selecciona automáticamente)
        clickOn("#btNewRow"); 
        WaitForAsyncUtils.waitForFxEvents();

        // 2. Localizar la celda Amount (columna 2) DENTRO de la fila SELECCIONADA
        // Esto evita errores de índices visuales vs lógicos
        Node row = lookup(".table-row-cell:selected").query();
        Node amountCell = from(row).lookup(".table-cell").nth(2).query();
        
        // 3. Editar (Doble click es necesario para TextFieldTableCell)
        doubleClickOn(amountCell);
        write("50.00");
        push(KeyCode.ENTER);
        WaitForAsyncUtils.waitForFxEvents();

        // 4. Verificación
        assertEquals("La tabla debería crecer", totalInicial + 1, tv.getItems().size());
        
        // Obtenemos el último elemento real de los datos para verificar
        Movement created = tv.getItems().get(tv.getItems().size() - 1);
        assertEquals("Importe correcto", 50.0, created.getAmount(), 0.01);
    }

    /**
     * Test 3: CREATE Payment (Lógica de conversión a negativo).
     */
    @Test
    @Ignore
    public void test3_CreatePayment_Logic() {
        navegarHastaMovements();
        TableView<Movement> tv = lookup("#tvMovements").queryAs(TableView.class);
        
        clickOn("#btNewRow");
        WaitForAsyncUtils.waitForFxEvents();
        
        // 1. Buscamos la fila seleccionada (la nueva)
        Node row = lookup(".table-row-cell:selected").query();
        
        // 2. Cambiar Tipo a "Payment" USANDO TECLADO
        Node typeCell = from(row).lookup(".table-cell").nth(1).query();
        clickOn(typeCell); // Foco en la celda
        
        // En vez de buscar el texto "Payment" con el ratón (que puede fallar),
        // pulsamos la flecha ABAJO para cambiar la selección del combo y ENTER para confirmar.
        type(KeyCode.DOWN); 
        type(KeyCode.ENTER);
        WaitForAsyncUtils.waitForFxEvents();
        
        // 3. Escribir importe POSITIVO en Amount (Columna 2)
        // Volvemos a buscar la fila seleccionada por si el foco cambió ligeramente
        row = lookup(".table-row-cell:selected").query(); 
        Node amountCell = from(row).lookup(".table-cell").nth(2).query();
        
        doubleClickOn(amountCell); 
        write("20.00");
        push(KeyCode.ENTER);
        WaitForAsyncUtils.waitForFxEvents();

        // 4. Verificar que se convirtió a negativo
        // Obtenemos el último ítem de la lista de datos
        Movement created = tv.getItems().get(tv.getItems().size() - 1);
        assertEquals("Debe convertirse a negativo", -20.0, created.getAmount(), 0.01);
    }

    /**
     * Test 4: Validación de Fondos (Lógica de Negocio).
     */
    @Test
    @Ignore
    public void test4_BusinessLogic_InsufficientFunds() {
        navegarHastaMovements();
        TableView<Movement> tv = lookup("#tvMovements").queryAs(TableView.class);
        int totalAntes = tv.getItems().size();

        clickOn("#btNewRow");
        WaitForAsyncUtils.waitForFxEvents();

        Node row = lookup(".table-row-cell:selected").query();

        // 1. Tipo Payment
        Node typeCell = from(row).lookup(".table-cell").nth(1).query();
        clickOn(typeCell);
        
        type(KeyCode.DOWN);
        type(KeyCode.ENTER);
        WaitForAsyncUtils.waitForFxEvents();

        // 2. Importe excesivo
        row = lookup(".table-row-cell:selected").query();
        Node amountCell = from(row).lookup(".table-cell").nth(2).query();
        
        doubleClickOn(amountCell);
        write("99999999");
        push(KeyCode.ENTER);
        WaitForAsyncUtils.waitForFxEvents();
        
        // 3. Verificar alerta de forma segura para evitar NPE
        // Usamos lookup con try-catch o verificamos existencia antes de interactuar
        verifyThat(".dialog-pane", isVisible()); // .dialog-pane es más seguro que .alert
        
        // Buscar el botón Aceptar dentro del diálogo
        clickOn("Aceptar"); 
        
        WaitForAsyncUtils.waitForFxEvents();

        // 4. Verificar Rollback
        assertEquals("El movimiento no debe guardarse", totalAntes, tv.getItems().size());
    }

    @Test
    @Ignore
    public void test5_InputValidation() {
        navegarHastaMovements();
        clickOn("#btNewRow");
        
        Node row = lookup(".table-row-cell:selected").query();
        Node amountCell = from(row).lookup(".table-cell").nth(2).query();
        
        doubleClickOn(amountCell);
        write("TEXTO");
        push(KeyCode.ENTER);

        verifyThat(".dialog-pane", isVisible());
        clickOn("Aceptar");
    }

    @Test
    @Ignore
    public void test6_DeleteLastMovement() {
        navegarHastaMovements();
        TableView<Movement> tv = lookup("#tvMovements").queryAs(TableView.class);
        
        // Aseguramos datos para borrar
        if(tv.getItems().isEmpty()) {
            clickOn("#btNewRow");
            Node row = lookup(".table-row-cell:selected").query();
            Node amount = from(row).lookup(".table-cell").nth(2).query();
            doubleClickOn(amount);
            write("10");
            push(KeyCode.ENTER);
        }

        int totalAntes = tv.getItems().size();

        clickOn("#btUndoLast");
        clickOn("Aceptar"); // Ajustar si el botón es "OK" o "Sí"
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals("Debe haber una fila menos", totalAntes - 1, tv.getItems().size());
    }
}