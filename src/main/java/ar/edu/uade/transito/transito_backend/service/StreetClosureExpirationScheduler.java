package ar.edu.uade.transito.transito_backend.service;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "street-closures.expiration-enabled", havingValue = "true", matchIfMissing = true)
public class StreetClosureExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(StreetClosureExpirationScheduler.class);

    private final StreetClosureService streetClosureService;

    @Scheduled(fixedDelayString = "${street-closures.expiration-check-ms:60000}")
    public void expireDueClosures() {
        OffsetDateTime now = OffsetDateTime.now();
        // Cada corte en su propia transacción: un fallo en uno no revierte a los demás, y como los
        // eventos van al outbox en la misma transacción, nunca sale un evento de un corte revertido.
        for (UUID id : streetClosureService.findClosuresDueForExpiration(now)) {
            try {
                streetClosureService.expireClosure(id, now);
            } catch (RuntimeException ex) {
                log.warn("[street-closures] no se pudo vencer el corte {}: {}", id, ex.getClass().getSimpleName());
            }
        }
    }
}
