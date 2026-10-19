**MODULO 7 \- Transito** 

> Histórico. Para contratos externos vigentes con envelope Core, usar [modulo7-payloads-v4-core.md](modulo7-payloads-v4-core.md). Esta versión conserva formatos y ejemplos anteriores.

El documento del TPO exige que todo evento tenga como mínimo: tipo de evento, ID único, fecha/hora de generación, módulo de origen, y los datos del evento. 

Por ello, se envuelve todos los payloads así, y documentar los campos de abajo (`infractionId`, `licensePlate`, etc.) como el contenido de `data`: 

**{**

  **"eventId": "uuid",**

  **"eventType": "infractionConfirmed",**

  **"occurredAt": "2026-08-24T14:30:00-03:00",**

  **"sourceModule": "transito",**

  **"data": { "...": "..." }**

**}**

---

# **Eventos que PUBLICAMOS:**

* A **Rentas**:

  * ### infractionConfirmed {

    * ***infractionId:*** string (UUID);  
    * ***debtorId:*** string    
    * ***debtorIdType:*** "DNI" | "CUIT"   
    * ***licensePlate:*** string;   
    * ***infractionType:*** string (enum);   
    * ***infractionDateTime:*** datetime (ISO 8601 con zona horaria);  
    * ***baseAmount:*** number;  
    * ***aggravatingFactors:*** array\<string\>;   
    * ***finalAmount:*** number;  
    * ***inspectorId:*** string;  
    * ***location:*** {street: string, crossStreet: string, latitude: number, longitude: number}  

    }

| *Campo* | *Tipo* | *Req.* | *Descripcion* |
| :---- | :---- | :---- | :---- |
| *infractionId* | string (UUID)  | Si | Identificador único de la infracción.  |
| *debtorId* | string  | Si | Identificador de la persona u organización responsable de la deuda.  |
| *debtorIdType* | “DNI”/”CUIT” | Si | Indica qué tipo de identificación contiene **debtorId.**  |
| *licensePlate* | string  | Si | Patente del vehículo involucrado.  |
| *infractionType* | string (enum) | Si | Tipo de infraccion. |
| *infractionDateTime* | datetime | Si | Fecha y hora en que ocurrio la infraccion. |
| *baseAmount*  | number | Si | Importe base de la infracción.  |
| *aggravatingFactors*  | array\<string\> | Si | Agravantes aplicados. Puede ser un arreglo vacío.   |
| *finalAmount*  | number | Si | Importe final luego de aplicar agravantes. |
| *inspectorId*  | string | Si | Identificador del inspector responsable.  |
| *location*  | object | Si | Ubicación de la infracción. |

**Ejemplo:**  
{  
  "eventId": "7f9c91f0-03c6-4baf-8791-8cb2c38981d2",  
  "eventType": "infractionConfirmed",  
  "occurredAt": "2026-08-24T14:30:00-03:00",  
  "sourceModule": "transito",  
  "data": {  
    "infractionId": "93fd4382-2343-4f3f-a127-9328b29fc520",  
    "debtorId": "40123456",  
    "debtorIdType": "DNI",  
    "licensePlate": "AB123CD",  
    "infractionType": "ILLEGAL\_PARKING",  
    "infractionDateTime": "2026-08-24T13:45:00-03:00",  
    "baseAmount": 50000,  
    "aggravatingFactors": \["REINCIDENCE"\],  
    "finalAmount": 75000,  
    "inspectorId": "INS-245",  
    "location": {  
      "street": "Av. Cabildo",  
      "crossStreet": "Juramento",  
      "latitude": \-34.5621,  
      "longitude": \-58.4563  
    }  
  }  
}

* A **Obras y Ambiente**:

  * ### streetClosureApproved (M3 y M6) {

    * **streetClosureId:**  string (UUID);  
    * **closureRequestId:** string;  
    * **requestingModule;** "Obras" | "Ambiente" → *Este campo hace referencia a QUÉ módulo pidió el corte de calle, ya sea el Módulo de Obras o el de Ambiente. Clave.*  
    * **startDate / endDate;** datetime;  
    * **affectedStreet:** array\<string\>;   
    * **detours:** array \<{ street: string, alternateRoute: string }\> ;  
    * **conditions:** array\<string\>;

		}

| *Campo*  | *Tipo* | *Req.* | *Descripcion* |
| :---- | :---- | :---- | :---- |
| *streetClosureId* | string (UUID) | si | Identificador del corte creado por Tránsito.  |
| *closureRequestId* | string | si | Identificador de la solicitud original.  |
| *requestingModule* | “Obras”/”Ambiente” | si | Módulo que solicitó el corte.  |
| *startDate* | datetime | si | Inicio autorizado del corte.  |
| *endDate* | datetime | si | Fin autorizado del corte.  |
| *affectedStreet* | array\<string\>  | si | Calles afectadas.  |
| *detours* | array\<string\>  | si | Desvíos definidos por Tránsito.  |
| *conditions* | array\<string\>  | si | Condiciones o restricciones aplicables.  |

**Ejemplo:**

{  
  "eventId": "7acd8bce-adc9-4310-a390-28daf0956e8b",  
  "eventType": "streetClosureApproved",  
  "occurredAt": "2026-08-24T15:10:00-03:00",  
  "sourceModule": "transito",  
  "data": {  
    "streetClosureId": "c819cfbc-9848-43cc-9172-e79201009126",  
    "closureRequestId": "REQ-2001",  
    "requestingModule": "Obras",  
    "startDate": "2026-08-25T08:00:00-03:00",  
    "endDate": "2026-08-25T18:00:00-03:00",  
    "affectedStreet": \["Av. Santa Fe"\],  
    "detours": \[  
      {  
        "street": "Av. Santa Fe",  
        "alternateRoute": "Av. Córdoba"  
      }  
    \],  
    "conditions": \[  
      "Colocar señalización preventiva",  
      "Mantener acceso para emergencias"  
    \]  
  }  
}

* ### streetClosureRejected (M3 y M6) {

  * **closureRequestId:** string (UUID);  
    * **rejectionReason:** string;  
    * **requestingModule:** "Obras" | "Ambiente" ;

		}

| *Campo* | *Tipo* | *Req.* | *Descripcion* |
| :---- | :---- | :---- | :---- |
| *closureRequestId* | string (UUID) | si | Solicitud de corte rechazada.  |
| *rejectionReason* | string | si | Motivo del rechazo.  |
| *requestingModule* | “Obras”/”Ambiente” | si | Módulo que realizó la solicitud.  |

**Ejemplo:**

{  
  "eventId": "4253152c-2733-465d-b90c-c45a60921fb8",  
  "eventType": "streetClosureRejected",  
  "occurredAt": "2026-08-24T15:20:00-03:00",  
  "sourceModule": "transito",  
  "data": {  
    "closureRequestId": "REQ-2002",  
    "rejectionReason": "El corte afectaría una vía principal durante horario pico.",  
    "requestingModule": "Ambiente"  
  }  
}

* ### streetClosureEnded (M3 y M6)  {

  * **streetClosureId:** string (UUID);  
    * **closureRequestId:** string (UUID);   
    * **completionDateTime:** datetime;   
    * **notes:** string;

		}

| *Campo* | *Tipo* | *Req.* | *Descripcion* |
| :---- | :---- | :---- | :---- |
| *streetClosureId* | string (UUID) | si | Identificador del corte finalizado.  |
| *closureRequestId*  | string (UUID) | si | Identificador de la solicitud original enviada por Obras o Ambiente. Permite mantener la trazabilidad con `streetClosureApproved`.  |
| *completionDateTime* | datetime | si | Fecha y hora efectiva de finalización.  |
| *notes* | string | si | Observaciones del cierre.  |

**Ejemplo:**

{  
  "eventId": "e7ad58f0-b46d-4760-b68a-d88e04bdc09d",  
  "eventType": "streetClosureEnded",  
  "occurredAt": "2026-08-25T17:40:00-03:00",  
  "sourceModule": "transito",  
  "data": {  
    "streetClosureId": "c819cfbc-9848-43cc-9172-e79201009126",  
    "closureRequestId": "5a51284e-8fbe-4ab2-8db4-823d37c27d12",  
    "completionDateTime": "2026-08-25T17:35:00-03:00",  
    "notes": "La circulación fue restablecida normalmente."  
  }  
}

* ### roadAccidentRegistered (M3) {

  *  **accidentId:** string (UUID);  
    *  **accidentDateTime:** datetime;  
    * **location:** { street: string, crossStreet: string, latitude: number, longitude: number };  
    * **involvesInfrastructureDamage:** boolean;  
    * **damageDescription:** string;  
    * **vehicles:** array\<{ licensePlate: string, ownerNationalId: string }\>;  
    * **persons:** array\<{ nationalId: string, injuryLevel: "none" | "minor" | "severe" }\>;  
    * **inspectorId:** string;

		}

| *Campo* | *Tipo* | *Req.* | *Descripcion* |
| :---- | :---- | :---- | :---- |
| *accidentId* | string (UUID) | si | Identificador del accidente.  |
| *accidentDateTime* | datetime | si | Fecha y hora del accidente.  |
| *location* | object | si | Ubicación del hecho.  |
| *involvesInfrastructureDamage* | boolean | si | Indica si hubo daños sobre infraestructura.  |
| *damageDescription* | string | si | Descripción del daño. Requerido si **involvesInfrastructureDamage \= true.**  |
| *vehicles* | array\<object\>  | si | Vehículos involucrados.  |
| *persons* | array\<object\>   | si | Personas involucradas.  |
| *inspectorId* | string | si | Inspector que registró el accidente.  |

**Ejemplo:**

{  
  "eventId": "b95adb2d-82e8-4f71-96aa-c85804759eac",  
  "eventType": "roadAccidentRegistered",  
  "occurredAt": "2026-08-24T18:40:00-03:00",  
  "sourceModule": "transito",  
  "data": {  
    "accidentId": "ACC-5021",  
    "accidentDateTime": "2026-08-24T18:20:00-03:00",  
    "location": {  
      "street": "Av. Rivadavia",  
      "crossStreet": "Medrano",  
      "latitude": \-34.6118,  
      "longitude": \-58.4214  
    },  
    "involvesInfrastructureDamage": true,  
    "damageDescription": "Semáforo dañado por impacto.",  
    "vehicles": \[  
      {  
        "licensePlate": "AC456EF",  
        "ownerNationalId": "38765432"  
      }  
    \],  
    "persons": \[  
      {  
        "nationalId": "38765432",  
        "injuryLevel": "minor"  
      }  
    \],  
    "inspectorId": "INS-124"  
  }  
}

* A **Atención Ciudadana**:

  * ### updateTicketStatus

Nota de formato — eventos de tickets: `updateTicketStatus` y `ticketUpdated` no usan el sobre estándar (`eventId`/`occurredAt`/`sourceModule`/`data`). Usan un formato acordado específicamente con Módulo 2 para toda la familia de eventos de tickets: `specVersion`, `eventId`, `eventType`, `occurredAt`, `producer: {moduleId, service}`, `subject`, `data`. Es una excepción intencional, no un error de transcripción. 

| *Campo*  | *Tipo* | *Req.* | *Descripcion* |
| :---- | :---- | :---- | :---- |
| *ticketId* | integer  | si | Identificador del ticket de M2 sobre el que Tránsito informa una novedad. |
| *updateType* | enum | si | Tipo de hecho operativo informado por M7. Valores: STARTED, PROGRESS, INFORMATION\_REQUIRED, RETURNED, RESOLVED, REJECTED. |
| *publicMessage* | string / null | no | Mensaje que M2 puede mostrar al ciudadano. |
| *internalMessage* | string / null | no | Nota privada para M2. No se muestra al ciudadano ni se republica. |
| *progress* | integer / null | no | Avance estimado de la gestión cuando corresponda. |
| *details* | object / null | cond | Objeto específico según el updateType. |
| *attachments* | array\<attachment\> | no | Archivos o evidencias nuevas asociadas a esa actualización. |
| *updatedBy* | actor | si | Actor de M7 que realizó o registró la actualización. |
| *statusChangedAt* | datetime | si | Fecha y hora real en que ocurrió el hecho operativo. |

**Ejemplo:**

{  
  "specVersion": "1.0",  
  "eventId": "b7a0ed73-b644-4d22-bc27-36cb39f0fe02",  
  "eventType": "updateTicketStatus",  
  "occurredAt": "2026-08-24T18:30:00-03:00",  
  "producer": {  
    "moduleId": "M7",  
    "service": "transito-api"  
  },  
  "subject": "tickets/2045",  
  "data": {  
    "ticketId": 2045,  
    "updateType": "RESOLVED",  
    "publicMessage": "La situación fue verificada por el área de Tránsito.",  
    "internalMessage": "Se constató la infracción y se labró el acta correspondiente.",  
    "progress": 100,  
    "details": {  
      "resolution": {  
        "type": "ACTION\_COMPLETED"  
      }  
    },  
    "attachments": \[\],  
    "updatedBy": {  
      "type": "AREA\_USER",  
      "id": "USR-M7-15"  
    },  
    "statusChangedAt": "2026-08-24T18:29:00-03:00"  
  }  
}

* A **Notificaciones (Core) o eventos internos**:

  * ### infractionRegistered {

    * ***infractionId***: string (UUID);  
    * ***offenderNationalId***: string; (opcional) → Puede haber una infracción asociada al vehículo/titular sin tener necesariamente identificado al conductor.  
    * ***licensePlate***: string;  
    * ***infractionType***: string (enum);   
    * ***location***: { street: string, crossStreet: string, latitude: number, longitude: number };  
    * ***evidenceUrl***: array\<string\>;   
    * ***inspectorId***: string;  
    * ***dateTime***: datetime;  
    * ***baseAmount:*** number;  
    * ***aggravatingFactors:*** array\<string\>;   
    * ***finalAmount:*** number;

  }


Los últimos 3 valores reflejan el cálculo inicial; **infractionConfirmed** lleva el valor firme, que puede diferir si hubo ajuste post-apelación. 

| *Campo* | *Tipo* | *Req.* | *Descripcion* |
| :---- | :---- | :---- | :---- |
| *infractionId* | string (UUID) | si | Identificador de la infracción.  |
| *offenderNationalId* | string | no | DNI del conductor, si pudo ser identificado.  |
| *licensePlate* | string | si | Patente del vehículo.  |
| *infractionType* | string (enum) | si  | Tipo de infracción.  |
| *location* | object | si | Ubicación del hecho.  |
| *evidenceUrl* | array\<string\> | si | Evidencias asociadas.  |
| *inspectorId* | string | si | Inspector responsable.  |
| *dateTime* | datetime | si | Fecha y hora del registro.  |
| *baseAmount* | number | si | Importe base calculado al momento de labrar el acta.  |
| *aggravatingFactors* | array\<string\> | si | Agravantes aplicados en el momento del registro. Puede ser un arreglo vacío.  |
| *finalAmount* | number | si | Importe final luego de aplicar agravantes, al momento del registro.  |

**Ejemplo:**

{  
  "eventId": "af0a54da-678d-4904-a7ea-c195663ff649",  
  "eventType": "infractionRegistered",  
  "occurredAt": "2026-08-24T13:47:00-03:00",  
  "sourceModule": "transito",  
  "data": {  
    "infractionId": "93fd4382-2343-4f3f-a127-9328b29fc520",  
    "offenderNationalId": null,  
    "licensePlate": "AB123CD",  
    "infractionType": "ILLEGAL\_PARKING",  
    "location": {  
      "street": "Av. Cabildo",  
      "crossStreet": "Juramento",  
      "latitude": \-34.5621,  
      "longitude": \-58.4563  
    },  
    "evidenceUrl": \[  
      "https://storage.example/evidence/infraction-001.jpg"  
    \],  
    "inspectorId": "INS-245",  
    "dateTime": "2026-08-24T13:45:00-03:00",  
    "baseAmount": 50000,  
    "aggravatingFactors": \[\],  
    "finalAmount": 50000  
  }  
}

* ### infractionAppealed {

  * ***infractionId***: string;  
    * ***appellantNationalId***: string;  
    * ***appealReason***: string;  
    * ***appealDate***: datetime;  
    * ***attachedDocuments***: array\<string\>;  
    * ***appealId***: string (UUID) → identificar indivdualmente cada apelacion 

  }

| *Campo* | *Tipo* | *Req.* | *Descripcion* |
| :---- | :---- | :---- | :---- |
| *infractionId* | string (UUID) | si | Infracción apelada.  |
| *appellantNationalId* | string | si | DNI del ciudadano que apela.  |
| *appealReason* | string | si | Motivo de la apelación.  |
| *appealDate* | datetime | si | Fecha de presentación.  |
| *attachedDocuments*  | array\<string\>  | si | Documentos adjuntos.  |
| *appealId*  | string (UUID)  | si | Identificador único de la apelación.  |

**Ejemplo:**

{  
  "eventId": "703216aa-e394-43f4-92af-b0be75b09638",  
  "eventType": "infractionAppealed",  
  "occurredAt": "2026-08-26T09:15:00-03:00",  
  "sourceModule": "transito",  
  "data": {  
    "infractionId": "93fd4382-2343-4f3f-a127-9328b29fc520",  
    "appellantNationalId": "40123456",  
    "appealReason": "El vehículo contaba con autorización de estacionamiento.",  
    "appealDate": "2026-08-26T09:14:00-03:00",  
    "attachedDocuments": \[  
      \],  
    "appealId": "1a079b21-b294-4395-ac69-566cb16f028e"  
  }  
}

* ### trafficOperationCreated {

  * ***operationId***: string (UUID);  
    * ***type***: string (enum);  
    * ***zone***: string;  
    * ***startDateTime***: datetime;  
    * ***assignedInspectors***: array\<string\>;  
    * ***assignedUnits***: array\<string\>;

  }


| *Campo* | *Tipo* | *Req.* | *Descripcion* |
| :---- | :---- | :---- | :---- |
| *operationId* | string (UUID) | si | Identificador del operativo.  |
| *type* | string (enum) | si | Tipo de operativo.  |
| *zone* | string | si | Zona donde se realizará.  |
| *startDateTime* | datetime | si | Inicio del operativo.  |
| *assignedInspectors* | array\<string\>  | si | Inspectores asignados.  |
| *assignedUnits* | array\<string\>  | si | Móviles asignados.  |
| *ticketId* | integer / null | No | Ticket de M2 asociado, si el operativo se originó por un reclamo. |

**Ejemplo:**

{  
  "eventId": "510de8bb-b733-4537-b60c-da7629e843af",  
  "eventType": "trafficOperationCreated",  
  "occurredAt": "2026-08-24T08:30:00-03:00",  
  "sourceModule": "transito",  
  "data": {  
    "operationId": "07a9df62-453c-424d-968f-281a272948c6",  
    "type": "ALCOHOL\_CONTROL",  
    "zone": "Palermo",  
    "startDateTime": "2026-08-24T22:00:00-03:00",  
    "assignedInspectors": \[\],  
    "assignedUnits": \[\],  
    "ticketId": null  
  }  
}

* ### vehicleImpounded {

  * **impoundmentId:** string (UUID);  
    * **licensePlate:** string;  
    * **ownerNationalId:** string;  
    * **reason:** string;  
    * **depotLocation:** string;  
    * **entryDateTime:** datetime;  
    * **originType:** "infraction" | "operation";  → fue retenido por una infracción o por un operativo  
    * **originId:** string ;  → identifica específicamente la infracción o el operativo que originó la retención.                

    }

    

| *Campo* | *Tipo* | *Req.* | *Descripcion* |
| :---- | :---- | :---- | :---- |
| *impoundmentId* | string (UUID) | si  | Identificador de la retención. |
| *licensePlate* | string | si | Patente del vehículo. |
| *ownerNationalId* | string | si | Identificación del titular.  |
| *reason* | string | si | Motivo de la retención.  |
| *depotLocation* | string | si | Depósito donde queda alojado.  |
| *entryDateTime* | datetime | si | Fecha y hora de ingreso.  |
| *originType* | “infraction”/”operation” | si | Tipo de origen de la retención.  |
| *originId* | string | si | Identificador de la infracción u operativo de origen.  |

**Ejemplo:**

{  
  "eventId": "389fd813-f83c-447c-8544-806ae1ae0691",  
  "eventType": "vehicleImpounded",  
  "occurredAt": "2026-08-24T23:20:00-03:00",  
  "sourceModule": "transito",  
  "data": {  
    "impoundmentId": "6fae9a12-ad38-46c9-9182-b5e479c67818",  
    "licensePlate": "AD987GH",  
    "ownerNationalId": "35456789",  
    "reason": "Alcoholemia positiva",  
    "depotLocation": "Depósito Norte",  
    "entryDateTime": "2026-08-24T23:15:00-03:00",  
    "originType": "operation",  
    "originId": "07a9df62-453c-424d-968f-281a272948c6"  
  }  
}

* ### vehicleReleased {

  * **impoundmentId:** string (UUID);  
    * **releaseDateTime**: datetime;  
    * **verifiedRequirements:** array\<string\>;  
    * **towingAndStorageFee**: number;  
    * **paymentReference:** string;	→ para saber con que pago se valido la liberacion.          
    * **releasedBy:** string;             → identifica al usuario que autorizó o realizó la liberación. 

  }


| *Campo* | *Tipo* | *Req.* | *Descripcion* |
| :---- | :---- | :---- | :---- |
| *impoundmentId* | string (UUID) | si | Retención que se libera.  |
| *releaseDateTime* | datetime | si | Fecha y hora de liberación. |
| *verifiedRequirements* | array\<string\> | si | Requisitos validados. |
| *towingAndStorageFee* | number | si | Total de acarreo y estadía. |
| *paymentReference* | string / null | cond. | Referencia del pago utilizado para liberar. Obligatorio si `towingAndStorageFee > 0`. |
| *releasedBy* | string | si | Usuario que realizó/autorizó la liberación. |

**Ejemplo:**

{  
  "eventId": "bc795c7f-280f-47f6-8751-c704476bec90",  
  "eventType": "vehicleReleased",  
  "occurredAt": "2026-08-27T16:00:00-03:00",  
  "sourceModule": "transito",  
  "data": {  
    "impoundmentId": "6fae9a12-ad38-46c9-9182-b5e479c67818",  
    "releaseDateTime": "2026-08-27T15:58:00-03:00",  
    "verifiedRequirements": \[  
      "DOCUMENTATION\_OK",  
      "DEBT\_SETTLED",  
      "TOWING\_AND\_STORAGE\_PAID"  
    \],  
    "towingAndStorageFee": 85000,  
    "paymentReference": "PAY-98451",  
    "releasedBy": "USR-DEP-15"  
  }  
}

---

# **Eventos que RECIBIMOS:**

* **De Obras y Ambiente:**  
  * streetClosureRequested (Necesario tener un campo para saber qué módulo pide cerrar la calle) {requestingModule: "Obras" | "Ambiente" }

| Código (M-number) | Nombre completo | Envelope (lowercase) |
| :---- | :---- | :---- |
| M3 | Obras | obras |
| M6 | Ambiente | ambiente |

Recomendación de implementación: usen el **código M-number como clave canónica interna** (es como probablemente lo identifique el Core/hub de eventos), y estas otras dos son solo "vistas" de esa misma clave para distintos contextos:

* `data.sourceModule` (el que llega en `streetClosureRequested`) → usa el M-number.  
* El envelope externo (`sourceModule: "obras"`) → usa el nombre lowercase.  
* Lo que ustedes publican como `requestingModule` → usa nombre con mayúscula inicial (`"Obras"`).

**UNIFICADO** 

| Campo | Tipo | Req. | Descripción |
| :---- | ----- | ----- | ----- |
| closureRequestId | string (UUID) | Sí | Identificador único de la solicitud. |
| sourceModule | string | Sí | Módulo que origina la solicitud: M3 o M6. |
| sourceRef | string | Sí | Referencia al elemento que originó el corte, por ejemplo una orden de trabajo o intervención. |
| reason | string | Sí | Motivo del corte. |
| affectedSections | array | Sí | Calles o tramos afectados. |
| requestedFrom | datetime | Sí | Inicio solicitado. |
| requestedTo | datetime | Sí | Fin solicitado. |
| closureType | string / enum | No | Tipo de corte, si aplica. |
| requestedAt | datetime | Sí | Momento en que se generó la solicitud. |

**ejemplo de lo que recibimos del ambiente**

{  
  "eventId": "89cb2fd1-1af7-45d5-a2ab-c538d14f5210",  
  "eventType": "streetClosureRequested",  
  "occurredAt": "2026-09-01T16:00:00-03:00",  
  "sourceModule": "ambiente",  
  "data": {  
    "closureRequestId": "REQ-M6-301",  
    "sourceModule": "M6",  
    "sourceRef": "INT-025",  
    "reason": "Poda de árboles que requiere interrupción de la circulación",  
    "affectedSections": \[  
      "Av. Rivadavia entre Av. La Plata y Muñiz"  
    \],  
    "requestedFrom": "2026-09-02T08:00:00-03:00",  
    "requestedTo": "2026-09-02T12:00:00-03:00",  
    "closureType": "PARTIAL",  
    "requestedAt": "2026-09-01T15:58:00-03:00"  
  }  
}

**ejemplo de lo que recibimos del obras** 

{  
  "eventId": "971ed407-fec8-4899-a616-c31f49391ca0",  
  "eventType": "streetClosureRequested",  
  "occurredAt": "2026-09-01T17:00:00-03:00",  
  "sourceModule": "obras",  
  "data": {  
    "closureRequestId": "REQ-M3-501",  
    "sourceModule": "M3",  
    "sourceRef": "WO-1050",  
    "reason": "Reparación de calzada",  
    "affectedSections": \[  
      "Av. Cabildo entre Juramento y Mendoza"  
    \],  
    "requestedFrom": "2026-09-04T08:00:00-03:00",  
    "requestedTo": "2026-09-04T18:00:00-03:00",  
    "closureType": "TOTAL",  
    "requestedAt": "2026-09-01T16:58:00-03:00"  
  }  
}

* urbanServiceScheduled (M6) 

| Campo | Tipo | Req. | Descripción |
| ----- | ----- | ----- | ----- |
| serviceId | string | Sí | Identificador del servicio. |
| serviceTypeCode | string | Sí | Código del tipo de servicio. |
| category | string | Sí | Categoría del servicio. |
| mode | enum | Sí | Modalidad: ROUTE o POINT. |
| zoneIds | array | Sí | Zonas involucradas. |
| routeId | string / null | No | Ruta asociada, si corresponde. |
| targetRef | string / null | No | Referencia específica, si corresponde. |
| scheduledDate | date | Sí | Día programado. |
| timeWindow | object | Sí | Franja horaria { from, to }. |
| crewId | string / null | No | Cuadrilla asignada. |
| vehicleId | string / null | No | Vehículo asignado. |
| origin | enum | Sí | SCHEDULED, TICKET, INSPECTION o INTERNAL. |
| ticketId | integer / null | No | Ticket de M2 asociado, si existe. |

**ejemplo:**

{  
  "eventId": "6a74cb52-3298-4fd9-a81d-275d6ef12910",  
  "eventType": "urbanServiceScheduled",  
  "occurredAt": "2026-09-01T14:00:00-03:00",  
  "sourceModule": "ambiente",  
  "data": {  
    "serviceId": "SER-104",  
    "serviceTypeCode": "STREET\_CLEANING",  
    "category": "CLEANING",  
    "mode": "ROUTE",  
    "zoneIds": \[  
      "ZONE-05",  
      "ZONE-06"  
    \],  
    "routeId": "ROUTE-12",  
    "targetRef": null,  
    "scheduledDate": "2026-09-03",  
    "timeWindow": {  
      "from": "08:00",  
      "to": "12:00"  
    },  
    "crewId": "CREW-08",  
    "vehicleId": "VEH-15",  
    "origin": "SCHEDULED",  
    "ticketId": null  
  }  
}

* treeRiskDetected (M6)

| Campo | Tipo | Req. | Descripción |
| ----- | ----- | ----- | ----- |
| treeId | string | Sí | Identificador del árbol relevado. |
| surveyCode | string | Sí | Código del relevamiento realizado. |
| species | string | Sí | Especie del árbol. |
| zoneId | string | Sí | Identificador de la zona donde se encuentra. |
| location | object | Sí | Ubicación del árbol. |
| riskLevel | enum | Sí | Nivel de riesgo detectado: HIGH o CRITICAL. |
| riskType | string | Sí | Tipo de riesgo detectado. |
| healthStatus | enum | Sí | Estado del árbol: HEALTHY, DECLINING o DEAD. |
| suggestedIntervention | enum | Sí | Intervención sugerida: PRUNING, FELLING, TREATMENT o MONITORING. |
| requiresStreetClosure | boolean | Sí | Indica si se requiere un corte de calle. |
| requiresPublicWorks | boolean | Sí | Indica si se requiere intervención del módulo de Obras. |
| surveyedAt | datetime | Sí | Fecha y hora en que se realizó el relevamiento. |

**Ejemplo**

{  
  "eventId": "34e23c79-851d-44e0-a83e-47d5bd87e102",  
  "eventType": "treeRiskDetected",  
  "occurredAt": "2026-09-01T11:30:00-03:00",  
  "sourceModule": "ambiente",  
  "data": {  
    "treeId": "TREE-101",  
    "surveyCode": "SURVEY-2026-150",  
    "species": "Tipa",  
    "zoneId": "ZONE-05",  
    "location": {  
      "street": "Av. Rivadavia",  
      "number": "4500",  
      "neighborhoodId": "NEIGH-08",  
      "lat": \-34.6175,  
      "lng": \-58.4352  
    },  
    "riskLevel": "HIGH",  
    "riskType": "BRANCH\_FALL",  
    "healthStatus": "DECLINING",  
    "suggestedIntervention": "PRUNING",  
    "requiresStreetClosure": true,  
    "requiresPublicWorks": false,  
    "surveyedAt": "2026-09-01T11:25:00-03:00"  
  }  
}

* treePruningScheduled (M6)

| *Campo* | *Tipo* | *Req.* | *Descripción* |
| ----- | ----- | ----- | ----- |
| *interventionId* | string / UUID | Sí | Identificador de la intervención de poda. |
| *serviceId* | string / UUID | Sí | Identificador del servicio relacionado. |
| *interventionType* | string / enum | Sí | Tipo de intervención de arbolado programada. |
| *treeIds* | array | Sí | Identificadores de los árboles involucrados. |
| *zoneId* | string | Sí | Identificador de la zona donde se realizará la intervención. |
| *location* | object | Sí | Ubicación donde se llevará a cabo la poda. |
| *scheduledDate* | date / datetime | Sí | Fecha programada para la intervención. |
| *timeWindow* | object | Sí | Franja horaria prevista para realizar el trabajo. |
| *timeWindow.from* | time / datetime | Sí | Inicio de la franja horaria. |
| *timeWindow.to* | time / datetime | Sí | Fin de la franja horaria. |
| *crewId* | string | Sí | Identificador de la cuadrilla asignada. |
| *requiresStreetClosure* | boolean | Sí | Indica si la intervención requiere un corte de calle. |

**Ejemplo**

{  
  "eventId": "550e8400-e29b-41d4-a716-446655440000",  
  "eventType": "treePruningScheduled",  
  "occurredAt": "2026-08-30T10:00:00-03:00",  
  "sourceModule": "ambiente",  
  "data": {  
    "interventionId": "INT-025",  
    "serviceId": "SER-104",  
    "interventionType": "PRUNING",  
    "treeIds": \[  
      "TREE-101",  
      "TREE-102",  
      "TREE-103"  
    \],  
    "zoneId": "ZONE-05",  
    "location": {  
      "street": "Av. Rivadavia",  
      "crossStreet": "Av. La Plata"  
    },  
    "scheduledDate": "2026-09-02",  
    "timeWindow": {  
      "from": "08:00",  
      "to": "12:00"  
    },  
    "crewId": "CREW-08",  
    "requiresStreetClosure": true  
  }  
}

* workOrderScheduled (M3) → para conocer con anticipación trabajos programados por Obras que puedan afectar la circulación y evaluar si requieren coordinación o un corte de calle. 

| *Campo* | *Tipo* | *Req.* | *Descripción* |
| ----- | ----- | ----- | ----- |
| *workOrderId* | string | Sí | Identificador de la orden de trabajo programada. |
| *sourceRequestId* | string | Sí | Identificador de la solicitud que dio origen a la orden de trabajo. |
| *scheduledDate* | datetime | Sí | Fecha y hora programada para realizar el trabajo. |
| *estimatedDuration* | number | Sí | Duración estimada de la tarea programada. |

  * workOrderCompleted (M3) → para conocer que una intervención de Obras ya terminó y actualizar la información asociada a circulación, cortes o desvíos cuando corresponda. 

| *Campo* | *Tipo* | *Req.* | *Descripción* |
| ----- | ----- | ----- | ----- |
| *workOrderId* | string | Sí | Identificador de la orden de trabajo finalizada. |
| *sourceRequestId* | string | Sí | Identificador de la solicitud que originó la orden de trabajo. |
| *completedAt* | datetime | Sí | Fecha y hora en que se completó efectivamente el trabajo. |
| *outcome* | string / enum | Sí | Resultado de la ejecución de la orden de trabajo. |
| *consumedMaterials* | array | No | Materiales utilizados durante la ejecución del trabajo. |
| *evidence* | array | No | Evidencias asociadas a la finalización, como fotografías o documentos. |

* **De Atención Ciudadana:**  
  * ticketUpdated → M7 consume ticketUpdated y procesa el evento únicamente cuando responsibleAreaId corresponde al área de Tránsito. Las demás actualizaciones se ignoran.

| *Campo* | *Tipo* | *Req.* | *Descripcion* |
| :---- | :---- | :---- | :---- |
| *ticketId* | integer | si | Identificador técnico del ticket en M2. |
| *citizenId* | integer / null | si | Identificador del ciudadano. Es null si el ticket es anónimo. |
| *isAnonymous* | boolean | si | Indica si el ticket fue creado de manera anónima. |
| *responsibleAreaId* | string | si | Área responsable del ticket. M7 debe actuar cuando corresponda a Tránsito.  |
| *updateType* | enum | si | Tipo de actualización publicada por M2. |
| *currentStatus* | ticketStatus | si | Estado actual del ticket determinado por M2. |
| *currentPriority* | priority | si | Prioridad vigente del ticket. |
| *publicMessage* | string / null | no | Mensaje público asociado a la actualización.  |
| *progress* | integer / null | no | Avance estimado, cuando corresponda |
| *details* | object / null | cond | Información específica según el updateType. |
| *attachments* | array\<attachment\>  | no | Archivos nuevos o relevantes asociados a esa actualización. |
| *updatedAt* | datetime | si | Fecha y hora en la que M2 persistió la actualización.  |

**Ejemplo:**

{  
  "specVersion": "1.0",  
  "eventId": "98c03826-7a01-4f20-a8eb-3507883ffcf4",  
  "eventType": "ticketUpdated",  
  "occurredAt": "2026-08-24T17:10:00-03:00",  
  "producer": {  
    "moduleId": "M2",  
    "service": "help-center-api"  
  },  
  "subject": "tickets/2045",  
  "data": {  
    "ticketId": 2045,  
    "citizenId": 4012,  
    "isAnonymous": false,  
    "responsibleAreaId": "M7",  
    "updateType": "ROUTED",  
    "currentStatus": "ROUTED",  
    "currentPriority": "HIGH",  
    "publicMessage": "El ticket fue derivado al área de Tránsito.",  
    "progress": null,  
    "details": {  
      "routing": {  
        "requestType": {  
          "id": 31,  
          "name": "Vehículo mal estacionado"  
        },  
        "ticketType": "COMPLAINT",  
        "summary": "Vehículo estacionado en espacio reservado",  
        "description": "Hay un vehículo estacionado en un espacio reservado.",  
        "formData": {  
          "licensePlate": "AB123CD"  
        },  
        "location": {  
          "street": "Av. Cabildo",  
          "streetNumber": "2500",  
          "latitude": \-34.56,  
          "longitude": \-58.45  
        },  
        "resolutionDueAt": "2026-08-28T18:00:00-03:00",  
        "escalation": {  
          "active": false,  
          "reasonCode": null,  
          "escalatedAt": null  
        }  
      }  
    },  
    "attachments": \[\],  
    "updatedAt": "2026-08-24T17:09:30-03:00"  
  }  
}

* **De Rentas:**  
  * paymentRegistered

| Campo | Tipo | Req. | Descripción |
| ----- | ----- | ----- | ----- |
| paymentId | string | Sí | Identificador del pago registrado. |
| originType | enum | Sí | Origen de la deuda. Para M7: TRAFFIC\_INFRACTION. |
| originId | string | Sí | Identificador de la infracción asociada. |
| amountPaid | number | Sí | Importe pagado. |
| paidAt | datetime | Sí | Fecha y hora del pago. |
| remainingBalance | number | Sí | Saldo restante luego del pago. |

**ejemplo:**

{  
  "eventId": "uuid-v4",  
  "eventType": "paymentRegistered",  
  "occurredAt": "2026-09-05T10:30:00-03:00",  
  "sourceModule": "rentas",  
  "data": {  
    "paymentId": "pay-09200",  
    "originType": "TRAFFIC\_INFRACTION",  
    "originId": "inf-00850",  
    "amountPaid": 50000.00,  
    "paidAt": "2026-09-05T10:30:00-03:00",  
    "remainingBalance": 25000.00  
  }  
}

* debtSettled 

| Campo | Tipo | Req. | Descripción |
| ----- | ----- | ----- | ----- |
| debtId | string | Sí | Identificador de la deuda. |
| originType | enum | Sí | Tipo de origen de la deuda. |
| originId | string | Sí | Infracción de M7 asociada. |
| settledAt | datetime | Sí | Fecha y hora en que la deuda quedó totalmente cancelada. |

**ejemplo**

{  
  "eventId": "uuid-v4",  
  "eventType": "debtSettled",  
  "occurredAt": "2026-09-10T09:15:00-03:00",  
  "sourceModule": "rentas",  
  "data": {  
    "debtId": "debt-03100",  
    "originType": "TRAFFIC\_INFRACTION",  
    "originId": "inf-00850",  
    "settledAt": "2026-09-10T09:15:00-03:00"  
  }  
}

* paymentReversed 

| Campo | Tipo | Req. | Descripción |
| ----- | ----- | ----- | ----- |
| paymentId | string | Sí | Identificador del pago revertido. |
| originType | enum | Sí | Tipo de origen de la deuda. |
| originId | string | Sí | Infracción asociada. |
| reversedAmount | number | Sí | Importe revertido. |
| reversedAt | datetime | Sí | Fecha y hora efectiva de la reversión. |
| remainingBalance | number | Sí | Nuevo saldo de la deuda. |
| reason | string | Sí | Motivo de la reversión. |

**ejemplo**

{  
  "eventId": "uuid-v4",  
  "eventType": "paymentReversed",  
  "occurredAt": "2026-09-12T12:00:00-03:00",  
  "sourceModule": "rentas",  
  "data": {  
    "paymentId": "pay-09200",  
    "originType": "TRAFFIC\_INFRACTION",  
    "originId": "inf-00850",  
    "reversedAmount": 75000.00,  
    "reversedAt": "2026-09-12T12:00:00-03:00",  
    "remainingBalance": 75000.00,  
    "reason": "Reversión autorizada"  
  }  
}

---

# **Eventos internos**

**infractionAnulled  – es interno**

* infractionAnulled {  
  *     infractionId: string (UUID);  
  *     appealId: string (UUID);  
  *     annulmentReason: string;  
  *     annulledBy: string;  
  *     annulmentDateTime: datetime;

  }

| Campo | Tipo | Req. | Descripción |
| ----- | ----- | ----- | ----- |
| infractionId | string (UUID) | Sí | Identificador de la infracción anulada. |
| appealId | string (UUID) / null | Cond. | Identificador del descargo, si la anulación se produjo por una apelación aceptada. |
| annulmentReason | string | Sí | Motivo por el cual se anuló la infracción. |
| annulledBy | string | Sí | Identificador del responsable que autorizó la anulación. |
| annulmentDateTime | datetime | Sí | Fecha y hora en que se realizó la anulación. |

**ejemplo**

{  
  "eventId": "1511bd04-05a0-466d-bfe0-4283ca9e53ef",  
  "eventType": "infractionAnulled",  
  "occurredAt": "2026-08-30T17:10:00-03:00",  
  "sourceModule": "transito",  
  "data": {  
    "infractionId": "93fd4382-2343-4f3f-a127-9328b29fc520",  
    "appealId": "75c37f25-72f8-4975-8ae7-b588eeaf9362",  
    "annulmentReason": "Descargo aceptado.",  
    "annulledBy": "SUP-015",  
    "annulmentDateTime": "2026-08-30T17:09:00-03:00"  
  }  
}

* streetClosureActivated

| Campo | Tipo | Req. | Descripción |
| ----- | ----- | ----- | ----- |
| streetClosureId | string (UUID) | Sí | Identificador único del corte que se activó. |
| sourceRequestId | string | Sí | Identificador de la solicitud original que generó el corte. |
| sourceModule | string | Sí | Módulo que originó la solicitud (`M3` o `M6`). |
| activatedAt | datetime | Sí | Fecha y hora efectiva en que comenzó el corte. |
| activatedBy | string | Sí | Identificador del operador que activó el corte. |

**ejemplo:**

{  
  "eventId": "55b49ad4-a304-4755-a760-9efea087a7bb",  
  "eventType": "streetClosureActivated",  
  "occurredAt": "2026-09-02T08:00:00-03:00",  
  "sourceModule": "transito",  
  "data": {  
    "streetClosureId": "91ac5632-3186-48fa-a735-d55d918de202",  
    "sourceRequestId": "REQ-301",  
    "sourceModule": "M3",  
    "activatedAt": "2026-09-02T08:00:00-03:00",  
    "activatedBy": "OP-025"  
  }  
}
