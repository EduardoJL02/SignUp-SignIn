package UI;

/**
 * Interfaz que define el contrato de ayuda contextual para los controladores
 * de la aplicación Bank App.
 *
 * Cualquier controlador que quiera integrarse con el menú de ayuda
 * debe implementar esta interfaz. Esto permite que {@link MenuController}
 * funcione de forma genérica, sin conocer el tipo concreto de cada pantalla.
 *
 * PRINCIPIO APLICADO: Polimorfismo por interfaz.
 * MenuController declara una variable de tipo HelpProvider (no AccountsController
 * ni MovementController), y en tiempo de ejecución Java invoca el método
 * correcto según el objeto real que se haya inyectado.
 *
 * CÓMO AÑADIR UNA NUEVA PANTALLA AL SISTEMA DE AYUDA:
 *   1. Tu controlador implementa HelpProvider.
 *   2. Rellena getHelpFile() y getWindowTitle().
 *   3. Añade el HTML correspondiente en /resources/.
 *   4. Inyecta el controlador en MenuController con setActiveController(this).
 *   → No es necesario tocar MenuController en absoluto.
 *
 * @author [Tu nombre]
 * @version 1.0
 * @see MenuController
 * @see AccountsController
 * @see MovementController
 */
public interface HelpProvider {

    // =========================================================
    // CONSTANTES DE AYUDA DISPONIBLES
    // Centralizar los nombres aquí evita errores tipográficos
    // en las implementaciones ("account.html" vs "accounts.html")
    // =========================================================

    /** Archivo de ayuda para la pantalla de Cuentas */
    String HELP_ACCOUNTS  = "accounts.html";

    /** Archivo de ayuda para la pantalla de Movimientos */
    String HELP_MOVEMENTS = "movements.html";

    /** Archivo de ayuda genérico (fallback si no hay uno específico) */
    String HELP_DEFAULT   = "accounts.html";


    // =========================================================
    // MÉTODOS ABSTRACTOS (OBLIGATORIOS en cada implementación)
    // =========================================================

    /**
     * Retorna el nombre del archivo HTML de ayuda contextual
     * que corresponde a esta pantalla.
     *
     * El archivo debe existir en: /resources/{nombre}.html
     * Se recomienda usar las constantes definidas en esta interfaz
     * (HELP_ACCOUNTS, HELP_MOVEMENTS, etc.) para evitar errores.
     *
     * @return Nombre del archivo HTML. Nunca null ni vacío.
     */
    String getHelpFile();

    /**
     * Retorna el título que debe mostrarse en la ventana de ayuda.
     * Permite personalizar el encabezado según la pantalla activa.
     *
     * Ejemplo: "Ayuda - Gestión de Cuentas"
     *
     * @return Título descriptivo de la ventana de ayuda.
     */
    String getWindowTitle();


    // =========================================================
    // MÉTODOS DEFAULT (comportamiento base, se pueden sobrescribir)
    // Disponibles en Java 8+ mediante la palabra "default"
    // =========================================================

    /**
     * Valida que los datos retornados por esta implementación
     * sean usables antes de intentar cargar el archivo HTML.
     *
     * Este método tiene comportamiento por defecto: verifica que
     * getHelpFile() y getWindowTitle() no sean nulos ni vacíos.
     * Un controlador puede sobrescribirlo si necesita validación adicional.
     *
     * Ejemplo de uso en MenuController:
     * <pre>
     *   if (activeController.isHelpAvailable()) {
     *       showWebWindow(...);
     *   }
     * </pre>
     *
     * @return true si el sistema de ayuda está correctamente configurado.
     */
    default boolean isHelpAvailable() {
        // Validación defensiva: protege contra implementaciones incompletas
        String file  = getHelpFile();
        String title = getWindowTitle();

        return file  != null && !file.trim().isEmpty()
            && title != null && !title.trim().isEmpty();
    }

    /**
     * Retorna una descripción breve de la pantalla actual.
     * Útil para mostrar en la barra de estado o en el encabezado del HTML.
     *
     * Por defecto retorna el título de la ventana.
     * Los controladores pueden sobrescribirlo para dar más contexto.
     *
     * @return Descripción de la pantalla. Por defecto: getWindowTitle().
     */
    default String getScreenDescription() {
        return getWindowTitle();
    }
}