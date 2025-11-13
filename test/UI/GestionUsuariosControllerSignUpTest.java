/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package UI;

/**
 *
 * @author pablo
 */

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


/**
 *
 * @author david
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GestionUsuariosControllerSignUpTest extends ApplicationTest{
    
    @Override
    public void start(Stage stage) throws Exception{
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
        verifyThat("#btCreate", isDisabled());
        verifyThat("#tfFName", isFocused());
    }
    
    @Test
    public void test2_SignUp(){
        clickOn("#tfFName");
        write("Nisa");
        clickOn("#tfMName");
        write("A.");
        clickOn("#tfLName");
        write("Abyss");
        clickOn("#tfAddress");
        write("67 streen number 67");
        clickOn("#tfCity");
        write("Turkey");
        clickOn("#tfState");
        write("idkXD");
        clickOn("#tfZip");
        write("67676");
        clickOn("#tfPhone");
        write("676767670");
        clickOn("#tfEmail");
        write("6767676754@gmail.com");
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
        write("SP");
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
    }
}