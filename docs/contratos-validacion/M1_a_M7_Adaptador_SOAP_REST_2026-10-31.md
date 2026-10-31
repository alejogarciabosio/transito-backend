# Contrato de autenticación — M1 Usuarios/Ciudadanos y M7 Tránsito

**Fecha:** 31/10/2026
**Estado:** implementado del lado de M7; pendiente de credenciales de prueba y prueba E2E con M1.
**Alcance:** autenticación síncrona; no usa eventos, Core ni RabbitMQ.

## Decisión de integración

M1 mantiene exclusivamente su interfaz REST:

`POST https://ciudadanos-m1-backend.onrender.com/auth/usuario/login`

```json
{
  "gmail": "correo@ejemplo.com",
  "password": "contraseña"
}
```

Por el requisito académico de SOAP, M7 expone una fachada SOAP 1.1 propia. Esa fachada recibe
la solicitud SOAP, invoca el REST de M1 y emite el JWT de M7. M1 no debe proveer SOAP, WSDL ni
namespace.

M7 sólo acepta una respuesta HTTP 200 cuando `auth` es `true`. Los tokens de M1 que llegan en
headers no se reutilizan: M7 emite y firma su propio JWT.

```text
Consumidor SOAP → M7 /soap/authentication → REST M1 /auth/usuario/login
```

## Interfaz SOAP de M7

- **Endpoint:** `POST /soap/authentication`
- **WSDL de M7:** `GET /soap/authentication?wsdl`
- **Protocolo:** SOAP 1.1 sobre HTTPS.
- **Operación:** `authenticateUser`.
- **Namespace de M7:** `https://transito.uade.edu.ar/soap/authentication`.

### Request SOAP

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:auth="https://transito.uade.edu.ar/soap/authentication">
  <soapenv:Body>
    <auth:authenticateUserRequest>
      <auth:gmail>usuario@ejemplo.com</auth:gmail>
      <auth:password>contraseña</auth:password>
    </auth:authenticateUserRequest>
  </soapenv:Body>
</soapenv:Envelope>
```

### Response SOAP

En caso válido, M7 devuelve su token JWT propio, su rol interno y su expiración:

```xml
<auth:authenticateUserResponse xmlns:auth="https://transito.uade.edu.ar/soap/authentication">
  <auth:token>jwt-de-m7</auth:token>
  <auth:role>INSPECTOR</auth:role>
  <auth:expiresInSeconds>7200</auth:expiresInSeconds>
</auth:authenticateUserResponse>
```

Las credenciales inválidas generan SOAP Fault HTTP 401; una cuenta bloqueada/inactiva, HTTP 403;
una caída de M1, HTTP 503. Una respuesta con forma inválida de M1 se traduce a HTTP 502.

## Identidad y roles

M7 conserva la identidad técnica como `actorType:actorId` y valida que `actorId` y `citizenId`
sean UUID. `citizenId` sólo se acepta para `actorType: CIUDADANO` y se conserva como claim
separado. Para ciudadanos M7 toma el campo `dni` como documento; para empleados lo ignora por
completo y nunca interpreta `0` como documento válido.

| Rol M1 | Rol interno M7 | Alcance |
|---|---|---|
| `CIUDADANO` | `CIUDADANO` | Consultas y acciones propias del ciudadano. |
| `MESA_ENTRADAS`, `RESPONSABLE_AREA` | `INSPECTOR` | Operación de tránsito. |
| `ADMINISTRATIVO` | `ADMINISTRADOR` | Administración operativa. |
| `AUDITOR` | `AUDITOR` | Sin permisos operativos hasta definir consultas de auditoría. |

## Pendientes de M1 para cerrar la integración

1. Cuentas de prueba: ciudadano válido, empleado válido, credenciales
   inválidas y cuenta bloqueada/inactiva.
2. Prueba E2E de M7 contra el endpoint REST de M1.
