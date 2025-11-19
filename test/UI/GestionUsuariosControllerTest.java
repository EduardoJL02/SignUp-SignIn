package UI;

import static org.testfx.matcher.base.NodeMatchers.isDisabled; 
import static org.testfx.matcher.base.NodeMatchers.isEnabled; 
import static org.testfx.matcher.base.NodeMatchers.isVisible; 
import javafx.stage.Stage;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.runners.MethodSorters;
import org.testfx.framework.junit.ApplicationTest;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.control.TextInputControlMatchers.hasText;
import signup.signin.SignUpSignIn;

@FixMethodOrder(MethodSorters.NAME_ASCENDING) public class GestionUsuariosControllerTest extends ApplicationTest{
    @Override
    public void start(Stage stage) throws Exception {
        new SignUpSignIn().start(stage);
    }

    @Test
    @Ignore
    public void test1_InitialState() {
        verifyThat("#EmailTextField", hasText(""));
        verifyThat("#PasswordField",hasText(""));
        verifyThat("#LoginButton", isDisabled());
    }

    @Ignore
    @Test
    public void test2_LoginisDisabled(){
        clickOn("#EmailTextField");
        write("usuario");
        verifyThat("#LoginButton", isDisabled());
        eraseText(7);
        clickOn("#PasswordField");
        write("password");
        verifyThat("#LoginButton", isDisabled());
        eraseText(8);
        verifyThat("#LoginButton", isDisabled());
    }

    @Ignore
    @Test
    public void test3_LoginisEnabled(){
        clickOn("#EmailTextField");
        write("username@gmail.com");
        clickOn("#PasswordField");
        write("password");
        verifyThat("#LoginButton", isEnabled());
    }
    
    @Test
    public void test4_NotAuthorizedException() {
        clickOn("#EmailTextField");
        write("eduardo@gmail.com");
        clickOn("#PasswordField");
        write("qwerty*9876");
        verifyThat("#LoginButton", isEnabled());
        
        clickOn("#LoginButton");
        sleep(1000);
        verifyThat("Incorrect email or password.", isVisible());
 
    }

    @Test
public void test5_NotAuthorizedException() {
        clickOn("#EmailTextField");
        write("awallace@gmail.com");
        clickOn("#PasswordField");
        write("awallace*1234");
        verifyThat("#LoginButton", isEnabled());
        
        clickOn("#LoginButton");
        sleep(1000);
        verifyThat("Incorrect email or password.", isVisible());
 
    }    

    @Ignore
    @Test
    public void test5_RegisterisEnabled() {
        clickOn("#SignUpLink");
        
        sleep(1000);
        
        try {
            verifyThat("#tfFName", isVisible());
            verifyThat("#btCreate", isVisible());
            verifyThat("#btBack", isVisible());
            
            clickOn("#btBack");
                      
            sleep(1000);
            
            verifyThat("#EmailTextField", isVisible());
            verifyThat("#LoginButton", isVisible());
            
        } catch (Exception e) {
            verifyThat(".dialog-pane", isVisible());
            
            clickOn("Aceptar");
        }
    }
    
   @Test
   public void test6_SuccessfulLogin() {
       clickOn("#EmailTextField");
       write("awallace@gmail.com");
       clickOn("#PasswordField");
       write("qwerty*9876"); 

       verifyThat("#LoginButton", isEnabled());
       clickOn("#LoginButton");

       sleep(2000);

       verifyThat("#WelcomeLabel", isVisible());
       verifyThat("#CustomerNameLabel", isVisible());
       verifyThat("#LogoutButton", isVisible());
//       clickOn("#LogoutButton");
//       
//       sleep(500);
//            verifyThat(".dialog-pane", isVisible());
//            
//            clickOn("Aceptar");

   }
}