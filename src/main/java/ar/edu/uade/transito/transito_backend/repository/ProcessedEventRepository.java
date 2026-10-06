package ar.edu.uade.transito.transito_backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.uade.transito.transito_backend.model.ProcessedEvent;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
