package io.github.maevarodrigues.quizapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Vérifie que le schéma créé par Liquibase existe et que ses contraintes protègent la donnée.
 * <p>
 * {@code @Transactional} : chaque test est annulé (rollback) à la fin, les tests ne se polluent pas.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class LiquibaseSchemaIT {

    @Autowired
    private JdbcTemplate jdbc;

    private long categoryId;

    @BeforeEach
    void insertCategory() {
        categoryId = jdbc.queryForObject(
                "INSERT INTO category (code, label) VALUES ('test-cat', 'Catégorie de test') RETURNING id",
                Long.class);
    }

    @Test
    void createsTables() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);

        assertThat(tables).contains("category", "question", "question_option",
                "databasechangelog", "databasechangeloglock");
    }

    @Test
    void withoutLocalProfile_loadsReferenceCategoriesButNoDemoQuestions() {
        List<String> codes = jdbc.queryForList(
                "SELECT code FROM category WHERE code <> 'test-cat' ORDER BY id", String.class);

        assertThat(codes).containsExactly("superstitions", "phobies");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM question", Integer.class)).isZero();
    }

    @Test
    void generatesIdsAndNormalizedTextKey() {
        long questionId = insertQuestion(categoryId, "  Quelle   est la PEUR des clowns ?  ");

        String textKey = jdbc.queryForObject("SELECT text_key FROM question WHERE id = ?", String.class, questionId);

        assertThat(textKey).isEqualTo("quelle est la peur des clowns ?");
    }

    @Test
    void rejectsSameQuestionTwiceInSameCategory_evenWithDifferentCaseAndSpaces() {
        insertQuestion(categoryId, "Quelle est la peur des clowns ?");

        assertThatThrownBy(() -> insertQuestion(categoryId, "quelle  est la PEUR des clowns ?"))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("uq_question_category_text_key");
    }

    @Test
    void allowsSameQuestionInAnotherCategory() {
        long otherCategoryId = jdbc.queryForObject(
                "INSERT INTO category (code, label) VALUES ('autre-cat', 'Autre') RETURNING id", Long.class);
        insertQuestion(categoryId, "Quelle est la peur des clowns ?");

        insertQuestion(otherCategoryId, "Quelle est la peur des clowns ?");

        assertThat(jdbc.queryForObject("SELECT count(*) FROM question", Integer.class)).isEqualTo(2);
    }

    @Test
    void rejectsBlankQuestionText() {
        assertThatThrownBy(() -> insertQuestion(categoryId, "   "))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_question_text_not_blank");
    }

    @Test
    void rejectsQuestionWithUnknownCategory() {
        assertThatThrownBy(() -> insertQuestion(999_999L, "Question orpheline ?"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_question_category");
    }

    @Test
    void rejectsInvalidCategoryCode() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO category (code, label) VALUES ('Pas Valide', 'X')"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_category_code_format");
    }

    @Test
    void deletesOptionsWithTheirQuestion() {
        long questionId = insertQuestion(categoryId, "Quelle est la peur des clowns ?");
        jdbc.update("INSERT INTO question_option (question_id, option_index, label) VALUES (?, 0, 'Coulrophobie'), (?, 1, 'Clinophobie')",
                questionId, questionId);

        jdbc.update("DELETE FROM question WHERE id = ?", questionId);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM question_option WHERE question_id = ?", Integer.class, questionId))
                .isZero();
    }

    private long insertQuestion(long categoryId, String text) {
        return jdbc.queryForObject(
                "INSERT INTO question (category_id, text, correct_option_index) VALUES (?, ?, 0) RETURNING id",
                Long.class, categoryId, text);
    }
}
