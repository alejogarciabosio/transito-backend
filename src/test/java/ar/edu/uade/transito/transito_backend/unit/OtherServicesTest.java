package ar.edu.uade.transito.transito_backend.unit;

import ar.edu.uade.transito.transito_backend.event.EventPublisher;
import ar.edu.uade.transito.transito_backend.exception.InvalidStateTransitionException;
import ar.edu.uade.transito.transito_backend.exception.ResourceNotFoundException;
import ar.edu.uade.transito.transito_backend.model.ParkingSession;
import ar.edu.uade.transito.transito_backend.model.ParkingTariff;
import ar.edu.uade.transito.transito_backend.model.StreetClosure;
import ar.edu.uade.transito.transito_backend.model.VehicleImpoundment;
import ar.edu.uade.transito.transito_backend.model.enums.ImpoundmentStatus;
import ar.edu.uade.transito.transito_backend.model.enums.ParkingTariffStatus;
import ar.edu.uade.transito.transito_backend.model.enums.StreetClosureStatus;
import ar.edu.uade.transito.transito_backend.repository.ParkingSessionRepository;
import ar.edu.uade.transito.transito_backend.repository.ParkingTariffRepository;
import ar.edu.uade.transito.transito_backend.repository.StreetClosureRepository;
import ar.edu.uade.transito.transito_backend.repository.VehicleImpoundmentRepository;
import ar.edu.uade.transito.transito_backend.service.ParkingTariffService;
import ar.edu.uade.transito.transito_backend.service.StreetClosureService;
import ar.edu.uade.transito.transito_backend.service.VehicleImpoundmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Reglas de servicios (unit)")
class OtherServicesTest {

    @ParameterizedTest(name = "TC-BE-SC-U01: desde {0} solo se puede aprobar/rechazar si REQUESTED")
    @EnumSource(value = StreetClosureStatus.class, names = "REQUESTED", mode = EnumSource.Mode.EXCLUDE)
    void closureApproveOnlyFromRequested(StreetClosureStatus from) {
        var repo = mock(StreetClosureRepository.class);
        var publisher = mock(EventPublisher.class);
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.of(StreetClosure.builder().streetClosureId(id).status(from).build()));
        var service = new StreetClosureService(repo, publisher);
        assertThatThrownBy(() -> service.approve(id, new StreetClosure())).isInstanceOf(InvalidStateTransitionException.class);
        assertThatThrownBy(() -> service.reject(id, "x")).isInstanceOf(InvalidStateTransitionException.class);
        verify(publisher, never()).publish(any());
    }

    @Test
    @DisplayName("TC-BE-IMP-U01: liberar un vehículo ya RELEASED → 409 sin evento")
    void impoundmentReleaseTwice() {
        var repo = mock(VehicleImpoundmentRepository.class);
        var publisher = mock(EventPublisher.class);
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.of(VehicleImpoundment.builder().impoundmentId(id).status(ImpoundmentStatus.RELEASED).build()));
        var service = new VehicleImpoundmentService(repo, publisher);
        assertThatThrownBy(() -> service.release(id, List.of(), null, null, "u")).isInstanceOf(InvalidStateTransitionException.class);
        verify(publisher, never()).publish(any());
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("TC-BE-PT-U01: borrar tarifa: sesiones cerradas no bloquean, una abierta sí")
    void tariffDelete() {
        var tariffs = mock(ParkingTariffRepository.class);
        var sessions = mock(ParkingSessionRepository.class);
        UUID tariffId = UUID.randomUUID();
        UUID zone = UUID.randomUUID();
        ParkingTariff tariff = ParkingTariff.builder().tariffId(tariffId).zoneId(zone).build();
        when(tariffs.findById(tariffId)).thenReturn(Optional.of(tariff));
        var service = new ParkingTariffService(tariffs, sessions);

        when(sessions.findByZoneId(zone)).thenReturn(List.of(ParkingSession.builder().endDateTime(LocalDateTime.now()).build()));
        service.delete(tariffId);
        verify(tariffs).save(tariff);

        // Caso independiente: una tarifa aún activa no puede eliminarse si hay sesiones abiertas.
        tariff.setStatus(ParkingTariffStatus.ACTIVE);
        when(sessions.findByZoneId(zone)).thenReturn(List.of(ParkingSession.builder().endDateTime(null).build()));
        assertThatThrownBy(() -> service.delete(tariffId)).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("TC-BE-SC-U02: un corte ACTIVE vencido se finaliza y publica streetClosureEnded")
    void finishesExpiredActiveClosures() {
        var repo = mock(StreetClosureRepository.class);
        var publisher = mock(EventPublisher.class);
        var expired = StreetClosure.builder()
                .streetClosureId(UUID.randomUUID())
                .closureRequestId("REQ-1")
                .status(StreetClosureStatus.ACTIVE)
                .endDate(OffsetDateTime.now().minusMinutes(1))
                .build();
        when(repo.findByStatusAndEndDateLessThanEqual(org.mockito.ArgumentMatchers.eq(StreetClosureStatus.ACTIVE), any()))
                .thenReturn(List.of(expired));
        when(repo.findById(expired.getStreetClosureId())).thenReturn(Optional.of(expired));
        when(repo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        int finished = new StreetClosureService(repo, publisher).finishExpired(OffsetDateTime.now());

        assertThat(finished).isEqualTo(1);
        assertThat(expired.getStatus()).isEqualTo(StreetClosureStatus.FINISHED);
        verify(publisher).publish(any());
    }
}
