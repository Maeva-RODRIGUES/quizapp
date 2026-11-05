package io.github.maevarodrigues.quizapp;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Fournit un PostgreSQL éphémère (Testcontainers) aux tests qui ont besoin d'une base.
 * <p>
 * {@code @ServiceConnection} configure automatiquement la datasource Spring
 * (URL, utilisateur, mot de passe) à partir du conteneur démarré.
 * Même image que {@code compose.yaml} : on teste contre le moteur utilisé en local.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:18"));
    }
}
