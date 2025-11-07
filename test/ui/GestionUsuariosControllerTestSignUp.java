/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ui;

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
import signupwindow.SignUpWindow;


/**
 *
 * @author david
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GestionUsuariosControllerTestSignUp extends ApplicationTest{
    
    @Override
    public void start(Stage stage) throws Exception{
        new SignUpWindow().start(stage);
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
    }
    
    @Test
    public void test3_UsuarioExiste(){
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