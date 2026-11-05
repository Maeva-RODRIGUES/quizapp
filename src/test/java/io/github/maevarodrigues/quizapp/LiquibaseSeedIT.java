package io.github.maevarodrigues.quizapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie le chargement des données de démo (profil "local" → context Liquibase "demo")
 * et le recalage des séquences après des insertions à id explicite.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("local")
@Transactional
class LiquibaseSeedIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void loadsReferenceCategories() {
        List<String> codes = jdbc.queryForList("SELECT code FROM category ORDER BY id", String.class);

        assertThat(codes).containsExactly("superstitions", "phobies");
    }

    @Test
    void loadsTenDemoQuestionsWithThreeOptionsEach() {
        assertThat(count("SELECT count(*) FROM question")).isEqualTo(10);
        assertThat(count("SELECT count(*) FROM question_option")).isEqualTo(30);
        assertThat(count("""
                SELECT count(*) FROM question q
                WHERE (SELECT count(*) FROM question_option o WHERE o.question_id = q.id) <> 3
                """)).isZero();
    }

    @Test
    void everyCorrectOptionIndexPointsToAnExistingOption() {
        // Règle que la base ne peut pas imposer par une contrainte CHECK : on la vérifie sur les données.
        assertThat(count("""
                SELECT count(*) FROM question q
                WHERE NOT EXISTS (SELECT 1 FROM question_option o
                                  WHERE o.question_id = q.id AND o.option_index = q.correct_option_index)
                """)).isZero();
    }

    @Test
    void questionSequenceIsRealignedAfterExplicitIds() {
        Long newId = jdbc.queryForObject(
                "INSERT INTO question (category_id, text, correct_option_index) VALUES (2, 'Nouvelle question ?', 0) RETURNING id",
                Long.class);

        assertThat(newId).isEqualTo(11L);
    }

    @Test
    void categorySequenceIsRealignedAfterExplicitIds() {
        Long newId = jdbc.queryForObject(
                "INSERT INTO category (code, label) VALUES ('nouvelle', 'Nouvelle catégorie') RETURNING id", Long.class);

        assertThat(newId).isEqualTo(3L);
    }

    private int count(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }
}
