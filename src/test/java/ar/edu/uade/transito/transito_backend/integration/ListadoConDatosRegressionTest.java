package ar.edu.uade.transito.transito_backend.integration;

import ar.edu.uade.transito.transito_backend.support.AbstractIntegrationTest;
import ar.edu.uade.transito.transito_backend.support.Payloads;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetailsService;

import static ar.edu.uade.transito.transito_backend.support.TestUsers.INSPECTOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regresión del defecto "listado de infracciones degradado con muchos datos": antes el listado hacía
 * 1 + 2·N consultas (una por cada @ElementCollection de cada fila, cargadas en la vista con open-in-view).
 * Ahora las colecciones se cargan en lotes dentro de la transacción del servicio.
 */
@DisplayName("Listados con datos: sin N+1, sin open-in-view y sin usuario en memoria generado")
class ListadoConDatosRegressionTest extends AbstractIntegrationTest {

    private static final int FILAS = 40;

    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private ApplicationContext context;

    @Value("${spring.jpa.open-in-view}")
    private boolean openInView;

    @Test
    @DisplayName("GET /infractions con 40 filas: misma respuesta (colecciones incluidas) en un número de consultas constante")
    void listadoSinNMasUno() throws Exception {
        for (int i = 0; i < FILAS; i++) {
            post("/infractions", INSPECTOR, Payloads.infraction("30777888")).andExpect(status().isCreated());
        }
        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        try {
            get("/infractions", INSPECTOR)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(FILAS)))
                    .andExpect(jsonPath("$[0].evidenceUrl[0]").value("https://evidencias/foto1.jpg"))
                    .andExpect(jsonPath("$[%d].aggravatingFactors[0]".formatted(FILAS - 1)).value("zona escolar"));
            // 1 consulta de infracciones + 1 lote por colección (antes: 1 + 2·40 = 81).
            assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(3);
        } finally {
            stats.setStatisticsEnabled(false);
        }
    }

    @Test
    @DisplayName("open-in-view desactivado y sin UserDetailsService autoconfigurado (no se genera password)")
    void configuracion() {
        assertThat(openInView).isFalse();
        assertThat(context.getBeanNamesForType(UserDetailsService.class)).isEmpty();
    }
}
