package ar.edu.uade.transito.transito_backend.service;

import java.time.OffsetDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/** Ejecuta el cierre automático solo en ambientes donde la regla fue habilitada. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "street-closures.expiration", name = "enabled", havingValue = "true")
public class StreetClosureExpirationScheduler {

    private final StreetClosureService streetClosureService;

    @Scheduled(fixedDelayString = "${street-closures.expiration.fixed-delay-ms:60000}")
    public void finishExpiredClosures() {
        streetClosureService.finishExpired(OffsetDateTime.now());
    }
}
