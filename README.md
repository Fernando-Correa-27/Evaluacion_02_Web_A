# Sistema de Gestión Hospitalaria

Aplicación web de gestión hospitalaria desarrollada con **Spring Boot 4.1.1** como evaluación del curso de Desarrollo de Aplicaciones Web. Implementa autenticación basada en sesiones, control de acceso por roles y permisos (RBAC), gestión de usuarios, áreas, roles y permisos, trazabilidad mediante auditoría y recuperación de contraseña por token.

## Contenido

- Descripción general
- Nuevas implementaciones
- Guía de uso
- Demostraciones por funcionalidad
- Pruebas en Postman
- Stack tecnológico
- Estructura del proyecto
- Modelo de datos
- Requisitos previos
- Configuración
- Ejecución
- Credenciales iniciales
- API REST
- Aplicación web

## Descripción general

El sistema cuenta con una interfaz web (Thymeleaf) y una API REST (`/api/**`). La aplicación realiza lo siguiente:

- Autenticación de usuarios con contraseña cifrada mediante BCrypt.
- Protección contra fuerza bruta: bloqueo temporal tras 3 intentos fallidos (30 minutos).
- Control de acceso por roles y permisos (RBAC) con permisos finos por módulo y acción.
- Gestión de usuarios ligados a empleados y organizados por áreas.
- Gestión de áreas, roles y permisos.
- Registro de auditoría de las operaciones realizadas por cada usuario.
- Recuperación de contraseña mediante tokens con expiración de 24 horas.

## Nuevas implementaciones

### Seguridad con Spring Security 7

La autenticación y la autorización se manejan a través de una cadena de filtros configurada en `config/SecurityConfig.java`:

- **Rutas públicas**: `/login`, `/recuperar/**`, `/restablecer/**`, `/api/auth/login`, `/api/auth/recuperar`, `/api/auth/restablecer`, recursos estáticos (`/css/**`, `/js/**`, etc.) y `/error`.
- **Rutas protegidas**: `/api/**` y cualquier otra ruta exigen autenticación.
- **Login mediante formulario**: POST a `/login` (parámetros `username` y `password`).
- **Logout mediante POST** a `/logout`: invalida la sesión, limpia la autenticación, elimina la cookie `JSESSIONID` y redirige a `/login?salida=true`.
- **Manejo de errores de autenticación**: al fallar el ingreso se guarda el mensaje en sesión y se redirige a `/login?error=true`, donde la alerta se muestra en la propia página (sin exponer detalles de implementación).

Componentes de apoyo:

| Clase | Responsabilidad |
|-------|-----------------|
| `seguridad/LoginAuthenticationProvider` | Autentica delegando en `AuthService.login`: valida BCrypt y aplica el bloqueo temporal por intentos fallidos. |
| `config/LoginSuccessHandler` | Tras un ingreso correcto, repuebla la sesión con el usuario logueado y las claves de sus permisos (`usuarioSesion` y `permisosSesion`). |
| `config/AccesoInterceptor` | Refuerza la autorización por módulo y exime rutas de alcance propio, como `/usuarios/perfil`. |

### Auditoría mediante AOP

Se sustituyó la auditoría manual por un enfoque transversal con **AspectJ**:

- `aspect/RegistrarAuditoria.java` define la anotación `@RegistrarAuditoria(entidad, accion, descripcion)`.
- `aspect/AuditoriaAspect.java` implementa un consejo `@Around` que:
  - mide la duración de la operación en milisegundos;
  - obtiene el usuario activo desde la sesión;
  - registra la dirección IP (considerando el encabezado `X-Forwarded-For`);
  - guarda el registro en la misma transacción de la operación.

Están anotados **17 métodos** en `UsuarioService`, `RolService`, `AreaService` y `PermisoService`, que abarcan crear, editar, activar, desactivar, asignar permisos y los cambios o restablecimientos de contraseña.

### Rediseño de la interfaz (design system clínico)

La interfaz se rediseñó por completo manteniendo intacta la lógica de negocio:

- **Sistema de tokens** en `static/css/styles.css`: colores semánticos (teal clínico `#0E7490`, verde de éxito, rojo destructivo, neutros *slate*), radios, sombras y transiciones.
- **Tipografía Figtree** desde Google Fonts, con jerarquía tipográfica clara y respaldos seguros del sistema.
- **Sidebar oscuro profesional** con ítems activos resaltados, secciones por área y visibilidad condicionada a los permisos del rol.
- **Tarjetas KPI** en el escritorio con chip de icono tintado y elevación al pasar el cursor.
- **Tablas** con cabecera neutra en mayúsculas, filas con *hover*, estados vacíos ilustrados y tarjetas de contenido alineadas a borde.
- **Botones semánticos** únicos (primario, éxito, peligro, aviso, suave, contorno) y **badges de estado** por acción.
- **Páginas de autenticación** (inicio de sesión, recuperación, restablecimiento) y de **acceso denegado** rediseñadas.

### Mejoras de experiencia de usuario

- **Menú lateral responsive**: en pantallas pequeñas el sidebar se convierte en un panel deslizante (off-canvas) con fondo atenuado; se abre desde la barra superior y se cierra con ESC, al hacer clic fuera o al elegir una opción.
- **Modal de confirmación** para acciones destructivas, reemplaza los diálogos nativos del navegador por un diálogo propio y consistente.
- **Estados de carga**: al enviar un formulario, los botones de envío se deshabilitan y muestran un indicador giratorio, lo que evita envíos duplicados.
- **Estados vacíos** en las tablas: cuando no hay registros o filtros sin resultados se muestra un mensaje descriptivo con icono.
- **Alertas accesibles**: se anuncian con `role="alert"`, se resaltan con colores semánticos y son cerrables con un botón propio.
- **Accesibilidad**: foco visible para navegación por teclado (`:focus-visible`), puntero en elementos interactivos y soporte para `prefers-reduced-motion`.
- **Barra superior mejorada**: avatar, nombre y rol del usuario, acceso a perfil y botón de cierre de sesión con etiqueta visible.

## Guía de uso

### Acceso al sistema

1. Ejecute la aplicación (ver sección [Ejecución](#ejecución)).
2. Abra `http://localhost:8080`.
3. Ingrese con una de las credenciales iniciales de la sección [Credenciales iniciales](#credenciales-iniciales).

Con credenciales incorrectas la página muestra una alerta de error. Tras 3 intentos fallidos la cuenta se bloquea temporalmente durante 30 minutos.

Si olvida la contraseña, en la página de inicio de sesión seleccione *Olvidé mi contraseña*; el sistema genera un enlace de recuperación (en el entorno de prueba, el enlace se muestra en pantalla) válido por 24 horas.

### Interfaz principal

- **Barra lateral**: muestra las secciones *Principal* (Escritorio) y *Administración* (Usuarios, Roles y Permisos, Áreas, Auditoría). Solo se muestran los módulos que el rol del usuario puede ver. El ítem de la página actual queda resaltado.
- **Barra superior**: muestra el avatar, el nombre, el rol del usuario y los botones *Mi perfil* y *Salir*.
- En pantallas pequeñas, la barra lateral se oculta y se abre mediante el botón de menú de la barra superior.

### Módulos

- **Escritorio**: presenta cuatro tarjetas con el total de usuarios, roles y permisos, áreas activas y registros de auditoría, con enlaces a cada módulo. También se muestra la información de la cuenta (nombre, rol, área, último acceso y estado).
- **Usuarios**: lista de cuentas con búsqueda por nombre, DNI o correo, y filtros por estado y rol. Permite crear, editar, restablecer contraseña y activar o desactivar usuarios según los permisos del rol.
- **Roles y permisos**: administra los roles y la asignación de permisos agrupados por módulo mediante casillas de verificación.
- **Áreas**: administra las áreas asistenciales y administrativas del hospital.
- **Auditoría**: consulta el registro de operaciones críticas (fecha, usuario, acción, entidad, detalle e IP). Las acciones favorables se muestran en verde y las de riesgo en rojo.
- **Mi perfil**: consulta los datos de la cuenta y actualiza la contraseña.

## Demostraciones por funcionalidad

Estos escenarios permiten comprobar las nuevas funcionalidades en una instancia en ejecución.

1. **Protección de rutas**
   Abrir `/dashboard` sin sesión activa redirige a `/login`. Tras iniciar sesión como `admin` / `Admin123` se ingresa al Escritorio con el mensaje *Bienvenido(a) Fernando Correa*.

2. **Mensajes de error de autenticación**
   Intentar ingresar con una contraseña incorrecta muestra la alerta de error en la propia página de inicio, sin detalles técnicos.

3. **Control de acceso por permisos**
   Iniciar sesión como `medico` / `Medico123`: el menú solo muestra *Escritorio*. Intentar acceder a `/usuarios` presenta la página *Acceso denegado*.

4. **Auditoría transversal (AOP)**
   Con la sesión de `admin`, crear un rol en *Roles y Permisos* y luego consultar *Auditoría*: aparece un registro `CREAR` con badge verde, usuario `admin`, IP y duración en milisegundos. Desactivar un área genera un registro `DESACTIVAR` con badge rojo.

5. **Modal de confirmación**
   En *Usuarios*, pulsar el botón de desactivar de una cuenta activa: se abre el modal de confirmación propio del sistema (en lugar del diálogo del navegador). Al confirmar, la operación se registra en auditoría.

6. **Estados de carga**
   Enviar el formulario *Nuevo usuario*: el botón *Guardar* se deshabilita y muestra un indicador giratorio hasta completar la operación.

7. **Interfaz responsiva**
   Reducir el ancho de la ventana o usar la vista de móvil en las herramientas de desarrollo: el menú se oculta y se despliega como panel lateral con fondo atenuado; se cierra al pulsar ESC o al hacer clic fuera.

8. **Estados vacíos**
   En *Usuarios*, escribir un término de búsqueda sin coincidencias: la tabla muestra un estado vacío con icono y mensaje descriptivo.

9. **Accesibilidad**
   Navegar con la tecla TAB por los formularios y menús: el elemento enfocado muestra un anillo de foco visible. Los elementos interactivos muestran el puntero correspondiente.

10. **Cierre de sesión seguro**
    Pulsar *Salir* envía una solicitud POST a `/logout`, invalida la sesión y redirige a `/login?salida=true`. Cualquier intento posterior de acceder a una ruta protegida vuelve a redirigir al inicio de sesión.

## Pruebas en Postman

La guía [Pruebas en Postman](GUIA_POSTMAN.md) describe, paso a paso, cómo verificar en Postman la autenticación con Spring Security, la protección de rutas, el bloqueo por intentos fallidos, la auditoría transversal por AOP, el control de acceso por permisos y la recuperación de contraseña por token. Incluye las recetas de cada solicitud, los cuerpos JSON y las respuestas esperadas.

## Stack tecnológico

| Tecnología | Versión |
|------------|---------|
| Java | 17 |
| Spring Boot | 4.1.1 |
| Spring Data JPA (Hibernate) | Incluido |
| Spring Security | Incluido |
| Spring AOP (AspectJ) | Incluido (`spring-boot-starter-aspectj`) |
| Thymeleaf | Incluido |
| Spring Security Crypto (BCrypt) | Incluido |
| MySQL Connector/J | Incluido |
| Maven | Wrapper incluido |
| Lombok | Incluido |

## Estructura del proyecto

```
src/main/java/com/tecup/exam_1/
├── aspect/         Auditoría transversal (@RegistrarAuditoria, AuditoriaAspect)
├── config/         Configuración de seguridad web, cifrado, interceptor y datos iniciales
├── controller/
│   ├── api/        Controladores REST (/api/**)
│   └── web/        Controladores de la aplicación web (Thymeleaf)
├── dto/            Objetos de transferencia de datos y validaciones
├── exception/      Manejo global de excepciones de la API
├── model/          Entidades JPA y enumerados
├── repository/     Repositorios de persistencia (Spring Data JPA)
├── seguridad/      Servicios de sesión y autenticación
└── service/        Lógica de negocio
src/main/resources/
├── static/         Recursos estáticos (CSS del design system)
└── templates/      Vistas Thymeleaf (fragmentos y páginas)
```

## Modelo de datos

El esquema se genera automáticamente mediante Hibernate (`ddl-auto=update`). Las tablas principales son:

- `areas`
- `empleados`
- `usuarios`
- `roles`
- `permisos`
- `rol_permiso` (tabla pivote roles-permisos)
- `auditorias`
- `tokens_recuperacion`

## Requisitos previos

- JDK 17 o superior.
- MySQL 8.x corriendo en `localhost:3306`.
- Base de datos `db_datos_1` creada.

## Configuración

Edite `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/db_datos_1?useSSL=false&serverTimezone=America/Lima&characterEncoding=utf8&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=
server.port=8080
```

## Ejecución

```bash
./mvnw spring-boot:run
```

La aplicación levanta en `http://localhost:8080`. Al iniciar por primera vez, un `DataSeeder` carga datos iniciales si la base de datos está vacía.

## Credenciales iniciales

| Usuario | Contraseña | Rol |
|---------|------------|-----|
| `admin` | `Admin123` | SUPER ADMINISTRADOR |
| `medico` | `Medico123` | Médico |

## API REST

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/auth/login` | Iniciar sesión |
| POST | `/api/auth/recuperar` | Solicitar recuperación de contraseña |
| POST | `/api/auth/restablecer` | Restablecer contraseña con token |
| GET | `/api/usuarios` | Listar usuarios (filtros: `busqueda`, `estado`, `rolId`) |
| GET | `/api/usuarios/{id}` | Obtener usuario por id |
| POST | `/api/usuarios` | Crear usuario |
| PUT | `/api/usuarios/{id}` | Editar usuario |
| DELETE | `/api/usuarios/{id}` | Desactivar usuario |
| POST | `/api/usuarios/{id}/activar` | Activar usuario |
| POST | `/api/usuarios/{id}/desactivar` | Desactivar usuario |
| PUT | `/api/usuarios/{id}/password` | Cambiar contraseña |
| PUT | `/api/usuarios/{id}/password-admin` | Restablecer contraseña (admin) |
| GET | `/api/roles` | Listar roles (filtro: `estado`) |
| GET | `/api/roles/{id}` | Obtener rol por id |
| GET | `/api/roles/{id}/permisos` | Permisos del rol |
| POST | `/api/roles` | Crear rol |
| PUT | `/api/roles/{id}` | Editar rol |
| PUT | `/api/roles/{id}/permisos` | Asignar permisos al rol |
| DELETE | `/api/roles/{id}` | Desactivar rol |
| POST | `/api/roles/{id}/activar` | Activar rol |
| GET | `/api/permisos` | Listar permisos |
| GET | `/api/permisos/modulos` | Permisos agrupados por módulo |
| GET | `/api/permisos/{id}` | Obtener permiso por id |
| POST | `/api/permisos` | Crear permiso |
| PUT | `/api/permisos/{id}` | Editar permiso |
| DELETE | `/api/permisos/{id}` | Eliminar permiso |
| GET | `/api/areas` | Listar áreas (filtro: `estado`) |
| GET | `/api/areas/{id}` | Obtener área por id |
| POST | `/api/areas` | Crear área |
| PUT | `/api/areas/{id}` | Editar área |
| DELETE | `/api/areas/{id}` | Desactivar área |
| POST | `/api/areas/{id}/activar` | Activar área |
| GET | `/api/auditorias` | Últimas 100 auditorías |
| GET | `/api/auditorias/usuario/{username}` | Auditorías por usuario |
| GET | `/api/auditorias/{id}` | Obtener auditoría por id |

## Aplicación web

| Ruta | Descripción |
|------|-------------|
| `/login` | Inicio de sesión |
| `/dashboard` | Panel principal |
| `/usuarios/**` | Gestión de usuarios |
| `/roles/**` | Gestión de roles y asignación de permisos |
| `/areas/**` | Gestión de áreas |
| `/auditoria` | Consulta de auditoría |
| `/recuperar` | Solicitud de recuperación de contraseña |
| `/restablecer` | Restablecimiento de contraseña |

## Autor

Fernando Correa Huincho