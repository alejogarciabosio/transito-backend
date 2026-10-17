package ar.edu.uade.transito.transito_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TransitoBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(TransitoBackendApplication.class, args);
	}

}
