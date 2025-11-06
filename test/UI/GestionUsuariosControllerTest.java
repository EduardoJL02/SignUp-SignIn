/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package UI;

import javafx.scene.input.MouseButton;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import static org.testfx.api.FxAssert.verifyThat;
import org.testfx.framework.junit.ApplicationTest;
import static org.testfx.matcher.base.NodeMatchers.isDisabled;
import static org.testfx.matcher.control.LabeledMatchers.hasText;

/**
 *
 * @author edu
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GestionUsuariosControllerTest extends ApplicationTest{
    
    
    
    public GestionUsuariosControllerTest() {
        
    }

    @Test
    public void testSomeMethod() {
        
    }
    @Test
    public void test1_InitialState() {
        verifyThat("#tfUsuario", hasText(""));
        verifyThat("#tfPassword",hasText(""));
        verifyThat("#btAceptar", isDisabled());
    }
    
}
