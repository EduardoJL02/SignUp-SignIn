# Sistema de Autenticación y Gestión de Usuarios - JavaFX

Sistema completo de autenticación de usuarios (Sign-In/Sign-Up) desarrollado con JavaFX, integrando un backend REST sobre Java EE con servidor GlassFish y base de datos MySQL.

---

## 📋 Descripción del Proyecto

Aplicación de escritorio que implementa un sistema de registro e inicio de sesión con las siguientes características:

- **Login (Sign-In)**: Autenticación de usuarios mediante REST API
- **Registro (Sign-Up)**: Creación de nuevas cuentas con validación completa de datos
- **Página Principal**: Interfaz personalizada post-autenticación con gestión de sesión
- **Arquitectura Cliente-Servidor**: Separación clara entre frontend (JavaFX) y backend (Java EE REST)

---

## 🏗️ Arquitectura del Sistema

### **Cliente (JavaFX)**
```
src/
├── signup/signin/
│   ├── SignUpSignIn.java           # Entry point de la aplicación
│   └── SignUpWindow.java           # Inicializador ventana registro
├── UI/
│   ├── GestionUsuariosController.java       # Controlador LOGIN
│   ├── GestionUsuariosControllerSignUp.java # Controlador SIGN-UP
│   ├── PaginaPrincipalController.java       # Controlador página principal
│   ├── FXMLDocument.fxml                    # Interfaz LOGIN
│   ├── FXMLDocumentSignUp.fxml              # Interfaz SIGN-UP
│   └── PaginaPrincipal.fxml                 # Interfaz página principal
├── logic/
│   └── CustomerRESTClient.java     # Cliente REST (JAX-RS)
└── model/
    └── Customer.java               # Entidad Customer (POJO)
```

### **Servidor (Java EE - No incluido en este repo)**
- Backend REST con JAX-RS
- Persistencia con JPA/Hibernate
- Base de datos MySQL
- Endpoints:
  - `POST /customer` - Crear usuario (Sign-Up)
  - `GET /customer/signin/{email}/{password}` - Autenticación (Sign-In)

---

## 🔧 Requisitos Técnicos

### **Desarrollo**
- **JDK**: 1.8 (Java 8)
- **IDE**: NetBeans 8.2 (recomendado)
- **Build**: Apache Ant (incluido en NetBeans)

### **Servidor (Desarrollo Backend)**
- **Servidor de Aplicaciones**: GlassFish 4.x
- **Base de Datos**: MySQL 5.7+
- **Driver JDBC**: MySQL Connector/J 5.1.x

### **Librerías JavaFX**
- JavaFX 8 (incluido en JDK 8)
- Jersey Client 2.x (JAX-RS)

---

## 📖 Guía de Uso

### **1. Ventana LOGIN (Sign-In)**

#### **Funcionalidades:**
- Validación en tiempo real de email y contraseña
- Feedback visual de errores (bordes rojos, mensajes inline)
- Autenticación asíncrona contra REST API
- Navegación a Sign-Up mediante hyperlink

#### **Validaciones:**
- **Email**: Formato válido `usuario@dominio.com`
- **Contraseña**: Mínimo 8 caracteres

#### **Flujo de Autenticación:**
1. Usuario ingresa credenciales
2. Botón LOGIN se habilita si validaciones pasan
3. Petición POST a `/customer/signin/{email}/{password}`
4. **Respuestas del servidor:**
   - `200 OK`: Login exitoso → Navega a Página Principal
   - `401 Unauthorized`: Credenciales incorrectas → Muestra error inline
   - `500 Internal Server Error`: Error del servidor → Alert modal

---

### **2. Ventana SIGN-UP (Registro)**

#### **Funcionalidades:**
- Formulario completo de registro (11 campos obligatorios)
- Validación en tiempo real por campo
- Tooltips informativos (icono "?")
- Confirmación al volver (si hay datos ingresados)

#### **Campos y Validaciones:**

| Campo | Validación | Ejemplo |
|-------|-----------|---------|
| **First Name** | Solo letras | John |
| **Middle Initial** | Formato "A." | J. |
| **Last Name** | Solo letras | Doe |
| **Address** | Alfanumérico + símbolos básicos | 123 Main St |
| **City** | Solo letras | New York |
| **State** | Letras o código (e.g., "NY") | NY / Texas |
| **ZIP** | Exactamente 5 dígitos | 10001 |
| **Phone** | Mínimo 9 dígitos | 123456789 |
| **Email** | Formato email válido | john@example.com |
| **Password** | 8+ chars, 1 mayús, 1 minús, 1 número, 1 símbolo | Pass@123 |
| **Repeat Password** | Debe coincidir con Password | Pass@123 |

#### **Flujo de Registro:**
1. Usuario completa formulario
2. Botón CREATE ACCOUNT se habilita cuando todos los campos son válidos
3. Petición POST a `/customer` con datos del Customer
4. **Respuestas del servidor:**
   - `201 Created`: Cuenta creada → Alert éxito + Cierra ventana
   - `403 Forbidden`: Email ya registrado → Alert warning
   - `400 Bad Request`: Datos inválidos → Alert error
   - `500 Internal Server Error`: Error del servidor → Alert error

---

### **3. Página Principal**

#### **Funcionalidades:**
- Muestra información del usuario autenticado
- Saludo personalizado según hora del día
- Botón Logout con confirmación

#### **Datos Mostrados:**
- Nombre completo (First Name + Middle Initial + Last Name)
- Email
- ID de usuario

#### **Flujo de Logout:**
1. Usuario hace clic en "Log out"
2. Alert de confirmación
3. Si confirma: Limpia sesión + Navega a LOGIN
4. Si cancela: Permanece en Página Principal

---

## 🔐 Seguridad y Buenas Prácticas

### **Cliente (JavaFX)**

#### **1. Validación de Datos**

#### **2. Encoding de Parámetros URL**

#### **3. Manejo de Excepciones REST**

#### **4. Operaciones Asíncronas**


### **Servidor (Backend - Recomendaciones)**

#### **1. Nunca Almacenar Contraseñas en Texto Plano**

#### **2. Usar PreparedStatement (Prevenir SQL Injection)**

#### **3. HTTPS en Producción**

---

## 🐛 Manejo de Errores

### **Errores Comunes y Soluciones**

#### **1. "Connection refused" al ejecutar cliente**
**Causa:** Backend no está corriendo o URL incorrecta


#### **2. "FXMLLoader cannot find controller"**
**Causa:** fx:controller incorrecto en FXML


#### **3. "ClassNotFoundException: javax.ws.rs..."**
**Causa:** Librerías JAX-RS no incluidas

**Solución:**
1. Project Properties → Libraries
2. Add JAR/Folder → Agregar Jersey Client JARs

#### **4. Navegación falla al volver de Sign-Up a Login**
**Causa:** Stage no se reutiliza correctamente

---

## 🧪 Testing

### **Casos de Prueba Recomendados**

#### **Login**
- [ ] Email vacío → Botón deshabilitado
- [ ] Email inválido → Mensaje error inline
- [ ] Contraseña < 8 chars → Botón deshabilitado
- [ ] Credenciales incorrectas → 401 → Mensaje error
- [ ] Credenciales correctas → 200 → Navega a Página Principal
- [ ] Servidor offline → Muestra error de conexión

#### **Sign-Up**
- [ ] Todos los campos vacíos → Botón deshabilitado
- [ ] Middle Initial sin formato "A." → Error inline
- [ ] ZIP con letras → Error inline
- [ ] Contraseñas no coinciden → Botón deshabilitado
- [ ] Email duplicado → 403 → Alert warning
- [ ] Registro exitoso → 201 → Alert éxito + Cierra ventana
- [ ] Botón BACK con datos → Confirmación antes de cerrar

#### **Página Principal**
- [ ] Muestra nombre completo correcto
- [ ] Muestra email correcto
- [ ] Saludo personalizado según hora
- [ ] Logout con confirmación → Regresa a Login
- [ ] Logout sin confirmación → Permanece en Página Principal

---

## 📝 Notas Técnicas

### **Separación de Capas**

```
[UI Layer] → [Logic Layer] → [REST Client] → [Backend]
    ↓              ↓               ↓              ↓
Controllers   CustomerREST   JAX-RS Client   REST API
             Client logic                    (Java EE)
```

### **Gestión de Stage (Ventanas)**

- **Login**: Stage principal (único Stage de toda la app)
- **Sign-Up**: Modal APPLICATION_MODAL (bloquea Login hasta cerrar)
- **Página Principal**: Reutiliza Stage principal (no crea nuevo)

**Ventaja:** Evita múltiples ventanas abiertas simultáneamente.

---

## 📚 Recursos Adicionales

### **Documentación Oficial**
- [JavaFX Documentation](https://docs.oracle.com/javase/8/javafx/api/)
- [JAX-RS (Jersey) Guide](https://eclipse-ee4j.github.io/jersey/)
- [GlassFish Documentation](https://javaee.github.io/glassfish/)

### **Tutoriales Recomendados**
- JavaFX Scene Builder
- REST API con Java EE
- JPA/Hibernate básico

---

## 📞 Soporte y Contribuciones

### **Problemas Comunes**
- Revisar sección "Manejo de Errores" arriba
- Verificar logs de GlassFish y cliente

### **Contribuir**
1. Fork del repositorio
2. Crear rama feature (`git checkout -b feature/NuevaFuncionalidad`)
3. Commit cambios (`git commit -m 'Agrega nueva funcionalidad'`)
4. Push a la rama (`git push origin feature/NuevaFuncionalidad`)
5. Abrir Pull Request

### **Contacto**
- Issues: [GitHub Issues](URL_REPOSITORIO/issues)
- Email: eduardo.jimenez3@educa.madrid.org

---

## ✅ Checklist de Configuración Inicial

- [ ] JDK 8 instalado y configurado
- [ ] NetBeans 8.2 instalado
- [ ] GlassFish 4 configurado en NetBeans
- [ ] MySQL instalado y corriendo
- [ ] Base de datos `bank_db` creada
- [ ] Tabla `customer` creada con esquema correcto
- [ ] Proyecto backend desplegado en GlassFish
- [ ] Endpoint REST accesible (`http://localhost:8080/...`)
- [ ] Librerías JAX-RS agregadas al proyecto cliente
- [ ] URL del backend configurada en `CustomerRESTClient.java`
- [ ] Proyecto cliente ejecuta sin errores

---

**Versión:** 1.0.0  
**Última actualización:** Noviembre 2024  
**Autor:** Eduardo Jiménez y Pablo Rodríguez
