package UI;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality; // IMPORTANTE: Para hacer la ventana modal
import javafx.stage.Stage;
import javafx.stage.Window;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MenuController {
    
    private static final Logger LOGGER =
        Logger.getLogger(MenuController.class.getName());

    private static final String TITLE_MOVEMENTS = "Movements";
    private static final String TITLE_ACCOUNTS = "Accounts Management";
    private HelpProvider activeController;
    
    
    public void setActiveController(HelpProvider controller) {
        if (controller == null) {
            LOGGER.warning("Se intentó registrar un HelpProvider nulo.");
            return;
        }
        this.activeController = controller;
        LOGGER.info("HelpProvider registrado: " + controller.getClass().getSimpleName());
    }
    
    // --- SALIR ---
    @FXML
    private void handleExit(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cerrar Aplicación");
        alert.setHeaderText("Salir definitivamente");
        alert.setContentText("¿Está seguro de que desea cerrar la aplicación?");
        
        if (alert.showAndWait().orElse(null) == javafx.scene.control.ButtonType.OK) {
            Platform.exit();
            System.exit(0);
        }
    }

    // --- LOGOUT ---
    @FXML
    private void handleLogout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cerrar Sesión");
        alert.setHeaderText("Salir de la aplicación");
        alert.setContentText("¿Está seguro de que desea cerrar sesión y volver al login?");
        
        if (alert.showAndWait().orElse(null) == javafx.scene.control.ButtonType.OK) {
            Stage currentStage = getCurrentStage(event);
            if (currentStage != null) {
                Window parentWindow = currentStage.getOwner();
                currentStage.close();
                if (parentWindow != null && parentWindow instanceof Stage) {
                    ((Stage) parentWindow).close();
                }
            }
        }
    }

    // --- ACERCA DE (Modificado para usar WebView) ---
    @FXML
    private void handleAbout(ActionEvent event) {
        // Usamos la misma lógica de ventana modal para mostrar el About HTML
        Stage ownerStage = getCurrentStage(event);
        showWebWindow(ownerStage, "index.html", "Acerca de Bank App");
    }

    // --- AYUDA CONTEXTUAL ---
    @FXML
    private void handleHelp(ActionEvent event) {
        try {
            Stage currentStage = getCurrentStage(event);

            // Validación antes de intentar cargar el HTML
            if (activeController == null) {
                LOGGER.warning("handleHelp: no hay HelpProvider registrado. Usando default.");
                showWebWindow(currentStage, HelpProvider.HELP_DEFAULT, "Ayuda - Bank App");
                return;
            }

            // isHelpAvailable() valida que los datos no sean nulos/vacíos
            if (!activeController.isHelpAvailable()) {
                LOGGER.warning("HelpProvider inválido en: " 
                    + activeController.getClass().getSimpleName());
                showAlert("Ayuda", "Ayuda no disponible",
                    "No se encontró ayuda para esta pantalla.");
                return;
            }

            // POLIMORFISMO EN ACCIÓN:
            // getHelpFile() ejecuta la versión correcta según el tipo real del objeto.
            // Si es AccountsController  → "accounts.html"
            // Si es MovementController  → "movements.html"
            // MenuController no sabe ni le importa cuál es.
            String helpFile    = activeController.getHelpFile();
            String windowTitle = activeController.getWindowTitle();

            LOGGER.info("Abriendo ayuda: " + helpFile + " para: " 
                + activeController.getClass().getSimpleName());

            showWebWindow(currentStage, helpFile, windowTitle);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en handleHelp", e);
            showAlert("Error", "No se pudo cargar la ayuda", e.getMessage());
        }
    }

    // --- MÉTODO GENÉRICO PARA ABRIR HTML MODAL ---
    private void showWebWindow(Stage owner, String htmlFileName, String title) {
        Stage webStage = new Stage();
        webStage.setTitle(title);

        // --- CONFIGURACIÓN MODAL ---
        // Esto bloquea la ventana de atrás hasta que se cierre esta
        if (owner != null) {
            webStage.initOwner(owner);
            webStage.initModality(Modality.WINDOW_MODAL); 
        }

        WebView browser = new WebView();
        WebEngine webEngine = browser.getEngine();

        URL url = getClass().getResource("/resources/" + htmlFileName);
        
        if (url != null) {
            webEngine.load(url.toExternalForm());
        } else {
            System.err.println("Archivo no encontrado: /resources/" + htmlFileName);
            webEngine.loadContent("<html><body><h1>Error 404</h1><p>Archivo no encontrado.</p></body></html>");
        }

        Scene scene = new Scene(browser, 600, 500); // Tamaño ajustado
        webStage.setScene(scene);
        webStage.showAndWait(); // showAndWait es clave para la modalidad
    }

    // --- AUXILIARES ---
    private Stage getCurrentStage(ActionEvent event) {
        Object source = event.getSource();
        if (source instanceof MenuItem) {
            MenuItem menuItem = (MenuItem) source;
            if (menuItem.getParentPopup() != null) {
                 return (Stage) menuItem.getParentPopup().getOwnerWindow();
            }
            return null; 
        } else if (source instanceof Node) {
            return (Stage) ((Node) source).getScene().getWindow();
        }
        return null;
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}