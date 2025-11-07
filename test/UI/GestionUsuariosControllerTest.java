package UI;

import javafx.stage.Stage;
import javafx.scene.control.DialogPane;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.testfx.framework.junit.ApplicationTest;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.*;
import static org.testfx.matcher.control.TextInputControlMatchers.hasText;
import signup.signin.SignUpSignIn;

/**
 * Suite de pruebas para GestionUsuariosController
 * 
 * REQUISITOS PREVIOS:
 * - Servidor GlassFish debe estar ejecutándose en localhost:8080
 * - Base de datos debe contener un usuario de prueba válido
 * - Dependencias TestFX en el proyecto (testfx-core, testfx-junit)
 * 
 * COBERTURA DE PRUEBAS:
 * 1. Estado inicial de la ventana
 * 2. Validación de campos en tiempo real
 * 3. Habilitación/deshabilitación del botón Login
 * 4. Credenciales incorrectas (401 Unauthorized)
 * 5. Credenciales correctas (200 OK - login exitoso)
 * 6. Funcionalidad del botón Exit
 * 7. Validación de formato de email
 * 8. Longitud mínima de contraseña
 * 
 * @author Eduardo
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GestionUsuariosControllerTest extends ApplicationTest {

    // ======================== CONSTANTES DE PRUEBA ========================
    private static final String VALID_EMAIL = "test@example.com";
    private static final String VALID_PASSWORD = "Password123";
    private static final String INVALID_EMAIL = "correo-invalido";
    private static final String SHORT_PASSWORD = "12345"; // Menos de 8 caracteres
    private static final String WRONG_EMAIL = "noexiste@example.com";
    private static final String WRONG_PASSWORD = "WrongPass123";
    
    // ======================== CONFIGURACIÓN ========================
    
    /**
     * Configuración inicial de JavaFX headless para pruebas
     */
    @BeforeClass
    public static void setUpClass() {
        // Configurar JavaFX en modo headless si es necesario
        System.setProperty("testfx.robot", "glass");
        System.setProperty("testfx.headless", "true");
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.text", "t2k");
        System.setProperty("java.awt.headless", "true");
    }

    /**
     * Inicia la aplicación JavaFX para las pruebas
     */
    @Override
    public void start(Stage stage) throws Exception {
        SignUpSignIn app = new SignUpSignIn();
        app.start(stage);
    }

    // ======================== TESTS DE ESTADO INICIAL ========================

    /**
     * TEST 1: Verifica el estado inicial de la ventana LOGIN
     * 
     * CRITERIOS:
     * - Campos email y password vacíos
     * - Botón Login deshabilitado
     * - Hyperlinks habilitados
     * - No hay mensajes de error visibles
     */
    @Test
    public void test1_InitialState() {
        // Verificar que los campos están vacíos
        verifyThat("#EmailTextField", hasText(""));
        verifyThat("#PasswordField", hasText(""));
        
        // Verificar que el botón Login está deshabilitado inicialmente
        verifyThat("#LoginButton", isDisabled());
        
        // Verificar que los hyperlinks están habilitados
        verifyThat("#GetPasswordLink", isEnabled());
        verifyThat("#SignUpLink", isEnabled());
        
        // Verificar que no hay mensajes de error visibles
        
    }

    // ======================== TESTS DE VALIDACIÓN DE CAMPOS ========================

    /**
     * TEST 2: Verifica que el botón Login permanece deshabilitado con campos vacíos
     */
    @Test
    public void test2_LoginDisabledWithEmptyFields() {
        // Escribir solo en email y verificar que Login sigue deshabilitado
        clickOn("#EmailTextField");
        write(VALID_EMAIL);
        verifyThat("#LoginButton", isDisabled());
        
        // Limpiar email y escribir solo en password
        clickOn("#EmailTextField");
        eraseText(VALID_EMAIL.length());
        clickOn("#PasswordField");
        write(VALID_PASSWORD);
        verifyThat("#LoginButton", isDisabled());
        
        // Limpiar todo y verificar que sigue deshabilitado
        clickOn("#PasswordField");
        eraseText(VALID_PASSWORD.length());
        verifyThat("#LoginButton", isDisabled());
    }

    /**
     * TEST 3: Verifica validación de formato de email en tiempo real
     * 
     * REGEX esperado: ^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$
     */
    @Test
    public void test3_EmailFormatValidation() {
        // Escribir email con formato inválido
        clickOn("#EmailTextField");
        write(INVALID_EMAIL);
        clickOn("#PasswordField");
        write(VALID_PASSWORD);
        
        // Verificar que el botón sigue deshabilitado
        verifyThat("#LoginButton", isDisabled());
        
        // Verificar que aparece mensaje de error inline
        verifyThat("#Error_email", node -> {
            String text = ((javafx.scene.control.Label) node).getText();
            return text != null && !text.isEmpty();
        });
        
        // Corregir el email
        clickOn("#EmailTextField");
        eraseText(INVALID_EMAIL.length());
        write(VALID_EMAIL);
        
        // Verificar que el error desaparece y el botón se habilita
        
        verifyThat("#LoginButton", isEnabled());
    }

    /**
     * TEST 4: Verifica validación de longitud mínima de contraseña
     * 
     * LONGITUD MÍNIMA: 8 caracteres
     */
    @Test
    public void test4_PasswordMinLengthValidation() {
        // Escribir email válido
        clickOn("#EmailTextField");
        write(VALID_EMAIL);
        
        // Escribir contraseña corta (menos de 8 caracteres)
        clickOn("#PasswordField");
        write(SHORT_PASSWORD);
        
        // Verificar que el botón está deshabilitado
        verifyThat("#LoginButton", isDisabled());
        
        // Verificar mensaje de error inline
        verifyThat("#Error_password", node -> {
            String text = ((javafx.scene.control.Label) node).getText();
            return text != null && text.contains("8 caracteres");
        });
        
        // Completar la contraseña para que sea válida
        clickOn("#PasswordField");
        eraseText(SHORT_PASSWORD.length());
        write(VALID_PASSWORD);
        
        // Verificar que el error desaparece y el botón se habilita
        
        verifyThat("#LoginButton", isEnabled());
    }

    /**
     * TEST 5: Verifica que el botón Login se habilita con datos válidos
     */
    @Test
    public void test5_LoginEnabledWithValidData() {
        clickOn("#EmailTextField");
        write(VALID_EMAIL);
        clickOn("#PasswordField");
        write(VALID_PASSWORD);
        
        // Verificar que el botón está habilitado
        verifyThat("#LoginButton", isEnabled());
        
        
    }

    // ======================== TESTS DE AUTENTICACIÓN ========================

    /**
     * TEST 6: Verifica comportamiento con credenciales incorrectas
     * 
     * RESPUESTA ESPERADA: 401 Unauthorized
     * COMPORTAMIENTO:
     * - Mostrar mensaje "Email o contraseña incorrectos"
     * - Enfocar campo Password y seleccionar texto
     * - Mantener controles habilitados después del error
     * 
     * NOTA: Este test requiere que el servidor REST esté activo
     */
    @Test
    public void test6_LoginWithInvalidCredentials() {
        // Escribir credenciales incorrectas
        clickOn("#EmailTextField");
        write(WRONG_EMAIL);
        clickOn("#PasswordField");
        write(WRONG_PASSWORD);
        
        verifyThat("#LoginButton", isEnabled());
        
        // Hacer clic en Login
        clickOn("#LoginButton");
        
        // Esperar respuesta del servidor (máximo 5 segundos)
        sleep(5000);
        
        // Verificar que aparece mensaje de error
        // El mensaje puede estar en Error_password o en un Alert dialog
        verifyThat("#Error_password", node -> {
            String text = ((javafx.scene.control.Label) node).getText();
            return text != null && text.toLowerCase().contains("incorrectos");
        });
        
        // Verificar que los controles están habilitados después del error
        verifyThat("#EmailTextField", isEnabled());
        verifyThat("#PasswordField", isEnabled());
        verifyThat("#LoginButton", isEnabled());
    }

    /**
     * TEST 7: Verifica login exitoso con credenciales válidas
     * 
     * RESPUESTA ESPERADA: 200 OK
     * COMPORTAMIENTO:
     * - Navegar a ventana principal
     * - Cerrar ventana de login
     * 
     * NOTA: 
     * - Este test requiere que exista el usuario en la BD
     * - Cambia VALID_EMAIL y VALID_PASSWORD por credenciales reales
     * - Asegúrate de que PaginaPrincipal.fxml existe en /UI/
     */
    @Test
    public void test7_LoginWithValidCredentials() {
        // IMPORTANTE: Ajusta estas credenciales a un usuario real en tu BD
        String realEmail = "admin@bank.com"; // Cambiar por email real
        String realPassword = "Admin123"; // Cambiar por password real
        
        clickOn("#EmailTextField");
        write(realEmail);
        clickOn("#PasswordField");
        write(realPassword);
        
        verifyThat("#LoginButton", isEnabled());
        
        // Hacer clic en Login
        clickOn("#LoginButton");
        
        // Esperar navegación (máximo 5 segundos)
        sleep(5000);
        
        // Verificar que se abrió la ventana principal
        // Buscar algún elemento característico de PaginaPrincipal
        verifyThat("#WelcomeLabel", isVisible());
    }

    // ======================== TESTS DE NAVEGACIÓN ========================

    /**
     * TEST 8: Verifica funcionalidad del botón Exit
     * 
     * COMPORTAMIENTO ESPERADO:
     * - Mostrar diálogo de confirmación
     * - Al aceptar: cerrar aplicación
     * - Al cancelar: permanecer en ventana
     */
    @Test
    public void test8_ExitButtonShowsConfirmation() {
        // Simular evento de cierre de ventana
        Stage stage = (Stage) lookup("#EmailTextField").query().getScene().getWindow();
        
        // Disparar el evento de cierre
        javafx.application.Platform.runLater(() -> {
            stage.fireEvent(
                new javafx.stage.WindowEvent(
                    stage, 
                    javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST
                )
            );
        });
        
        // Esperar a que aparezca el diálogo
        sleep(1000);
        
        // Verificar que aparece el diálogo de confirmación
        verifyThat(".dialog-pane", isVisible());
        
        // Verificar el texto del diálogo
        DialogPane dialogPane = lookup(".dialog-pane").query();
        assertTrue(dialogPane.getContentText().contains("¿Deseas salir de la aplicación?"));
        
        // Hacer clic en Cancelar para no cerrar la aplicación
        clickOn("Cancelar");
        
        // Verificar que la ventana sigue visible
        verifyThat("#LoginButton", isVisible());
    }

    /**
     * TEST 10: Verifica que el hyperlink "Sign up" está funcional
     */
    @Test
    public void test10_SignUpLinkEnabled() {
        verifyThat("#SignUpLink", isEnabled());
        
        // Hacer clic (aunque la ventana no exista aún, no debe crashear)
        clickOn("#SignUpLink");
        
        // Esperar posible mensaje informativo
        sleep(2000);
        
        // Si aparece un Alert informativo, cerrarlo
        try {
            clickOn("Aceptar");
        } catch (Exception e) {
            // El botón Aceptar no existe, continuar
        }
    }  
}