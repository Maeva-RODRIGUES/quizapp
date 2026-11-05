package io.github.maevarodrigues.quizapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée de l'application.
 * <p>
 * {@code @SpringBootApplication} active l'auto-configuration et le scan des composants
 * de ce package et de ses sous-packages.
 */
@SpringBootApplication
public class QuizappApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuizappApplication.class, args);
	}

}
