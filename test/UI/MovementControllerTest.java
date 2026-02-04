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
    public void test1_FlujoSinUndo() {
        navegarHastaMovements();
        TextField tfBalance = lookup("#tfBalance").queryAs(TextField.class);
        String saldoInicial = tfBalance.getText();
        
        // 1. Crear fila
        clickOn((Node) lookup("#btNewRow").query());
        WaitForAsyncUtils.waitForFxEvents();

        // 2. Editar Tipo
        Node tipo = lookup("#tvMovements .table-row-cell").nth(0).lookup(".table-cell").nth(1).query();
        clickOn(tipo);
        clickOn("Payment");
        type(KeyCode.ENTER);

        // 3. Editar Cantidad
        Node amount = lookup("#tvMovements .table-row-cell").nth(0).lookup(".table-cell").nth(2).query();
        clickOn(amount);
        doubleClickOn(amount);
        write("100.00");
        type(KeyCode.ENTER);
        type(KeyCode.ENTER);
       
        assertNotEquals("El saldo debería cambiar (Sin Undo)", saldoInicial, tfBalance.getText());

        // Salida limpia para el siguiente test
        clickOn((Node) lookup("#btBack").query());
    }

    @Test
    public void test2_FlujoConUndo() {
        navegarHastaMovements();
        TextField tfBalance = lookup("#tfBalance").queryAs(TextField.class);
        String saldoAntes = tfBalance.getText();
        
        // 1. Crear fila
        clickOn((Node) lookup("#btNewRow").query());
        WaitForAsyncUtils.waitForFxEvents();

        // 2. Editar Cantidad
        Node amount = lookup("#tvMovements .table-row-cell").nth(0).lookup(".table-cell").nth(2).query();
        clickOn(amount);
        doubleClickOn(amount);
        write("50.00");
        type(KeyCode.ENTER);
        type(KeyCode.ENTER);

        assertNotEquals("El saldo debería cambiar antes del Undo", saldoAntes, tfBalance.getText());

        // 4. Realizar Deshacer (Undo Last)
        clickOn((Node) lookup("#btUndoLast").query());
        clickOn("Aceptar");

        // 5. Salida final
        clickOn((Node) lookup("#btBack").query());
        verifyThat("#tbAccounts", isVisible());
    }
}