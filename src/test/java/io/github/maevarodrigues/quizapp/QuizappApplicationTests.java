package io.github.maevarodrigues.quizapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Démarre l'application complète sur un PostgreSQL Testcontainers.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class QuizappApplicationTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void contextLoadsAndConnectsToPostgres() {
		String version = jdbcTemplate.queryForObject("SELECT version()", String.class);

		assertThat(version).startsWith("PostgreSQL 18");
	}

}
