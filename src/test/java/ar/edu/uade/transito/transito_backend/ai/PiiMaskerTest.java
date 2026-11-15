package ar.edu.uade.transito.transito_backend.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("PiiMasker (Ley 25.326)")
class PiiMaskerTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "DNI 30111222 frena|30111222",
            "DNI 30.111.222 frena|30.111.222",
            "CUIT 20-30111222-3 titular|20-30111222-3",
            "documento 12345678901 x|12345678901",
            "mail juan.perez@gmail.com ok|juan.perez@gmail.com",
            "patente AB123CD estacionado|AB123CD",
            "patente ab 123 cd estacionado|ab 123 cd",
            "patente vieja ABC123 estacionado|ABC123",
            "llamar al +54 9 11 5555-1234 ya|+54 9 11 5555-1234",
            "llamar al 011 4444-5555 ya|011 4444-5555",
            "llamar al 11 5555-1234 ya|11 5555-1234"})
    void masksPii(String text, String pii) {
        String masked = PiiMasker.mask(text);
        assertThat(masked).doesNotContain(pii.strip());
        assertThat(masked).containsPattern("\\[(DOC|EMAIL|PATENTE|TEL)\\]");
    }

    @Test
    void keepsNormalTextAndHandlesNull() {
        assertThat(PiiMasker.mask("Cruzó en rojo a 60 km/h, zona escolar")).isEqualTo("Cruzó en rojo a 60 km/h, zona escolar");
        assertThat(PiiMasker.mask(null)).isEmpty();
    }
}
