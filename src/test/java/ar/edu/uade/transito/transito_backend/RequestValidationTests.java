package ar.edu.uade.transito.transito_backend;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ar.edu.uade.transito.transito_backend.model.Location;
import ar.edu.uade.transito.transito_backend.model.enums.DebtorIdType;
import ar.edu.uade.transito.transito_backend.model.enums.InfractionType;
import ar.edu.uade.transito.transito_backend.web.dto.CreateInfractionRequest;
import ar.edu.uade.transito.transito_backend.web.dto.CreateParkingTariffRequest;
import ar.edu.uade.transito.transito_backend.web.dto.CreateRoadAccidentRequest;
import ar.edu.uade.transito.transito_backend.web.dto.ReleaseImpoundmentRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class RequestValidationTests {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void rejectsInvalidInfractionAmountsAndIncompleteDebtorIdentification() {
        CreateInfractionRequest request = new CreateInfractionRequest(
                "AB123CD",
                InfractionType.ILLEGAL_PARKING,
                LocalDateTime.now().minusMinutes(1),
                Location.builder().street("Av. Cabildo").build(),
                List.of(),
                null,
                null,
                "40123456",
                null,
                new BigDecimal("50000"),
                List.of(),
                new BigDecimal("40000"),
                null,
                null,
                null);

        Set<ConstraintViolation<CreateInfractionRequest>> violations = validator.validate(request);

        assertTrue(hasMessage(violations, "debtorId y debtorIdType deben informarse juntos"));
        assertTrue(hasMessage(violations, "El importe final no puede ser menor que el importe base"));
    }

    @Test
    void requiresDamageDescriptionWhenAccidentDamagesInfrastructure() {
        CreateRoadAccidentRequest request = new CreateRoadAccidentRequest(
                LocalDateTime.now().minusMinutes(1),
                Location.builder().street("Av. Rivadavia").build(),
                true,
                " ",
                List.of(),
                List.of(),
                null);

        assertTrue(hasMessage(validator.validate(request),
                "La descripción del daño es obligatoria cuando existe daño de infraestructura"));
    }

    @Test
    void requiresPaymentReferenceForPaidVehicleRelease() {
        ReleaseImpoundmentRequest request = new ReleaseImpoundmentRequest(
                List.of("DOCUMENTATION_OK"), new BigDecimal("85000"), null);

        assertTrue(hasMessage(validator.validate(request),
                "La referencia de pago es obligatoria cuando el costo es mayor que cero"));
    }

    @Test
    void rejectsParkingTariffWithInvertedValidityRange() {
        CreateParkingTariffRequest request = new CreateParkingTariffRequest(
                UUID.randomUUID(),
                new BigDecimal("1200"),
                LocalDate.of(2026, 12, 31),
                LocalDate.of(2026, 1, 1));

        assertTrue(hasMessage(validator.validate(request),
                "La fecha de vigencia final no puede ser anterior a la inicial"));
    }

    private static boolean hasMessage(Set<? extends ConstraintViolation<?>> violations, String message) {
        return violations.stream().anyMatch(violation -> message.equals(violation.getMessage()));
    }
}
