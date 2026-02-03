package UI;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import model.Account;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.testfx.framework.junit.ApplicationTest;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isDisabled;
import static org.testfx.matcher.base.NodeMatchers.isEnabled;
import static org.testfx.matcher.base.NodeMatchers.isVisible;
import static org.testfx.matcher.control.TextInputControlMatchers.hasText;
import static org.testfx.matcher.control.TableViewMatchers.hasItems;
import signup.signin.SignUpSignIn;

/**
 * Clase de prueba para la vista de Cuentas (AccountsController).
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AccountsControllerTest extends ApplicationTest {

    // Referencias a los IDs del FXML (Ajústalos si tus IDs son diferentes)
    private static final String TABLE_VIEW_ID = "#tbAccounts";
    private static final String BTN_CREATE_ID = "#btnCreate"; // Botón de crear
    private static final String BTN_CANCEL_ID = "#btnCancel"; // Botón de crear
    private static final String BTN_MODIFY_ID = "#btnModify"; // Botón de modificar
    private static final String BTN_DELETE_ID = "#btnDelete"; // Botón de eliminar
    private static final String BTN_MOVEMENTS = "#btnMovements";
    private static final String BTN_LOGOUT_ID = "#btnLogOut"; // Botón o menú de salir (Back)

    /**
     * Inicio de la aplicación para el entorno de pruebas.
     */
    @Override
    public void start(Stage stage) throws Exception {
        new SignUpSignIn().start(stage);
    }

    @Test
   public void test1_Login() {
       clickOn("#EmailTextField");
       write("awallace@gmail.com");
       clickOn("#PasswordField");
       write("qwerty*9876"); 

       verifyThat("#LoginButton", isEnabled());
       clickOn("#LoginButton");
    }
   
    /**
     * Test 2: Verificar el estado inicial de la ventana.
     * La tabla debe tener datos (si el servidor tiene datos) y los botones deben estar habilitados.
     */
    @Test
    public void test2_InitialState() {
        // Verificar que la tabla es visible
        verifyThat(TABLE_VIEW_ID, isVisible());
        
        // Verificar que los botones principales están habilitados
        verifyThat(BTN_CREATE_ID, isEnabled());
        verifyThat(BTN_MODIFY_ID, isDisabled());
        verifyThat(BTN_DELETE_ID, isDisabled());
        verifyThat(BTN_CANCEL_ID, isDisabled());
        verifyThat(BTN_MOVEMENTS, isEnabled());
    }

    /**
     * Test 2: Prueba de creación de una cuenta nueva.
     */
    @Test
    public void test2_CreatingAccount() {
        // Si la creación es mediante una nueva fila en la tabla editable:
        // Verificamos que el número de items en la tabla ha aumentado o verificamos
        // que aparezca un mensaje de éxito.
        
        // Si hay una alerta de confirmación o éxito, la cerramos pulsando ENTER
        // push(KeyCode.ENTER); 
        // Obtenemos el número inicial de cuentas en la tabla
        int rowCount = lookup(TABLE_VIEW_ID).queryTableView().getItems().size();

        clickOn(BTN_CREATE_ID); // Entrar en modo creación
        verifyThat(BTN_CREATE_ID, hasText("Save"));
        verifyThat(BTN_CANCEL_ID, isEnabled());

        // Simulamos la edición de la descripción en la nueva celda
        clickOn("Description");
        write("New Account TEST");
        press(KeyCode.ENTER);
        release(KeyCode.ENTER);

        clickOn(BTN_CREATE_ID); // Pulsar Confirmar

        // Verificamos el mensaje de éxito y el refresco de la tabla
        verifyThat("Cuenta Creada", isVisible());
        clickOn("Aceptar");

        // Comprobamos que el recuento de filas ha aumentado en 1
        assertEquals("La tabla debería tener una fila más tras la creación",
                rowCount + 1, lookup(TABLE_VIEW_ID).queryTableView().getItems().size());
        }
    }
//
//    /**
//     * Test 3: Prueba de edición en línea (TableView Editable).
//     * Intenta modificar la descripción de una cuenta.
//     */
//    @Test
//    public void test3_EditAccountDescription() {
//        // Seleccionar la primera fila de la tabla
//        clickOn(TABLE_VIEW_ID).type(KeyCode.HOME); // Ir al inicio
//        
//        // Hacemos doble clic en la celda de descripción (asumiendo que es la segunda columna)
//        // Mover el ratón un poco a la derecha para acertar en la columna Descripción
//        moveBy(50, 0); 
//        doubleClickOn(); // Doble clic para activar edición
//
//        // Escribir nuevo texto
//        write("Cuenta TestFX");
//        push(KeyCode.ENTER); // Confirmar edición
//
//        // Aquí podríamos verificar en el modelo si se actualizó, 
//        // pero visualmente basta con que no haya error.
//    }
//
//    /**
//     * Test 4: Prueba de borrado de cuenta.
//     * Selecciona una cuenta y pulsa eliminar, gestionando la alerta.
//     */
//    @Test
//    public void test4_DeleteAccount() {
//        // Seleccionar un item en la tabla (el último para evitar borrar datos críticos)
//        clickOn(TABLE_VIEW_ID).type(KeyCode.END); 
//        
//        // Hacer clic en borrar
//        clickOn(BTN_DELETE_ID);
//
//        // Verificar que aparece una alerta de confirmación (Alert)
//        // TestFX detecta la ventana modal automáticamente.
//        verifyThat("¿Está seguro de que desea borrar la cuenta seleccionada?", isVisible()); // Texto aproximado
//
//        // Confirmar el borrado (OK)
//        clickOn("Aceptar"); // O el texto que tenga el botón OK en tu idioma local (OK/Sí)
//        
//        // Opcional: Cerrar alerta de éxito si aparece
//        // push(KeyCode.ENTER);
//    }
//    
//    /**
//     * Test 5: Navegación (Salir/Logout).
//     */
//    @Test
//    public void test5_LogOut() {
//        // Clic en el botón de salir
//        clickOn(BTN_LOGOUT_ID);
//        
//        // Verificar alerta de confirmación de cierre de sesión
//        verifyThat("¿Está seguro de que desea cerrar sesión y volver al login?", isVisible());
//        
//        // Cancelar para no cerrar el stage del test abruptamente si no es el último test
//        clickOn("Cancelar");
//    }
//}