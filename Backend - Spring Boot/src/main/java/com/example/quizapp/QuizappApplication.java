package main.java.com.example.quizapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class QuizappApplication { // Annotation qui combine plusieurs fonctionnalités de Spring. Elle active l'auto-configuration, le balayage des composants et la configuration basée sur Spring Boot.

	public static void main(String[] args) {
	// Méthode principale qui sert de point d'entrée à l'application Spring Boot.
	// SpringApplication.run lance l'application Spring Boot et crée un contexte d'application Spring.
		SpringApplication.run(QuizappApplication.class, args);
	}

}
