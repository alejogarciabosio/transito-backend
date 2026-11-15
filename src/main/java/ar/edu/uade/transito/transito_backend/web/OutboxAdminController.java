package ar.edu.uade.transito.transito_backend.web;

import ar.edu.uade.transito.transito_backend.model.OutboxEvent;
import ar.edu.uade.transito.transito_backend.model.enums.OutboxStatus;
import ar.edu.uade.transito.transito_backend.repository.OutboxEventRepository;
import ar.edu.uade.transito.transito_backend.web.dto.OutboxEventResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Registro de eventos publicados (DEF-04), solo lectura: lo que quedó en el outbox y su estado.
 * Más recientes primero; nunca devuelve el payload.
 */
@RestController
@RequestMapping("/api/v1/admin/outbox")
@RequiredArgsConstructor
public class OutboxAdminController {

    private static final int MAX_LIMIT = 200;

    private final OutboxEventRepository repository;

    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'AUDITOR')")
    @GetMapping
    @Transactional(readOnly = true)
    public List<OutboxEventResponse> list(@RequestParam(required = false) OutboxStatus status,
                                          @RequestParam(defaultValue = "50") int limit) {
        PageRequest page = PageRequest.of(0, Math.max(1, Math.min(limit, MAX_LIMIT)));
        List<OutboxEvent> rows = status == null
                ? repository.findAllByOrderByCreatedAtDesc(page)
                : repository.findByStatusOrderByCreatedAtDesc(status, page);
        return rows.stream().map(OutboxEventResponse::from).toList();
    }
}
