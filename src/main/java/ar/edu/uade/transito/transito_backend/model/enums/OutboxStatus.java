package ar.edu.uade.transito.transito_backend.model.enums;

/** Estado de un evento del outbox transaccional (DEF-14 / DEF-04). */
public enum OutboxStatus {
    /** Guardado junto con la transacción de negocio; falta publicarlo (o reintentarlo). */
    PENDING,
    /** Entregado al broker (o al publicador de log en el perfil sin broker). */
    PUBLISHED,
    /** Se agotaron los reintentos; queda registrado para revisión manual. */
    FAILED
}
