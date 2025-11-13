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
        write("Nisa");
        
        clickOn("#tfMName");
        write("A.");
        
        clickOn("#tfLName");
        write("Abyss");
        
        clickOn("#tfAddress");
        write("67 street number 67"); 
        
        clickOn("#tfCity");
        write("Istanbul"); 
        
        clickOn("#tfState");
        write("TR");
        
        clickOn("#tfZip");
        write("34000"); 
        
        clickOn("#tfPhone");
        write("905551234567"); 
        
        clickOn("#tfEmail");
        String uniqueEmail = "nisa.abyss." + System.currentTimeMillis() + "@gmail.com";
        write(uniqueEmail);
        
        clickOn("#tfPass");
        write("Abcd!1234"); 
        
        clickOn("#tfRPass");
        write("Abcd!1234");
        
        verifyThat("#btCreate", isEnabled());
        
        clickOn("#btCreate");
        
        sleep(2000);
        
        verifyThat(".dialog-pane", isVisible());
        
        clickOn("Aceptar");
        sleep(1000);
        
        verifyThat("#EmailTextField", isVisible());
        verifyThat("#LoginButton", isVisible());
    }
  
    @Test
    public void test3_EmailDuplicado() {
        clickOn("#SignUpLink");
        sleep(1000);
        
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