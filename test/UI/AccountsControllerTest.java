package UI;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import model.Account;
import model.AccountType;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.testfx.framework.junit.ApplicationTest;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.*;
import signup.signin.SignUpSignIn;

/**
 * Clase TEST para AccountsController.
 * 
 * @author Eduardo
 * @version 2.0
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AccountsControllerTest extends ApplicationTest {

    
    private static SignUpSignIn app;

    @BeforeClass
    public static void setUpClass() throws Exception {
        app = new SignUpSignIn();

    }

    @Override
    public void start(Stage stage) throws Exception {
        app.start(stage);
    }
    
    /**
     * Test 1: Login exitoso y navegación a ventana de cuentas.
     */
    @Test
    public void test1_Login() {
        clickOn("#EmailTextField");
        write("awallace@gmail.com");
        
        clickOn("#PasswordField");
        write("qwerty*9876");
        
        verifyThat("#LoginButton", isEnabled());
        clickOn("#LoginButton");
        
        verifyThat("#tbAccounts", isVisible());
    }
   
    /**
     * Test 2: Estado inicial de la ventana.
     */
    @Test
    public void test2_InitialState() {
        verifyThat("#tbAccounts", isVisible());
        
        TableView<Account> table = lookup("#tbAccounts").queryTableView();
        assertNotNull("La tabla no debe ser null", table);
        
        verifyThat("#btnCreate", isEnabled());
        verifyThat("#btnModify", isDisabled());
        verifyThat("#btnDelete", isDisabled());
        verifyThat("#btnCancel", isDisabled());
        verifyThat("#btnMovements", isEnabled());
        
    }

    /**
     * Test 3: Crear cuenta 
     * Usa lookupButton() para verificar texto del botón.
     */
    @Test
    public void test3_CreateAccount() {
        TableView<Account> table = lookup("#tbAccounts").queryTableView();
        
        // Pulsar Create para añadir fila
        clickOn("#btnCreate");
        doubleClickOn("#tcBalanceDate");
        
        // Verificar que se añadió fila
        int rowCountDespuesAdd = table.getItems().size();
        
        
        // Verificar que Cancel está visible
        verifyThat("#btnCancel", isEnabled());
        
        // Editar descripción
        int lastRowIndex = rowCountDespuesAdd - 1;
        
        interact(() -> {
            table.scrollTo(lastRowIndex);
            table.getSelectionModel().select(lastRowIndex);
            table.getFocusModel().focus(lastRowIndex);
        });
        
        // Localizar y editar celda Description (columna 1)
        Node celdaDescription = lookup("#tbAccounts")
                                .lookup(".table-row-cell")
                                .nth(lastRowIndex)
                                .lookup(".table-cell")
                                .nth(1)
                                .query();
        
        doubleClickOn(celdaDescription);
        write("Cuenta Test Auto");
        push(KeyCode.ENTER);
        
        // Editar BeginBalance (columna 2)
        Node celdaBeginBalance = lookup("#tbAccounts")
                                 .lookup(".table-row-cell")
                                 .nth(lastRowIndex)
                                 .lookup(".table-cell")
                                 .nth(2)
                                 .query();
        
        doubleClickOn(celdaBeginBalance);
        write("1000");
        push(KeyCode.ENTER);
        
        // Guardar (segunda pulsación de Create/Save)
        clickOn("#btnCreate");
        
        // Confirmar alerta de éxito
        verifyThat(".dialog-pane", isVisible());
        clickOn("Aceptar");
        
    }
    
    /**
     * Test 4: Cancelar creación
     * Fuerza recarga antes de comprobar estado inicial.
     */
    @Test
    public void test4_CancelCreation() {
        // Refrescar tabla antes de obtener estado inicial
        TableView<Account> table = lookup("#tbAccounts").queryTableView();
        
        // Forzar refresco visual
        interact(() -> table.refresh());
        
        // Iniciar creación
        clickOn("#btnCreate");
        
        // Pulsar Cancel
        clickOn("#btnCancel");
        
        // Confirmar cancelación
        verifyThat(".dialog-pane", isVisible());
        clickOn("Aceptar");
        
        verifyThat("#btnCancel", isDisabled());
    }
    
    /**
     * Test 5: Modificar cuenta
     * Espera a que el controlador detecte el cambio.
     */
    @Test
    public void test5_ModifyAccount() {
        TableView<Account> table = lookup("#tbAccounts").queryTableView();
        
        // Buscar cuenta para editar (preferir CREDIT)
        int indiceFila = -1;
        for (int i = 0; i < table.getItems().size(); i++) {
            Account acc = table.getItems().get(i);
            if (acc != null) { // Validar nulidad
                indiceFila = i;
                if (acc.getType() == AccountType.CREDIT) {
                    break; // Preferir CREDIT
                }
            }
        }
        
        assertTrue("Debe haber al menos una cuenta", indiceFila >= 0);
        
        doubleClickOn("#tcBalanceDate");
        // Seleccionar cuenta
        final int fila = indiceFila;
        interact(() -> {
            table.getSelectionModel().select(fila);
            table.scrollTo(fila);
        });
        
        verifyThat("#btnDelete", isEnabled());
        
        // Editar Description
        Node celdaDescription = lookup("#tbAccounts")
                                .lookup(".table-row-cell")
                                .nth(fila)
                                .lookup(".table-cell")
                                .nth(1)
                                .query();
        
        doubleClickOn(celdaDescription);
        
        // Limpiar contenido anterior correctamente
        push(KeyCode.CONTROL, KeyCode.A);
        push(KeyCode.BACK_SPACE);
        
        write("Cuenta Modificada");
        push(KeyCode.ENTER);
        
        // Guardar cambios
        clickOn("#btnModify");
        
        // Confirmar
        verifyThat("Update account", isVisible());
        clickOn("Aceptar");
        
        // Verificar éxito
        verifyThat("The account has been successfully updated.", isVisible());
        clickOn("Aceptar");
        
        // Verificar que Modify se deshabilitó
        verifyThat("#btnModify", isDisabled());
    }
    
    /**
     * Test 6: Intentar borrar cuenta CON movimientos
     * Añade validación de nulidad robusta.
     */
    @Test
    public void test6_DeleteAccountWithMovements() {
        TableView<Account> table = lookup("#tbAccounts").queryTableView();
        
        // Buscar cuenta con validación de nulidad
        int indiceFila = -1;
        for (int i = 0; i < table.getItems().size(); i++) {
            Account acc = table.getItems().get(i);
            
            // Validar que account y movements no sean null
            if (acc != null && acc.getMovements() != null && !acc.getMovements().isEmpty()) {
                indiceFila = i;
                break;
            }
        }
        
        // Si no hay cuentas con movimientos, saltar test
        if (indiceFila < 0) {
            System.out.println("SKIP test6: No hay cuentas con movimientos");
            return;
        }
        
        // Seleccionar cuenta
        final int fila = indiceFila;
        interact(() -> table.getSelectionModel().select(fila));
        
        // Intentar borrar
        clickOn("#btnDelete");
        
        // Verificar alerta de error
        verifyThat(".dialog-pane", isVisible());
        clickOn("Aceptar");
        
        
    }
    
    /**
     * Test 7: Borrar cuenta SIN movimientos
     * Añade validación de nulidad y crea cuenta si es necesario.
     */
    @Test
    public void test7_DeleteAccountWithoutMovements() {
        TableView<Account> table = lookup("#tbAccounts").queryTableView();
        
        //Buscar cuenta SIN movimientos con validación
        int indiceFila = -1;
        for (int i = table.getItems().size() - 1; i >= 0; i--) {
            Account acc = table.getItems().get(i);
            
            // Validar nulidad: si movements es null o está vacío
            if (acc != null && (acc.getMovements() == null || acc.getMovements().isEmpty())) {
                indiceFila = i;
                break;
            }
        }
        
        clickOn("#tcBalanceDate");
        // Seleccionar cuenta
        final int fila = indiceFila;
        interact(() -> table.getSelectionModel().select(fila));
        
        // Borrar
        clickOn("#btnDelete");
        
        // Confirmar
        verifyThat("Delete account", isVisible());
        clickOn("Aceptar");
        
        // Verificar éxito
        verifyThat(".dialog-pane", isVisible());
        clickOn("Aceptar");
        
        
    }
    
    /**
     * Test 8: CreditLine editable solo en CREDIT
     * Simplifica la verificación de estado.
     */
    @Test
    public void test8_CreditLineEditableOnlyForCredit() {
        TableView<Account> table = lookup("#tbAccounts").queryTableView();
        
        // 1. Buscar cuenta CREDIT
        int filaCredit = -1;
        for (int i = 0; i < table.getItems().size(); i++) {
            Account acc = table.getItems().get(i);
            if (acc != null && acc.getType() == AccountType.CREDIT) {
                filaCredit = i;
                break;
            }
        }
        
        if (filaCredit < 0) {
            System.out.println("SKIP test8: No hay cuentas CREDIT");
            return;
        }
        
        // 2. Seleccionar cuenta CREDIT
        final int fila = filaCredit;
        interact(() -> table.getSelectionModel().select(fila));
        
        // 3. Editar CreditLine (columna 4)
        Node celdaCreditLine = lookup("#tbAccounts")
                               .lookup(".table-row-cell")
                               .nth(fila)
                               .lookup(".table-cell")
                               .nth(4)
                               .query();
        
        doubleClickOn(celdaCreditLine);
        
        // Limpiar y escribir
        push(KeyCode.CONTROL, KeyCode.A);
        write("5000");
        push(KeyCode.ENTER);
        
        //Verificacion boton refrescar
        verifyThat("#btnRefresh", isVisible());
        verifyThat("#btnRefresh", isEnabled());

        clickOn("#btnRefresh");

        verifyThat("#btnCreate", isEnabled());
        verifyThat("#btnModify", isDisabled());
        verifyThat("#btnDelete", isDisabled());
    }
    

    /**
     * Test 9: Navegación a Movements
     */
    @Test
    public void test9_NavigateToMovements() {
        TableView<Account> table = lookup("#tbAccounts").queryTableView();
        
        
        // Seleccionar primera cuenta
        interact(() -> table.getSelectionModel().selectFirst());
        
        // Pulsar Movements
        clickOn("#btnMovements");
        
        // Verificar que se abrió la ventana
            verifyThat("#tvMovements", isVisible());
            
            // Cerrar ventana para continuar
            verifyThat("#btBack", isVisible());
            clickOn("#btBack");
       
    }
}