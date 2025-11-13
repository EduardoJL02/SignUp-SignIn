package UI;

import javafx.stage.Stage;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import static org.testfx.api.FxAssert.verifyThat;
import org.testfx.framework.junit.ApplicationTest;
import static org.testfx.matcher.base.NodeMatchers.isDisabled;
import static org.testfx.matcher.base.NodeMatchers.isEnabled;
import static org.testfx.matcher.base.NodeMatchers.isFocused;
import static org.testfx.matcher.base.NodeMatchers.isVisible;
import signup.signin.SignUpSignIn;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GestionUsuariosControllerSignUpTest extends ApplicationTest {
    
    @Override
    public void start(Stage stage) throws Exception {
        new SignUpSignIn().start(stage);
    }
    
    private void clearFormFields() {
        clickOn("#tfFName").eraseText(40); 
        clickOn("#tfMName").eraseText(2);
        clickOn("#tfLName").eraseText(40);
        clickOn("#tfAddress").eraseText(50);
        clickOn("#tfCity").eraseText(50);
        clickOn("#tfState").eraseText(20);
        clickOn("#tfZip").eraseText(5);
        clickOn("#tfPhone").eraseText(15);
        clickOn("#tfEmail").eraseText(50);
        clickOn("#tfPass").eraseText(15);
        clickOn("#tfRPass").eraseText(15);
    }
    
    
    @Test
    public void test0_SignUpAcces(){
        verifyThat("#SignUpLink", isEnabled());
        clickOn("#SignUpLink");
        
    }

    @Test
    public void test1_InitialTests() {
        verifyThat("#SignUpLink", isEnabled());
        verifyThat("#LoginButton", isDisabled());
        
        clickOn("#SignUpLink");
        sleep(1000); 
        
        verifyThat("#btCreate", isDisabled());
        verifyThat("#tfFName", isFocused());
        
        clickOn("#btBack");
        sleep(500);
    }
    
    @Test
    public void test2_SignUpExitoso() {
        clickOn("#SignUpLink");
        sleep(1000);
        clickOn("#tfFName");
        write("Jorge");
        
        clickOn("#tfMName");
        write("G.");
        
        clickOn("#tfLName");
        write("Linares");
        
        clickOn("#tfAddress");
        write("Calle Contubernio 45"); 
        
        clickOn("#tfCity");
        write("Istanbul"); 
        
        clickOn("#tfState");
        write("TR");
        
        clickOn("#tfZip");
        write("34000"); 
        
        clickOn("#tfPhone");
        write("905551234567"); 
        
        clickOn("#tfEmail");
        String uniqueEmail = "jorge.gonzalez." + System.currentTimeMillis() + "@gmail.com";
        write(uniqueEmail);
        
        clickOn("#tfPass");
        write("Abcd!1234"); 
        
        clickOn("#tfRPass");
        write("Abcd!1234");
        
        verifyThat("#btCreate", isEnabled());
        
        clickOn("#btCreate");
        sleep(1000);
        verifyThat(".dialog-pane", isVisible());
        clickOn("Aceptar");
        sleep(1000);
    }
  
    @Test
    public void test3_UsuarioExiste(){
        clearFormFields();
        clickOn("#tfFName");
        write("Pablo");
        
        clickOn("#tfMName");
        write("R.");
        
        clickOn("#tfLName");
        write("Rodriguez");
        
        clickOn("#tfAddress");
        write("C/ Arcipreste de Hita, 5");
        
        clickOn("#tfCity");
        write("Madrid");
        
        clickOn("#tfState");
        write("MD");
        
        clickOn("#tfZip");
        write("28220");
        
        clickOn("#tfPhone");
        write("692879385");
        
        clickOn("#tfEmail");
       
        write("rodrguezdelgado.pablo@gmail.com");
        
        clickOn("#tfPass");
        write("PabloRodriguez!1996");
        
        clickOn("#tfRPass");
        write("PabloRodriguez!1996");
        
        verifyThat("#btCreate", isEnabled());
        
        clickOn("#btCreate");
        
        sleep(2000);
        
        verifyThat(".dialog-pane", isVisible());
        
        clickOn("Aceptar");
        sleep(500);
        
        verifyThat("#tfFName", isVisible());
        verifyThat("#btCreate", isVisible());
        verifyThat("#btBack", isVisible());
        
        clickOn("#btBack");
        sleep(500);
        
        verifyThat("#EmailTextField", isVisible());
    }
}
