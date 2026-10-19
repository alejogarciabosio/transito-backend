package ar.edu.uade.transito.transito_backend.service;

import java.time.OffsetDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "street-closures.expiration-enabled", havingValue = "true", matchIfMissing = true)
public class StreetClosureExpirationScheduler {

    private final StreetClosureService streetClosureService;

    @Scheduled(fixedDelayString = "${street-closures.expiration-check-ms:60000}")
    public void expireDueClosures() {
        streetClosureService.expireDueClosures(OffsetDateTime.now());
    }
}
