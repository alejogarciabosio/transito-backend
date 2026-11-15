package ar.edu.uade.transito.transito_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

// La autenticación es solo JWT (SecurityConfig): sin el usuario en memoria por defecto, Spring Boot no genera
// ni imprime "Using generated security password" al arrancar.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
public class TransitoBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(TransitoBackendApplication.class, args);
	}

}
