# Ventana LOGIN — SignUp-SignIn

Este proyecto contiene una implementación de ejemplo de una ventana de inicio de sesión (LOGIN) creada con JavaFX. La rama actual (`GestionUsuariosFeature`) incluye la interfaz FXML y el controlador que gestionan la validación de correo y contraseña.

## Contenido

- `src/` — Código fuente Java.
	- `signup/signin/SignUpSignIn.java` — Clase principal (entry point) del proyecto.
	- `UI/GestionUsuariosController.java` — Controlador JavaFX que maneja la ventana LOGIN.
- `src/UI/FXMLDocument.fxml` — Definición de la interfaz de usuario (FXML) para la ventana LOGIN.
- `build.xml` — Script de Ant para compilar/ejecutar el proyecto (proyecto NetBeans compatible).

## Requisitos

- JDK 8 o superior. Si usa JDK 11 o posterior, asegúrese de incluir los módulos de JavaFX o ejecutar con el classpath/module-path apropiado.
- NetBeans (recomendado) o Ant para compilar y ejecutar.

## Cómo ejecutar

1. Abrir el proyecto en NetBeans y ejecutar desde el IDE (Run Project).
2. (Opcional) Desde línea de comandos con Ant:

	 - Abra una terminal en la raíz del proyecto.
	 - Ejecutar: `ant` o usar el target configurado en `build.xml` (NetBeans genera targets por defecto).

Si usa JDK 11+, puede que necesite indicar la ruta a las librerías JavaFX, por ejemplo:

```
# Ejemplo (dependiendo de su instalación y versión de JavaFX):
# java --module-path /ruta/a/javafx/lib --add-modules javafx.controls,javafx.fxml -jar dist/YourApp.jar
```

## Uso de la ventana LOGIN

- El campo de correo (`EmailTextField`) valida el formato básico de correo.
- El botón `LoginButton` permanece deshabilitado hasta que los datos cumplan la validación mínima.
- La contraseña debe cumplir la longitud mínima configurada en el controlador (por defecto 8 caracteres).
- Mensajes de error y feedback se muestran en `Error_email`, `Error_password` y `Tooltip` configurados por el controlador.

## Estructura y edición rápida

- La UI está en `src/UI/FXMLDocument.fxml`. Si modifica la interfaz con Scene Builder, asegúrese de no cambiar los fx:id usados por el controlador.
- El controlador es `src/UI/GestionUsuariosController.java`. Aquí encontrará:
	- Validación del correo (expresión regular definida en `EMAIL_REGEX`).
	- Lógica para habilitar/deshabilitar el botón de login.
	- Manejo de eventos del botón y cambios en campos.

## Desarrollo y pruebas

- Para añadir usuarios o cambiar la lógica de autenticación, edite el controlador y/o incorpore una capa de servicios.
- Recomendación: añadir pruebas unitarias para la validación de correo y contraseña.

## Notas y problemas conocidos

- El `EMAIL_REGEX` en el controlador es una validación básica; si necesita validación estricta use una librería o una expresión más completa.
- Asegúrese de ejecutar la inicialización de la UI en el JavaFX Application Thread (Platform.runLater si es necesario).

## Contacto

Si tienes preguntas o quieres proponer mejoras, abre un issue o haz un pull request en el repositorio.
