# Guía de pruebas en Postman

La siguiente guía describe, paso a paso, cómo probar en **Postman** las nuevas funcionalidades del Sistema de Gestión Hospitalaria: autenticación con Spring Security, control de acceso, bloqueo por intentos fallidos, auditoría transversal con AOP y recuperación de contraseña por token.

## Requisitos previos

- La aplicación en ejecución en `http://localhost:8080` (ver sección *Ejecución* del `README.md`).
- MySQL activo con la base de datos `db_datos_1`.
- Postman instalado (aplicación de escritorio o versión web).
- Credenciales iniciales disponibles (`admin / Admin123` y `medico / Medico123`).

## Preparación del entorno

1. Abra Postman y cree un **entorno** llamado `hospital` con la variable `base_url`:
   - Clave: `base_url`
   - Valor inicial y actual: `http://localhost:8080`
2. Cree una **colección** llamada `Sistema Hospitalario` y seleccione el entorno `hospital`.
3. Verifique que en *Settings* de la solicitud la opción *Automatically follow redirects* esté **desactivada** para observar los redireccionamientos 302 de la seguridad. Puede activarla después para las pruebas de flujo completo.

> **Nota sobre las cookies.** La sesión se establece con la receta `POST /login` (formulario). Postman guarda automáticamente la cookie `JSESSIONID` en su "cookie jar" y la envía en las solicitudes siguientes. No es necesario copiarla manualmente.

> **Nota sobre CSRF.** La protección CSRF está desactivada en esta aplicación, por lo que las solicitudes POST no requieren enviar un token adicional.

## Parte 1. Autenticación y protección de rutas

### Prueba 1.1. Acceso a un recurso protegido sin sesión

- Solicitud: `GET {{base_url}}/api/roles`
- Respuesta esperada: **302** con `Location: http://localhost:8080/login`. Si tiene *follow redirects* activado, verá la página HTML de inicio de sesión.
- Finalidad: comprobar que los recursos de la API exigen autenticación.

### Prueba 1.2. Inicio de sesión correcto (establece la sesión)

Esta es la forma recomendada para las pruebas de la API, porque crea la sesión que la auditoría AOP utilizará para identificar al usuario.

- Solicitud: `POST {{base_url}}/login`
- Cuerpo: `x-www-form-urlencoded`
  - `username = admin`
  - `password = Admin123`
- Respuesta esperada: **302** con `Location: http://localhost:8080/dashboard`.
- Revise en *Cookies* que se haya almacenado `JSESSIONID`.

### Prueba 1.3. Acceso autenticado a la API

- Solicitud: `GET {{base_url}}/api/roles`
- Respuesta esperada: **200** con un arreglo JSON de roles (incluye `SUPER ADMINISTRADOR`, `Médico`, etc.).

### Prueba 1.4. Inicio de sesión con credenciales incorrectas

- Solicitud: `POST {{base_url}}/login`
- Cuerpo (`x-www-form-urlencoded`):
  - `username = admin`
  - `password = incorrecta`
- Respuesta esperada: **302** con `Location: http://localhost:8080/login?error=true`. Si activa el seguimiento de redirecciones, verá la página de inicio con la alerta de error.
- Finalidad: comprobar el manejo del error de autenticación sin exponer detalles técnicos.

### Prueba 1.5. Bloqueo temporal por intentos fallidos

Ejecute **tres** veces la Prueba 1.4 con la misma cuenta. En el cuarto intento:

- Respuesta esperada: la alerta indica *Cuenta bloqueada temporalmente hasta HH:mm* (30 minutos de bloqueo).

> **Advertencia.** Esta prueba bloquea la cuenta durante 30 minutos. Si bloquea a `admin`, no podrá continuar con el resto de la guía. Se recomienda probarla al final o crear antes un usuario de prueba (ver Parte 3).

### Prueba 1.6. Cierre de sesión

- Solicitud: `POST {{base_url}}/logout`
- Respuesta esperada: **302** con `Location: http://localhost:8080/login?salida=true`. La cookie `JSESSIONID` queda invalidada.
- Verificación: repita la Prueba 1.1; deberá obtener nuevamente **302 hacia /login**.

## Parte 2. Operaciones autenticadas y auditoría AOP

Asegúrese de haber completado la Prueba 1.2 (sesión de `admin`) antes de continuar.

### Prueba 2.1. Consultar los identificadores de áreas y roles

Para crear recursos necesita los `id` existentes.

- Solicitudes:
  - `GET {{base_url}}/api/areas`
  - `GET {{base_url}}/api/roles`
- Anote un `id` de área y un `id` de rol. En Postman, cree las variables de entorno `areaId` y `rolId` con esos valores.

### Prueba 2.2. Crear un rol (registro de auditoría CREAR)

- Solicitud: `POST {{base_url}}/api/roles`
- Cuerpo (JSON):
  ```json
  {
    "nombre": "Coordinador de Emergencias",
    "descripcion": "Rol de prueba creado desde Postman",
    "estado": "ACTIVO"
  }
  ```
- Respuesta esperada: **201** con el objeto del rol creado.
- Anote el `id` del rol creado en una variable (por ejemplo, `rolIdNuevo`).

### Prueba 2.3. Verificar la auditoría transversal

- Solicitud: `GET {{base_url}}/api/auditorias`
- Respuesta esperada: **200** y, entre los primeros registros, uno con:
  - `usuario`: `admin`
  - `accion`: `CREAR`
  - `entidad`: `ROL`
  - `detalle`: contiene *Se creó el rol (… ms)*
  - campos de IP y fecha poblados.
- Solicitud alternativa por usuario: `GET {{base_url}}/api/auditorias/usuario/admin`.
- Finalidad: demostrar que la operación quedó registrada de forma transversal por el aspecto AOP, sin código manual en el controlador.

### Prueba 2.4. Asignar permisos a un rol

- Solicitud: `GET {{base_url}}/api/permisos` para obtener los identificadores de los permisos disponibles.
- Solicitud: `PUT {{base_url}}/api/roles/{{rolIdNuevo}}/permisos`
- Cuerpo (JSON):
  ```json
  {
    "permisoIds": [1, 2]
  }
  ```
- Respuesta esperada: **200** con `{ "exito": true, "mensaje": "Permisos asignados correctamente" }`.
- Verificación: `GET {{base_url}}/api/roles/{{rolIdNuevo}}/permisos` devuelve el arreglo `[1, 2]`.

### Prueba 2.5. Desactivar un rol (registro de auditoría DESACTIVAR)

- Solicitud: `DELETE {{base_url}}/api/roles/{{rolIdNuevo}}`
- Respuesta esperada: **200** con `{ "exito": true, "mensaje": "Rol desactivado correctamente" }`.
- Verificación de auditoría: `GET {{base_url}}/api/auditorias` incluye un registro con `accion = DESACTIVAR` y `entidad = ROL`.

### Prueba 2.6. Crear un usuario y consultar su auditoría

- Solicitud: `POST {{base_url}}/api/usuarios`
- Cuerpo (JSON):
  ```json
  {
    "username": "postulante",
    "password": "Clave123",
    "nombres": "Lucía",
    "apellidos": "Mendoza",
    "dni": "12345678",
    "correo": "lucia@hospital.com",
    "telefono": "987654321",
    "cargo": "Enfermera",
    "estado": "ACTIVO",
    "areaId": {{areaId}},
    "rolId": {{rolId}}
  }
  ```
- Respuesta esperada: **201** con el objeto del usuario creado.
- Verificación: `GET {{base_url}}/api/auditorias/usuario/admin` incluye un registro `CREAR` con `entidad = USUARIO`.

### Prueba 2.7. Bloqueo de usuario (opcional)

- Solicitud: `POST {{base_url}}/api/usuarios/{{usuarioNuevoId}}/desactivar`
- Respuesta esperada: **200**.
- Verificación: al usar el mismo usuario con `POST /login`, el mensaje indica que la cuenta está desactivada.

## Parte 3. Recuperación de contraseña por token

> **Advertencia.** Estas pruebas cambian la contraseña real de la cuenta. Se recomienda realizarlas al final de la sesión de pruebas o crear antes una cuenta de prueba (Prueba 2.6).

### Prueba 3.1. Solicitar el enlace de recuperación

- Solicitud: `POST {{base_url}}/api/auth/recuperar`
- Cuerpo (JSON):
  ```json
  {
    "correo": "lucia@hospital.com"
  }
  ```
- Respuesta esperada: **200** con `{ "exito": true, "mensaje": "Se generó el enlace de recuperación. Token: <TOKEN>" }`.
- En el entorno de prueba el token se devuelve en la respuesta. Guárdelo en una variable `tokenRecuperacion`.

### Prueba 3.2. Restablecer la contraseña con el token

- Solicitud: `POST {{base_url}}/api/auth/restablecer`
- Cuerpo (JSON):
  ```json
  {
    "token": "{{tokenRecuperacion}}",
    "password": "NuevaClave456"
  }
  ```
- Respuesta esperada: **200** con `{ "exito": true, "mensaje": "Contraseña restablecida correctamente" }`.
- Verificación: inicie sesión con el usuario y la nueva contraseña (Prueba 1.2).

### Prueba 3.3. Correo no registrado

- Solicitud: `POST {{base_url}}/api/auth/recuperar`
- Cuerpo (JSON):
  ```json
  {
    "correo": "inexistente@hospital.com"
  }
  ```
- Respuesta esperada: **400** con `{ "exito": false, "mensaje": "El correo no está registrado en el sistema" }`.

## Parte 4. Control de acceso por permisos (capa web)

La API exige únicamente autenticación; la autorización detallada por módulo se aplica en las vistas web mediante el interceptor `AccesoInterceptor`. Puede verificar este comportamiento con la sesión de `medico`.

1. Inicie sesión como `medico / Medico123` (`POST /login`, la misma receta de la Prueba 1.2).
2. Solicitud: `GET {{base_url}}/usuarios`
3. Respuesta esperada: **302** con `Location: http://localhost:8080/acceso-denegado`, porque el rol *Médico* no tiene el permiso `USUARIOS:VER`.
4. Solicitud de referencia: `GET {{base_url}}/api/roles` con la misma sesión devuelve **200**, porque la API solo exige estar autenticado.

## Resumen de solicitudes

| Método | Endpoint | Autenticación | Respuesta esperada |
|--------|----------|---------------|--------------------|
| GET | `/api/roles` | Ninguna | 302 hacia `/login` |
| POST | `/login` (form) | Pública | 302 hacia `/dashboard` |
| GET | `/api/roles` | Cookie de sesión | 200 (arreglo JSON) |
| POST | `/api/roles` | Cookie de sesión | 201 (rol creado) |
| GET | `/api/auditorias` | Cookie de sesión | 200 (registros de auditoría) |
| PUT | `/api/roles/{id}/permisos` | Cookie de sesión | 200 (mensaje de éxito) |
| DELETE | `/api/roles/{id}` | Cookie de sesión | 200 (mensaje de éxito) |
| POST | `/api/usuarios` | Cookie de sesión | 201 (usuario creado) |
| POST | `/api/auth/recuperar` | Pública | 200 (token de recuperación) |
| POST | `/api/auth/restablecer` | Pública | 200 (contraseña restablecida) |
| POST | `/logout` | Cookie de sesión | 302 hacia `/login?salida=true` |
| GET | `/usuarios` (sesión médico) | Cookie de sesión | 302 hacia `/acceso-denegado` |

## Recomendaciones finales

- Para que la auditoría AOP registre al usuario, las solicitudes a `/api/**` deben enviar la cookie de sesión obtenida con `POST /login`. El endpoint `POST /api/auth/login` devuelve un JSON informativo (usuario, rol y permisos), pero no abre sesión para las siguientes peticiones.
- Deje *Automatically follow redirects* desactivado mientras verifica que las respuestas de seguridad sean 302; actívelo si prefiere ver el resultado final del flujo.
- Si la cuenta queda bloqueada por la Prueba 1.5, use otra cuenta o espere 30 minutos.
- Los tokens de recuperación expiran a las 24 horas.