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
import org.junit.Ignore;
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
     * 
     * Test 1 y test 2 fusionados para que el test 2 no dependa del primero 
     */
    @Test
    @Ignore
    public void test1_Login() {
        clickOn("#EmailTextField");
        write("awallace@gmail.com");
        
        clickOn("#PasswordField");
        write("qwerty*9876");
        
        verifyThat("#LoginButton", isEnabled());
        clickOn("#LoginButton");
        
       
        // Estado inicial de la ventana
        verifyThat("#tbAccounts", isVisible());
        
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
     * Test 2: Crear cuenta 
     * Uso lookupButton() para verificar texto del botón.
     */
    @Test
    @Ignore
    public void test2_CreateAccount() {
        TableView<Account> table = lookup("#tbAccounts").queryTableView();
        int rowsBefore = table.getItems().size();

        // Pulsar Create para añadir fila
        clickOn("#btnCreate");
        
        // Verificar que se añadió fila
        int rowsAfter = table.getItems().size();
        assertEquals("La tabla debería tener una fila más", rowsBefore + 1, rowsAfter);
        
        // Verificar que Cancel está visible
        verifyThat("#btnCancel", isEnabled());

        // Preparar índices para seleccionar la nueva fila (la última)
        int lastRowIndex = rowsAfter - 1;

        interact(() -> {
                table.scrollTo(lastRowIndex);
                table.getSelectionModel().clearSelection();
                table.getSelectionModel().select(lastRowIndex);
                table.getFocusModel().focus(lastRowIndex);
        });
        
        

        // Localizar y editar celda Description (columna 1)
        Node celdaDescription = lookup("#tbAccounts")
                                .lookup(".table-row-cell")
                                .lookup(".table-cell")
                                .nth(1)
                                .query();

        doubleClickOn(celdaDescription);
        push(KeyCode.CONTROL, KeyCode.A);
        write("Cuenta Test Auto");
        push(KeyCode.ENTER);

        // Editar BeginBalance (columna 2)
        Node celdaBeginBalance = lookup("#tbAccounts")
                                 .lookup(".table-row-cell")
                                 .lookup(".table-cell")
                                 .nth(2)
                                 .query();
        
        doubleClickOn(celdaBeginBalance);
        write("1000");
        push(KeyCode.ENTER);
        
        // Editar linea de credito
        // Solo editable si es CREDIT
        interact(() -> {
                Account nuevaCuenta = table.getItems().get(lastRowIndex);
                nuevaCuenta.setCreditLine(2000.0);
                table.refresh();
            });
        
        // Guardar (segunda pulsación de Create/Save)
        clickOn("#btnCreate");

        // Confirmar alerta de éxito
        verifyThat(".dialog-pane", isVisible());
        clickOn("Aceptar");
        
        clickOn("#tcBalanceDate");
        
        
        //FIXME Verificaciones insuficientes: verificar que hay un nuevo objeto Account en
        //FIXME los items de la tabla con los datos introducidos al crear: Tipo, descripción, saldo inicial y 
        //FIXME línea de crédito(si tipo es crédito, que es el tipo de cuenta más complejo y que habría que testear).
        
        Account createdAccount = table.getItems().get(lastRowIndex);
        assertEquals("El tipo de cuenta no coincide", 
                     AccountType.CREDIT, createdAccount.getType());
                     
        assertEquals("La descripción no se guardó correctamente", 
                     "Cuenta Test Auto", createdAccount.getDescription());
                     
        assertEquals("El saldo inicial no es correcto", 
                     1000.0, createdAccount.getBalance(), 0.01);
                     
        assertEquals("La línea de crédito no se guardó correctamente", 
                     2000.0, createdAccount.getCreditLine(), 0.01);
    }
        
    /**
     * Test 3: Cancelar creación
     * Fuerza recarga antes de comprobar estado inicial.
     */
    @Test
    @Ignore
    public void test3_CancelCreation() {
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
        
        clickOn("#tcBalanceDate");
    }
    
    /**
     * Test 4: Modificar cuenta
     * Espera a que el controlador detecte el cambio.
     */
    @Test
    @Ignore
    public void test4_ModifyAccount() {
        TableView<Account> table = lookup("#tbAccounts").queryTableView();
        
        // Buscar cuenta para editar (preferir CREDIT)
        int indiceFila = -1;
        Account cuentaSeleccionada = null;
       
        for (int i = 0; i < table.getItems().size(); i++) {
            Account acc = table.getItems().get(i);
            if (acc != null && acc.getType() == AccountType.CREDIT) { // Validar nulidad y credito
                // Guardamos la primera que encontremos por defecto
                if (indiceFila == -1) {
                    indiceFila = i;
                    cuentaSeleccionada = acc;
                   
                }
                if (acc.getType() == AccountType.CREDIT) {
                    indiceFila = i;
                    cuentaSeleccionada = acc;
                    break; // Preferir CREDIT
                }
                if (indiceFila < 0) {
                    System.out.println("SKIP test5: No hay cuentas CREDIT");
                    return;
                }
            }
        }
        
        assertTrue("Debe haber al menos una cuenta de credito", indiceFila >= 0);
        
        // Variables finales para uso en clases anónimas
        final int filaFinal = indiceFila;
        final Account cuentaFinal = cuentaSeleccionada;
        
        // Seleccionar cuenta
        interact(() -> {
            table.getSelectionModel().clearSelection();
                table.getSelectionModel().select(filaFinal);
                table.scrollTo(filaFinal);
                table.getFocusModel().focus(filaFinal);
        });
        
        verifyThat("#btnDelete", isEnabled());
        
        // Editar Description
        Node celdaDescription = lookup("#tbAccounts")
                                .lookup(".table-row-cell")
                                .nth(filaFinal)
                                .lookup(".table-cell")
                                .nth(1) 
                                .query();
        
        doubleClickOn(celdaDescription);
        
        // Limpiar contenido anterior correctamente
        push(KeyCode.CONTROL, KeyCode.A);
        push(KeyCode.BACK_SPACE);
        
        write("Cuenta Modificada");
        push(KeyCode.ENTER);
        
        // Editar CreditLine (columna 4)
        Node celdaCreditLine = lookup("#tbAccounts")
                               .lookup(".table-row-cell")
                               .nth(filaFinal)
                               .lookup(".table-cell")
                               .nth(4)
                               .query();
        
        doubleClickOn(celdaCreditLine);
        
        // Limpiar y escribir
        push(KeyCode.CONTROL, KeyCode.A);
        write("5000");
        push(KeyCode.ENTER);

        // Guardar cambios
        clickOn("#btnModify");
        
        // Confirmar
        verifyThat("Update account", isVisible());
        clickOn("Aceptar");
        
        // Verificar éxito
        verifyThat("The account has been successfully updated.", isVisible());
        clickOn("Aceptar");
        
        //FIXME Verificaciones insuficientes: verificar que hay un objeto Account en
        //FIXME los items de la tabla con los datos introducidos al modificar: descripción y 
        //FIXME línea de crédito(si tipo es crédito, que es el tipo de cuenta más complejo y que habría que testear).
        
        // Obtenemos el objeto actualizado directamente de la tabla
        Account cuentaActualizada = table.getItems().get(filaFinal);
        
        // Verificar Descripción
        assertEquals("La descripción debería haber cambiado", 
                     "Cuenta Modificada", 
                     cuentaActualizada.getDescription());
        
        // Verificar Línea de Crédito
        if (cuentaActualizada.getType() == AccountType.CREDIT) {
            assertEquals("La línea de crédito debería haber cambiado", 
                         5000, 
                         cuentaActualizada.getCreditLine(), 
                         0.01); 
        }
        
        // Verificar estado del botón
        verifyThat("#btnModify", isDisabled());
    }
    
    /**
     * Test 5: Intentar borrar cuenta CON movimientos
     * Añade validación de nulidad robusta.
     */
    @Test
    @Ignore
    public void test5_DeleteAccountWithMovements() {
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
     * Test 6: Borrar cuenta SIN movimientos
     * Añade validación de nulidad y crea cuenta si es necesario.
     */
    @Test
    @Ignore
    public void test6_DeleteAccountWithoutMovements() {
        TableView<Account> table = lookup("#tbAccounts").queryTableView();
        
        //Buscar cuenta SIN movimientos con validación
        int indiceFila = -1;
        Account cuentaEliminada = null;
        for (int i = table.getItems().size() - 1; i >= 0; i--) {
            Account acc = table.getItems().get(i);
            
            // Validar nulidad: si movements es null o está vacío
            if (acc != null && (acc.getMovements() == null || acc.getMovements().isEmpty())) {
                indiceFila = i;
                cuentaEliminada = acc;
                break;
            }
        }
        assertTrue("No hay cuentas sin movimientos para probar el borrado", indiceFila >= 0);
        
        // Seleccionar cuenta
        final int fila = indiceFila;
        final Account deletedAccount = cuentaEliminada;
        
        interact(() -> {
                table.getSelectionModel().select(fila);
                table.getSelectionModel().clearSelection();
                table.getSelectionModel().select(fila);
                table.scrollTo(fila);
                table.getFocusModel().focus(fila);
                
        });
        
        verifyThat("#btnDelete", isEnabled());
        // Borrar
        clickOn("#btnDelete");
        
        // Confirmar
        verifyThat("Delete account", isVisible());
        clickOn("Aceptar");
        
        // Verificar éxito
        verifyThat(".dialog-pane", isVisible());
        clickOn("Aceptar");
        
        //FIXME Verificaciones insuficientes: verificar que el objeto Account seleccionado para borrar
        //FIXME ya no está entre los items de la tabla.

        // Verificar que la cuenta ya no está en la tabla
        boolean bool = false;
        for (Account acc : table.getItems()) {
            if (acc.equals(deletedAccount)) {
                bool = true;
                break;
            }
        }
        
        assertFalse("La cuenta borrada debería haber desaparecido de la tabla", bool); 
    }
    
    /**
     * Test 7: Navegación a Movements
     */
    @Test
    @Ignore
    public void test7_NavigateToMovements() {
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
            
        // Verificacion boton refrescar
        verifyThat("#btnRefresh", isVisible());
        verifyThat("#btnRefresh", isEnabled());

        clickOn("#btnRefresh");
        
        verifyThat("#btnCreate", isEnabled());
        verifyThat("#btnModify", isDisabled());
        verifyThat("#btnDelete", isDisabled());
       
    }
}