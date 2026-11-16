package ar.edu.uade.transito.transito_backend.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reloj del sistema como bean: permite fijar la hora en los tests de reglas temporales (DEF-42). */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
