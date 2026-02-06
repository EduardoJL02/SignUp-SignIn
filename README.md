# BankApp - Cliente de Escritorio JavaFX

## 1. Descripción del Proyecto

Este proyecto consiste en el desarrollo del lado cliente (**Front-end**) de una aplicación bancaria de escritorio. La aplicación proporciona una Interfaz Gráfica de Usuario (UI) construida con **JavaFX** para gestionar clientes, cuentas bancarias y movimientos, conectándose a un servidor RESTful (GlassFish 4) mediante **Jersey Client**.

El desarrollo sigue estrictamente los estándares de **Java 8 (JDK 1.8)**, priorizando la claridad didáctica y el uso de estructuras clásicas (clases anónimas, bucles tradicionales y comunicación síncrona).

## 2. Tecnologías y Herramientas

* **Lenguaje:** Java SE 8 (JDK 1.8).
* **IDE Recomendado:** NetBeans 8.2.
* **Framework UI:** JavaFX (FXML + SceneBuilder 2.0).
* **Cliente HTTP:** Jersey Client (`javax.ws.rs`).
* **Pruebas:** JUnit + TestFX.

## 3. Arquitectura y Patrón de Diseño

El proyecto sigue el patrón **MVC (Modelo-Vista-Controlador)**:

* **Model:** Clases (`Account`, `Customer`, `Movement`) anotadas con JAXB para la serialización XML/JSON.
* **View:** Archivos `.fxml` que definen la estructura visual.
* **Controller:** Clases Java que gestionan la lógica de la UI y los eventos.
* **Logic:** Clases `RESTClient` que encapsulan la comunicación HTTP con el servidor backend.

> **Nota Técnica:** Por requisitos de diseño, **no se utiliza asincronía** (hilos en segundo plano). Todas las peticiones al servidor se realizan en el hilo principal de la aplicación (JavaFX Application Thread), bloqueando la UI durante la transacción para garantizar la integridad secuencial de los datos.

## 4. Funcionalidades y Reglas de Negocio

La aplicación implementa los siguientes casos de uso descritos en la documentación funcional:

### A. Gestión de Usuarios (Sign Up / Sign In / Admin)

* **Login:** Acceso seguro para usuarios (Clientes) y Administradores.
* **Sign Up:** Registro de nuevos clientes.
* **CRUD Clientes:** Los administradores pueden gestionar la información de los clientes.

### B. Gestión de Cuentas (My Accounts)

Permite a los clientes ver y gestionar sus cuentas bancarias.

* **Tipos de Cuenta:** `STANDARD` y `CREDIT`.
* **Creación:**
* El ID de la cuenta se genera localmente.
* *Simulación de Error:* El sistema está programado para permitir 3 peticiones exitosas; la cuarta intentará generar un conflicto intencionado (según especificación de pruebas).


* **Modificación (UPDATE):**
* Solo se permite modificar la **descripción** y el **límite de crédito**.
* El límite de crédito solo es editable si la cuenta es tipo `CREDIT` (no puede ser negativo ni 0).


* **Borrado (DELETE):**
* Restricción estricta: **No se pueden borrar cuentas que tengan movimientos asociados**.
* El sistema valida esta condición antes de enviar la petición al servidor para evitar errores 500.



### C. Gestión de Movimientos (My Movements)

Visualización y control del historial de transacciones.

* **Consultas (READ):** Filtrado de movimientos por rango de fechas (Desde, Hasta, Entre dos fechas).
* **Borrado (Deshacer):**
* No existe borrado arbitrario.
* Solo se permite borrar el **último movimiento** registrado (función "Deshacer").
* Al borrar, el saldo de la cuenta se actualiza automáticamente acorde a la operación revertida.



## 5. Estructura del Proyecto

```text
src/
├── model/                  # Entidades (Account, Customer, Movement, AccountType)
├── ui/                     # Controladores y Vistas FXML
│   ├── AccountsController.java
│   ├── FXMLAccounts.fxml
│   ├── MovementController.java
│   ├── ...
├── logic/                  # Clientes REST (Jersey)
│   ├── AccountRESTClient.java
│   ├── CustomerRESTClient.java
│   ├── MovementRESTClient.java
│   └── BusinessLogicFactory.java
├── resources/              # Imágenes, estilos CSS y configuración
└── signup/signin/          # Clase Main (Punto de entrada)

```

## 6. Configuración e Instalación

1. **Backend:** Asegúrese de que el servidor GlassFish 4 esté ejecutándose y la base de datos `bankdb` esté desplegada.
2. **Configuración de Conexión:**
* Verifique la URL base del servicio REST en el archivo de propiedades o en la constante `BASE_URI` de los clientes REST (generalmente `http://localhost:8080/BankAppServer/webresources`).


3. **Compilación:**
* Abra el proyecto en NetBeans 8.2.
* Realice un "Clean and Build".


4. **Ejecución:**
* Ejecute la clase principal: `signup.signin.SignUpSignIn`.



## 7. Pruebas y Calidad (QA)

Se han implementado pruebas unitarias y de interfaz utilizando **JUnit** y **TestFX**.

* **Cobertura:** Las pruebas verifican el flujo de navegación, la habilitación/deshabilitación correcta de botones y las validaciones de campos.
* **Ejecución:** Click derecho en el paquete `test` -> "Test File".

## 8. Notas sobre Estilo de Código

Para mantener la compatibilidad y legibilidad en entornos educativos:

* Se utilizan **Clases Anónimas Internas** para el manejo de eventos (`new EventHandler...`) en lugar de Lambdas.
* Se evitan los `Streams` de Java 8; la lógica de iteración se realiza mediante bucles `for` o `for-each`.
* El manejo de excepciones se realiza mediante bloques `try-catch` explícitos con retroalimentación visual al usuario (`Alert`).

---

**Desarrollado para el Módulo de Desarrollo de Interfaces (DIN).**
*Curso 2025-2026*
