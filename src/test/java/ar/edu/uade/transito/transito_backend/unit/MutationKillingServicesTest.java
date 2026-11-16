// Autor: revisado por el equipo (asistido por IA)
package ar.edu.uade.transito.transito_backend.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import ar.edu.uade.transito.transito_backend.event.EventEnvelope;
import ar.edu.uade.transito.transito_backend.event.EventPublisher;
import ar.edu.uade.transito.transito_backend.exception.InvalidRequestException;
import ar.edu.uade.transito.transito_backend.exception.InvalidStateTransitionException;
import ar.edu.uade.transito.transito_backend.exception.ResourceNotFoundException;
import ar.edu.uade.transito.transito_backend.listener.dto.RentasPaymentPayload;
import ar.edu.uade.transito.transito_backend.listener.handler.DebtSettledHandler;
import ar.edu.uade.transito.transito_backend.listener.handler.PaymentRegisteredHandler;
import ar.edu.uade.transito.transito_backend.listener.handler.PaymentReversedHandler;
import ar.edu.uade.transito.transito_backend.model.Infraction;
import ar.edu.uade.transito.transito_backend.model.Location;
import ar.edu.uade.transito.transito_backend.model.ParkingSession;
import ar.edu.uade.transito.transito_backend.model.ParkingTariff;
import ar.edu.uade.transito.transito_backend.model.ParkingZone;
import ar.edu.uade.transito.transito_backend.model.RoadAccident;
import ar.edu.uade.transito.transito_backend.model.VehicleImpoundment;
import ar.edu.uade.transito.transito_backend.model.enums.AccidentStatus;
import ar.edu.uade.transito.transito_backend.model.enums.DebtorIdType;
import ar.edu.uade.transito.transito_backend.model.enums.ImpoundmentStatus;
import ar.edu.uade.transito.transito_backend.model.enums.InfractionStatus;
import ar.edu.uade.transito.transito_backend.model.enums.ParkingTariffStatus;
import ar.edu.uade.transito.transito_backend.model.enums.ParkingZoneStatus;
import ar.edu.uade.transito.transito_backend.repository.InfractionPaymentEventRepository;
import ar.edu.uade.transito.transito_backend.repository.InfractionRepository;
import ar.edu.uade.transito.transito_backend.repository.ParkingSessionRepository;
import ar.edu.uade.transito.transito_backend.repository.ParkingTariffRepository;
import ar.edu.uade.transito.transito_backend.repository.ParkingZoneRepository;
import ar.edu.uade.transito.transito_backend.repository.RoadAccidentRepository;
import ar.edu.uade.transito.transito_backend.repository.VehicleImpoundmentRepository;
import ar.edu.uade.transito.transito_backend.service.InfractionPaymentEventService;
import ar.edu.uade.transito.transito_backend.service.InfractionService;
import ar.edu.uade.transito.transito_backend.service.ParkingSessionService;
import ar.edu.uade.transito.transito_backend.service.ParkingTariffService;
import ar.edu.uade.transito.transito_backend.service.ParkingZoneService;
import ar.edu.uade.transito.transito_backend.service.RoadAccidentService;
import ar.edu.uade.transito.transito_backend.service.VehicleImpoundmentService;
import ar.edu.uade.transito.transito_backend.service.command.UpdateRoadAccidentCommand;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Tests dirigidos a mutantes sobrevivientes de PIT: validan valores, estados y excepciones concretas. */
@DisplayName("Servicios y handlers: aserciones sobre estado y excepciones")
class MutationKillingServicesTest {

    private static final BigDecimal TEN = new BigDecimal("10.00");

    // ---------- ParkingSessionService / ParkingTariffService / ParkingZoneService ----------

    @Test
    void parkingSessionStartResetsPaymentStateAndPersistsOnActiveZone() {
        var sessions = mock(ParkingSessionRepository.class);
        var zones = mock(ParkingZoneRepository.class);
        var service = new ParkingSessionService(sessions, mock(InfractionRepository.class), zones, mock(InfractionService.class), java.time.Clock.systemUTC());
        UUID zoneId = UUID.randomUUID();
        when(zones.findById(zoneId)).thenReturn(Optional.of(ParkingZone.builder().zoneId(zoneId).status(ParkingZoneStatus.ACTIVE).build()));
        when(sessions.save(any(ParkingSession.class))).thenAnswer(i -> i.getArgument(0));
        var input = ParkingSession.builder().zoneId(zoneId).licensePlate("AB123CD").paid(true)
                .endDateTime(LocalDateTime.now()).paymentReference("REF-1").build();

        ParkingSession saved = service.start(input);

        assertThat(saved.isPaid()).isFalse();
        assertThat(saved.getEndDateTime()).isNull();
        assertThat(saved.getPaymentReference()).isNull();
        assertThat(saved.getLicensePlate()).isEqualTo("AB123CD");
    }

    @Test
    void parkingSessionStartRejectsMissingAndInactiveZones() {
        var sessions = mock(ParkingSessionRepository.class);
        var zones = mock(ParkingZoneRepository.class);
        var service = new ParkingSessionService(sessions, mock(InfractionRepository.class), zones, mock(InfractionService.class), java.time.Clock.systemUTC());
        UUID missing = UUID.randomUUID();
        UUID inactive = UUID.randomUUID();
        when(zones.findById(missing)).thenReturn(Optional.empty());
        when(zones.findById(inactive)).thenReturn(Optional.of(ParkingZone.builder().zoneId(inactive).status(ParkingZoneStatus.INACTIVE).build()));

        assertThatThrownBy(() -> service.start(ParkingSession.builder().zoneId(missing).build()))
                .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining(missing.toString());
        assertThatThrownBy(() -> service.start(ParkingSession.builder().zoneId(inactive).build()))
                .isInstanceOf(InvalidStateTransitionException.class).hasMessageContaining("no está activa");
        verify(sessions, never()).save(any());
    }

    @Test
    void parkingTariffCreateForcesActiveAndUpdateCopiesAllFields() {
        var repo = mock(ParkingTariffRepository.class);
        var service = new ParkingTariffService(repo, mock(ParkingSessionRepository.class));
        when(repo.save(any(ParkingTariff.class))).thenAnswer(i -> i.getArgument(0));

        ParkingTariff created = service.create(ParkingTariff.builder().status(ParkingTariffStatus.INACTIVE).pricePerHour(TEN).build());
        assertThat(created.getStatus()).isEqualTo(ParkingTariffStatus.ACTIVE);

        UUID id = UUID.randomUUID();
        var existing = ParkingTariff.builder().tariffId(id).status(ParkingTariffStatus.ACTIVE).pricePerHour(TEN)
                .validFrom(LocalDate.of(2026, 1, 1)).build();
        when(repo.findById(id)).thenReturn(Optional.of(existing));
        var changes = ParkingTariff.builder().pricePerHour(new BigDecimal("25.50"))
                .validFrom(LocalDate.of(2026, 3, 1)).validTo(LocalDate.of(2026, 12, 31)).build();

        ParkingTariff updated = service.update(id, changes);

        assertThat(updated.getPricePerHour()).isEqualByComparingTo("25.50");
        assertThat(updated.getValidFrom()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(updated.getValidTo()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    void parkingTariffUpdateRejectsInactiveTariff() {
        var repo = mock(ParkingTariffRepository.class);
        var service = new ParkingTariffService(repo, mock(ParkingSessionRepository.class));
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.of(ParkingTariff.builder().tariffId(id).status(ParkingTariffStatus.INACTIVE).build()));
        assertThatThrownBy(() -> service.update(id, new ParkingTariff()))
                .isInstanceOf(InvalidStateTransitionException.class).hasMessageContaining("inactiva");
        verify(repo, never()).save(any());
    }

    @Test
    void parkingZoneUpdateCopiesAllFieldsAndDeleteIsLogical() {
        var repo = mock(ParkingZoneRepository.class);
        var service = new ParkingZoneService(repo);
        UUID id = UUID.randomUUID();
        var existing = ParkingZone.builder().zoneId(id).name("vieja").boundaries("b0").allowedHours("0-1").status(ParkingZoneStatus.ACTIVE).build();
        when(repo.findById(id)).thenReturn(Optional.of(existing));
        when(repo.save(any(ParkingZone.class))).thenAnswer(i -> i.getArgument(0));

        ParkingZone updated = service.update(id, ParkingZone.builder().name("nueva").boundaries("b1").allowedHours("8-20")
                .status(ParkingZoneStatus.INACTIVE).build());
        assertThat(updated.getName()).isEqualTo("nueva");
        assertThat(updated.getBoundaries()).isEqualTo("b1");
        assertThat(updated.getAllowedHours()).isEqualTo("8-20");
        assertThat(updated.getStatus()).isEqualTo(ParkingZoneStatus.INACTIVE);

        service.create(existing);
        assertThat(existing.getStatus()).isEqualTo(ParkingZoneStatus.ACTIVE);
        service.delete(id);
        assertThat(existing.getStatus()).isEqualTo(ParkingZoneStatus.INACTIVE);
    }

    // ---------- InfractionService.update ----------

    @Test
    void infractionUpdateAppliesEachProvidedFieldAndKeepsTheOthers() {
        var repo = mock(InfractionRepository.class);
        var service = new InfractionService(repo, mock(EventPublisher.class));
        UUID id = UUID.randomUUID();
        Location oldLoc = Location.builder().street("Vieja").build();
        var existing = Infraction.builder().infractionId(id).licensePlate("OLD111").location(oldLoc)
                .evidenceUrl(List.of("old")).offenderNationalId("1").vehicleOwnerTaxId("2").debtorId("3")
                .debtorIdType(DebtorIdType.DNI).baseAmount(TEN).finalAmount(TEN)
                .aggravatingFactors(List.of("a")).status(InfractionStatus.PENDING_APPEAL).build();
        when(repo.findById(id)).thenReturn(Optional.of(existing));
        when(repo.save(any(Infraction.class))).thenAnswer(i -> i.getArgument(0));

        // Solo se cambian algunos campos: el resto debe conservarse.
        Infraction partial = new Infraction();
        partial.setLicensePlate("NEW222");
        partial.setEvidenceUrl(List.of("new"));
        Infraction result = service.update(id, partial);
        assertThat(result.getLicensePlate()).isEqualTo("NEW222");
        assertThat(result.getEvidenceUrl()).containsExactly("new");
        assertThat(result.getLocation()).isSameAs(oldLoc);
        assertThat(result.getOffenderNationalId()).isEqualTo("1");
        assertThat(result.getVehicleOwnerTaxId()).isEqualTo("2");
        assertThat(result.getDebtorId()).isEqualTo("3");
        assertThat(result.getBaseAmount()).isEqualByComparingTo(TEN);

        Location newLoc = Location.builder().street("Nueva").build();
        Infraction full = new Infraction();
        full.setLocation(newLoc);
        full.setOffenderNationalId("11");
        full.setVehicleOwnerTaxId("22");
        full.setDebtorId("33");
        full.setDebtorIdType(DebtorIdType.CUIT);
        full.setBaseAmount(new BigDecimal("5.00"));
        full.setAggravatingFactors(List.of("b"));
        full.setFinalAmount(new BigDecimal("7.00"));
        result = service.update(id, full);
        assertThat(result.getLocation()).isSameAs(newLoc);
        assertThat(result.getOffenderNationalId()).isEqualTo("11");
        assertThat(result.getVehicleOwnerTaxId()).isEqualTo("22");
        assertThat(result.getDebtorId()).isEqualTo("33");
        assertThat(result.getDebtorIdType()).isEqualTo(DebtorIdType.CUIT);
        assertThat(result.getBaseAmount()).isEqualByComparingTo("5.00");
        assertThat(result.getAggravatingFactors()).containsExactly("b");
        assertThat(result.getFinalAmount()).isEqualByComparingTo("7.00");
        assertThat(result.getLicensePlate()).isEqualTo("NEW222");
    }

    @Test
    void infractionUpdateAcceptsFinalEqualToBaseButRejectsLower() {
        var repo = mock(InfractionRepository.class);
        var service = new InfractionService(repo, mock(EventPublisher.class));
        UUID id = UUID.randomUUID();
        var existing = Infraction.builder().infractionId(id).baseAmount(TEN).finalAmount(TEN).status(InfractionStatus.PENDING_APPEAL).build();
        when(repo.findById(id)).thenReturn(Optional.of(existing));
        when(repo.save(any(Infraction.class))).thenAnswer(i -> i.getArgument(0));

        Infraction equal = new Infraction();
        equal.setFinalAmount(TEN);
        assertThat(service.update(id, equal).getFinalAmount()).isEqualByComparingTo(TEN);

        Infraction lower = new Infraction();
        lower.setFinalAmount(new BigDecimal("9.99"));
        assertThatThrownBy(() -> service.update(id, lower)).isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("menor");
    }

    // ---------- RoadAccidentService.update ----------

    private static RoadAccidentService accidentService(RoadAccidentRepository repo) {
        return new RoadAccidentService(repo, mock(EventPublisher.class));
    }

    @Test
    void roadAccidentUpdateAppliesChangesAndClearsDescriptionWhenNoDamage() {
        var repo = mock(RoadAccidentRepository.class);
        var service = accidentService(repo);
        UUID id = UUID.randomUUID();
        var existing = RoadAccident.builder().accidentId(id).status(AccidentStatus.PRELIMINARY)
                .accidentDateTime(LocalDateTime.of(2026, 1, 1, 10, 0)).involvesInfrastructureDamage(true)
                .damageDescription("poste").build();
        when(repo.findById(id)).thenReturn(Optional.of(existing));
        when(repo.save(any(RoadAccident.class))).thenAnswer(i -> i.getArgument(0));
        Location loc = Location.builder().street("Av. Siempreviva").build();
        LocalDateTime when = LocalDateTime.of(2026, 2, 2, 11, 30);

        RoadAccident r = service.update(id, new UpdateRoadAccidentCommand(when, loc, false, null, List.of(), List.of(), null));

        assertThat(r.getAccidentDateTime()).isEqualTo(when);
        assertThat(r.getLocation()).isSameAs(loc);
        assertThat(r.isInvolvesInfrastructureDamage()).isFalse();
        assertThat(r.getDamageDescription()).isNull();
        assertThat(r.getStatus()).isEqualTo(AccidentStatus.PRELIMINARY);
    }

    @Test
    void roadAccidentUpdateVerifiesAndEnforcesDamageDescription() {
        var repo = mock(RoadAccidentRepository.class);
        var service = accidentService(repo);
        UUID id = UUID.randomUUID();
        var existing = RoadAccident.builder().accidentId(id).status(AccidentStatus.PRELIMINARY).build();
        when(repo.findById(id)).thenReturn(Optional.of(existing));
        when(repo.save(any(RoadAccident.class))).thenAnswer(i -> i.getArgument(0));

        assertThatThrownBy(() -> service.update(id, new UpdateRoadAccidentCommand(null, null, true, "  ", null, null, null)))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("descripción del daño");

        RoadAccident r = service.update(id, new UpdateRoadAccidentCommand(null, null, true, "semáforo roto", null, null, AccidentStatus.VERIFIED));
        assertThat(r.getDamageDescription()).isEqualTo("semáforo roto");
        assertThat(r.getStatus()).isEqualTo(AccidentStatus.VERIFIED);

        // Una vez verificado ya no se puede modificar.
        assertThatThrownBy(() -> service.update(id, new UpdateRoadAccidentCommand(null, null, null, "x", null, null, null)))
                .isInstanceOf(InvalidStateTransitionException.class).hasMessageContaining("verificado");
    }

    @Test
    void roadAccidentUpdateRejectsTransitionBackToPreliminary() {
        var repo = mock(RoadAccidentRepository.class);
        var service = accidentService(repo);
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.of(RoadAccident.builder().accidentId(id).status(AccidentStatus.PRELIMINARY).build()));
        assertThatThrownBy(() -> service.update(id, new UpdateRoadAccidentCommand(null, null, null, null, null, null, AccidentStatus.PRELIMINARY)))
                .isInstanceOf(InvalidStateTransitionException.class);
        verify(repo, never()).save(any());
    }

    // ---------- VehicleImpoundmentService ----------

    @Test
    void impoundmentRegisterForcesImpoundedStatusAndPublishesEvent() {
        var repo = mock(VehicleImpoundmentRepository.class);
        var publisher = mock(EventPublisher.class);
        var service = new VehicleImpoundmentService(repo, publisher, java.time.Clock.systemUTC());
        when(repo.save(any(VehicleImpoundment.class))).thenAnswer(i -> i.getArgument(0));
        var input = VehicleImpoundment.builder().licensePlate("AA111BB").status(ImpoundmentStatus.RELEASED).paymentReference("X").build();

        VehicleImpoundment saved = service.register(input);

        assertThat(saved.getStatus()).isEqualTo(ImpoundmentStatus.IMPOUNDED);
        assertThat(saved.getPaymentReference()).isNull();
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<EventEnvelope> env = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(publisher).publish(env.capture());
        assertThat(env.getValue().getEventType()).isEqualTo("vehicleImpounded");
    }

    @Test
    void impoundmentReleaseBoundaryZeroFeeNeedsNoPaymentReferenceButPositiveFeeDoes() {
        var repo = mock(VehicleImpoundmentRepository.class);
        var service = new VehicleImpoundmentService(repo, mock(EventPublisher.class), java.time.Clock.systemUTC());
        UUID id = UUID.randomUUID();
        when(repo.save(any(VehicleImpoundment.class))).thenAnswer(i -> i.getArgument(0));
        when(repo.findById(id)).thenAnswer(i -> Optional.of(VehicleImpoundment.builder().impoundmentId(id).status(ImpoundmentStatus.IMPOUNDED).build()));

        VehicleImpoundment released = service.release(id, List.of("DNI"), BigDecimal.ZERO, null, "inspector");
        assertThat(released.getStatus()).isEqualTo(ImpoundmentStatus.RELEASED);
        assertThat(released.getReleasedBy()).isEqualTo("inspector");
        assertThat(released.getVerifiedRequirements()).containsExactly("DNI");

        assertThatThrownBy(() -> service.release(id, List.of("DNI"), TEN, " ", "i"))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("referencia de pago");
        assertThatThrownBy(() -> service.release(id, List.of("DNI"), new BigDecimal("-0.01"), "R", "i"))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("negativo");
        assertThatThrownBy(() -> service.release(id, List.of(), TEN, "R", "i"))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("requisito");
        assertThat(service.release(id, List.of("DNI"), TEN, "R", "i").getPaymentReference()).isEqualTo("R");
    }

    // ---------- InfractionPaymentEventService (validaciones) ----------

    private static final UUID INFRACTION = UUID.randomUUID();

    private static RentasPaymentPayload registered(String paymentId, String debtId, BigDecimal applied, BigDecimal remaining, OffsetDateTime paidAt) {
        return new RentasPaymentPayload(paymentId, debtId, "TRAFFIC_FINE", INFRACTION.toString(), INFRACTION.toString(),
                applied, null, remaining, null, null, "c1", null, paidAt, null, null);
    }

    private InfractionPaymentEventService paymentService(InfractionPaymentEventRepository events) {
        var infractions = mock(InfractionRepository.class);
        when(infractions.existsById(INFRACTION)).thenReturn(true);
        return new InfractionPaymentEventService(infractions, events);
    }

    @Test
    void paymentRegisteredIsPersistedWithAppliedAmountAndPaidAt() {
        var events = mock(InfractionPaymentEventRepository.class);
        OffsetDateTime paid = OffsetDateTime.parse("2026-05-01T10:00:00Z");
        paymentService(events).record("paymentRegistered",
                registered(UUID.randomUUID().toString(), UUID.randomUUID().toString(), TEN, BigDecimal.ZERO, paid));
        var captor = ArgumentCaptor.forClass(ar.edu.uade.transito.transito_backend.model.InfractionPaymentEvent.class);
        verify(events).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo(TEN);
        assertThat(captor.getValue().getOccurredAt()).isEqualTo(paid);
        assertThat(captor.getValue().getInfractionId()).isEqualTo(INFRACTION);
        assertThat(captor.getValue().getEventType()).isEqualTo("paymentRegistered");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "no-uuid"})
    void paymentRegisteredRejectsBadDebtAndPaymentIds(String bad) {
        var events = mock(InfractionPaymentEventRepository.class);
        var service = paymentService(events);
        String ok = UUID.randomUUID().toString();
        OffsetDateTime now = OffsetDateTime.now();
        assertThatThrownBy(() -> service.record("paymentRegistered", registered(ok, bad, TEN, BigDecimal.ZERO, now)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("debtId");
        assertThatThrownBy(() -> service.record("paymentRegistered", registered(bad, ok, TEN, BigDecimal.ZERO, now)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("paymentId");
        verify(events, never()).save(any());
    }

    @Test
    void paymentRegisteredRejectsBadAmountsAndMissingDate() {
        var events = mock(InfractionPaymentEventRepository.class);
        var service = paymentService(events);
        String p = UUID.randomUUID().toString();
        String d = UUID.randomUUID().toString();
        OffsetDateTime now = OffsetDateTime.now();
        assertThatThrownBy(() -> service.record("paymentRegistered", registered(p, d, BigDecimal.ZERO, BigDecimal.ZERO, now)))
                .hasMessageContaining("amountApplied");
        assertThatThrownBy(() -> service.record("paymentRegistered", registered(p, d, null, BigDecimal.ZERO, now)))
                .hasMessageContaining("amountApplied");
        assertThatThrownBy(() -> service.record("paymentRegistered", registered(p, d, TEN, new BigDecimal("-1"), now)))
                .hasMessageContaining("remainingBalance");
        assertThatThrownBy(() -> service.record("paymentRegistered", registered(p, d, TEN, null, now)))
                .hasMessageContaining("remainingBalance");
        assertThatThrownBy(() -> service.record("paymentRegistered", registered(p, d, TEN, BigDecimal.ZERO, null)))
                .hasMessageContaining("paidAt");
        verify(events, never()).save(any());
    }

    @Test
    void debtSettledValidatesConceptDateAndZeroBalances() {
        var events = mock(InfractionPaymentEventRepository.class);
        var service = paymentService(events);
        String d = UUID.randomUUID().toString();
        OffsetDateTime now = OffsetDateTime.now();
        java.util.function.BiFunction<String, BigDecimal, RentasPaymentPayload> make = (concept, outstanding) ->
                new RentasPaymentPayload(null, d, "TRAFFIC_FINE", INFRACTION.toString(), INFRACTION.toString(),
                        null, null, BigDecimal.ZERO, outstanding, concept, null, null, null, now, null);

        service.record("debtSettled", make.apply("TRAFFIC_INFRACTION", BigDecimal.ZERO));
        verify(events).save(any());
        assertThatThrownBy(() -> service.record("debtSettled", make.apply("OTHER", BigDecimal.ZERO)))
                .hasMessageContaining("conceptCode");
        assertThatThrownBy(() -> service.record("debtSettled", make.apply("TRAFFIC_INFRACTION", BigDecimal.ONE)))
                .hasMessageContaining("outstandingBalance");
        assertThatThrownBy(() -> service.record("debtSettled", make.apply("TRAFFIC_INFRACTION", null)))
                .hasMessageContaining("outstandingBalance");
        assertThatThrownBy(() -> service.record("debtSettled", new RentasPaymentPayload(null, d, "TRAFFIC_FINE",
                INFRACTION.toString(), INFRACTION.toString(), null, null, BigDecimal.ZERO, BigDecimal.ZERO,
                "TRAFFIC_INFRACTION", null, null, null, null, null))).hasMessageContaining("settledAt");
        assertThatThrownBy(() -> service.record("debtSettled", new RentasPaymentPayload(null, d, "TRAFFIC_FINE",
                INFRACTION.toString(), INFRACTION.toString(), null, null, BigDecimal.ONE, BigDecimal.ZERO,
                "TRAFFIC_INFRACTION", null, null, null, now, null))).hasMessageContaining("remainingBalance");
    }

    @Test
    void paymentReversedValidatesReasonAmountAndUsesReversedAmount() {
        var events = mock(InfractionPaymentEventRepository.class);
        var service = paymentService(events);
        String p = UUID.randomUUID().toString();
        String d = UUID.randomUUID().toString();
        OffsetDateTime now = OffsetDateTime.now();
        java.util.function.Function<String, RentasPaymentPayload> withReason = reason ->
                new RentasPaymentPayload(p, d, "TRAFFIC_FINE", INFRACTION.toString(), INFRACTION.toString(),
                        null, new BigDecimal("3.00"), BigDecimal.ONE, null, null, null, reason, null, null, now);

        service.record("paymentReversed", withReason.apply("error bancario"));
        var captor = ArgumentCaptor.forClass(ar.edu.uade.transito.transito_backend.model.InfractionPaymentEvent.class);
        verify(events).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("3.00");
        assertThat(captor.getValue().getReason()).isEqualTo("error bancario");

        assertThatThrownBy(() -> service.record("paymentReversed", withReason.apply(" "))).hasMessageContaining("reason");
        assertThatThrownBy(() -> service.record("paymentReversed", withReason.apply(null))).hasMessageContaining("reason");
        assertThatThrownBy(() -> service.record("paymentReversed", new RentasPaymentPayload(p, d, "TRAFFIC_FINE",
                INFRACTION.toString(), INFRACTION.toString(), null, BigDecimal.ZERO, BigDecimal.ONE, null, null, null,
                "r", null, null, now))).hasMessageContaining("reversedAmount");
        assertThatThrownBy(() -> service.record("paymentReversed", new RentasPaymentPayload(p, d, "TRAFFIC_FINE",
                INFRACTION.toString(), INFRACTION.toString(), null, TEN, BigDecimal.ONE, null, null, null,
                "r", null, null, null))).hasMessageContaining("reversedAt");
        assertThatThrownBy(() -> service.record("otroEvento", withReason.apply("x")))
                .hasMessageContaining("no soportado");
    }

    // ---------- Handlers de Rentas: tipo de evento y delegación ----------

    @Test
    void rentasHandlersExposeTheirEventTypeAndDelegateTheConvertedPayload() {
        var mapper = mock(ObjectMapper.class);
        var service = mock(InfractionPaymentEventService.class);
        JsonNode data = mock(JsonNode.class);
        var payload = registered(UUID.randomUUID().toString(), UUID.randomUUID().toString(), TEN, BigDecimal.ZERO, OffsetDateTime.now());
        when(mapper.convertValue(data, RentasPaymentPayload.class)).thenReturn(payload);

        var registeredHandler = new PaymentRegisteredHandler(mapper, service);
        var settledHandler = new DebtSettledHandler(mapper, service);
        var reversedHandler = new PaymentReversedHandler(mapper, service);
        assertThat(registeredHandler.eventType()).isEqualTo("paymentRegistered");
        assertThat(settledHandler.eventType()).isEqualTo("debtSettled");
        assertThat(reversedHandler.eventType()).isEqualTo("paymentReversed");

        registeredHandler.handle(data);
        settledHandler.handle(data);
        reversedHandler.handle(data);
        verify(service).record("paymentRegistered", payload);
        verify(service).record("debtSettled", payload);
        verify(service).record("paymentReversed", payload);
    }
}
