package ar.edu.uade.transito.transito_backend.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestEventsConfig {

    @Bean
    @Primary
    public RecordingEventPublisher recordingEventPublisher() {
        return new RecordingEventPublisher();
    }

    /** Hora fija para las reglas temporales de los cortes (DEF-42); cada test la reinicia. */
    @Bean
    @Primary
    public MutableTestClock mutableTestClock() {
        return new MutableTestClock();
    }
}
