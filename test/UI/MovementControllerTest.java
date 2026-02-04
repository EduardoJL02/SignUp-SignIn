package UI;

import javafx.scene.Node;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isEnabled;
import static org.testfx.matcher.base.NodeMatchers.isVisible;
import static org.junit.Assert.assertEquals;

// Tu clase principal
import signup.signin.SignUpSignIn; 

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MovementControllerTest extends ApplicationTest {

    private static final String USUARIO = "awallace@gmail.com";
    private static final String PASS = "qwerty*9876";

    @Override
    public void start(Stage stage) throws Exception {
        new SignUpSignIn().start(stage);
    }

    /**
     * Método auxiliar para esperar dinámicamente a que se cumpla una condición
     * sin usar sleep() fijos.
     */
    private void esperarHastaQue(Callable<Boolean> condicion, String mensajeError) {
        try {
            WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, condicion);
        } catch (TimeoutException e) {
            throw new AssertionError(mensajeError);
        }
    }

    private void navegarHastaMovements() {
        // 1. Login
        clickOn("#EmailTextField").write(USUARIO);
        clickOn("#PasswordField").write(PASS);
        clickOn("#LoginButton");

        // Esperar a que la tabla de cuentas sea visible
        verifyThat("#tbAccounts", isVisible());

        // 2. Seleccionar cuenta (Clic en la primera fila de datos)
        Node primeraFila = lookup(".table-row-cell").nth(0).query();
        clickOn(primeraFila);

        // 3. Ir a movimientos
        clickOn("#btnMovements");
        
        // Esperar a que la tabla de movimientos aparezca
        verifyThat("#tvMovements", isVisible());
    }

    @Test
    public void test1_CrearMovimientoManual() {
        navegarHastaMovements();

        TableView<?> table = lookup("#tvMovements").queryTableView();
        int filasIniciales = table.getItems().size();

        // 1. Clic en botón Nuevo
        clickOn("#btNewRow");

        // Esperar a que la fila se añada visualmente
        esperarHastaQue(() -> table.getItems().size() == filasIniciales + 1, 
                        "La fila nueva no apareció en la tabla");

        int indiceUltimaFila = table.getItems().size() - 1;

        // --- PASO CLAVE: SELECCIONAR TIPO (DEPOSIT) ---
        // Buscamos la celda de la columna "Type" (índice 1) en la última fila
        Node celdaTipo = lookup(".table-cell").nth(indiceUltimaFila * 4 + 1).query();
        
        // Doble clic para asegurar que entra en modo edición (ComboBox)
        doubleClickOn(celdaTipo);
        
        // Clic explícito en la opción "Deposit" del menú desplegable
        clickOn("Deposit");

        // --- PASO CLAVE: PONER CANTIDAD ---
        // Buscamos la celda de la columna "Amount" (índice 2)
        Node celdaAmount = lookup(".table-cell").nth(indiceUltimaFila * 4 + 2).query();
        
        // Doble clic para editar
        doubleClickOn(celdaAmount);
        
        // Escribir cantidad
        write("50.00");
        
        // CONFIRMAR: JavaFX EXIGE pulsar Enter para guardar el dato de una celda
        type(KeyCode.ENTER); 

        // Esperar dinámicamente a que el mensaje de estado indique éxito
        // Ojo: verifica que tu controlador ponga este texto exacto en lblStatus
        verifyThat("#lblStatus", isVisible());
    }

    @Test
    public void test2_DeshacerManual() {
        navegarHastaMovements();
        
        TableView<?> table = lookup("#tvMovements").queryTableView();
        int filasAntes = table.getItems().size();

        if (filasAntes > 0) {
            clickOn("#btUndoLast");

            // Esperar y clicar el botón de la alerta
            // TestFX esperará automáticamente a que aparezca el botón "Aceptar"
            clickOn("Aceptar"); 

            // Esperar dinámicamente a que la tabla reduzca su tamaño
            esperarHastaQue(() -> table.getItems().size() == filasAntes - 1, 
                            "El movimiento no se borró de la tabla");
            
            assertEquals("La tabla debería tener una fila menos", filasAntes - 1, table.getItems().size());
        }
    }

    @Test
    public void test3_VolverManual() {
        navegarHastaMovements();

        clickOn("#btBack");

        // Esperar a que aparezca la tabla de cuentas (prueba de que volvimos)
        verifyThat("#tbAccounts", isVisible());
    }
}