package io.github.maevarodrigues.quizapp;

import org.junit.jupiter.api.BeforeEach;
import io.github.maevarodrigues.quizapp.domain.model.QuestionKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
    void createsSpringBatchMetadataTablesAndImportRejection() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);

        assertThat(tables).contains("batch_job_instance", "batch_job_execution", "batch_job_execution_params",
                "batch_step_execution", "batch_step_execution_context", "batch_job_execution_context",
                "import_rejection");
    }

    @Test
    void storesRejectionLinkedToAJobExecution() {
        long executionId = insertJobExecution();

        jdbc.update("INSERT INTO import_rejection (job_execution_id, line_number, raw_line, reason) VALUES (?, 3, 'phobies;;A;B;;;1', 'EMPTY_TEXT')",
                executionId);

        assertThat(jdbc.queryForObject("SELECT created_at IS NOT NULL FROM import_rejection WHERE job_execution_id = ?",
                Boolean.class, executionId)).isTrue();
    }

    @Test
    void rejectsRejectionOfUnknownJobExecution() {
        assertThatThrownBy(() -> insertRejection(999_999L, 2, "EMPTY_TEXT"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_import_rejection_job_execution");
    }

    @Test
    void rejectsNonPositiveLineNumber() {
        long executionId = insertJobExecution();

        assertThatThrownBy(() -> insertRejection(executionId, 0, "EMPTY_TEXT"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_import_rejection_line_number");
    }

    @Test
    void rejectsReasonThatIsNotAnUpperSnakeCaseCode() {
        long executionId = insertJobExecution();

        assertThatThrownBy(() -> insertRejection(executionId, 2, "texte vide"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_import_rejection_reason_format");
    }

    @Test
    void deletesRejectionsWithTheirJobExecution() {
        long executionId = insertJobExecution();
        insertRejection(executionId, 2, "EMPTY_TEXT");

        jdbc.update("DELETE FROM batch_job_execution WHERE job_execution_id = ?", executionId);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM import_rejection WHERE job_execution_id = ?",
                Integer.class, executionId)).isZero();
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

    /**
     * Le dédoublonnage de l'import (Java, {@link QuestionKey}) et la contrainte d'unicité (SQL, {@code text_key})
     * doivent donner le même verdict. Les textes sont passés tels que le domaine les enregistre (sans espaces autour).
     */
    @ParameterizedTest
    @ValueSource(strings = {"Quelle est la peur des clowns ?", "QUELLE   est\tla\npeur des CLOWNS ?", "Peur des ARAIGNÉES ?"})
    void javaQuestionKeyMatchesDatabaseTextKey(String text) {
        long questionId = insertQuestion(categoryId, text);

        String textKey = jdbc.queryForObject("SELECT text_key FROM question WHERE id = ?", String.class, questionId);

        assertThat(QuestionKey.of("test-cat", text).normalizedText()).isEqualTo(textKey);
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

    /** Crée une instance et une exécution de job minimales, comme le ferait Spring Batch. */
    private long insertJobExecution() {
        long instanceId = jdbc.queryForObject("SELECT nextval('batch_job_instance_seq')", Long.class);
        jdbc.update("INSERT INTO batch_job_instance (job_instance_id, version, job_name, job_key) VALUES (?, 0, 'test-job', md5(?::text))",
                instanceId, instanceId);
        long executionId = jdbc.queryForObject("SELECT nextval('batch_job_execution_seq')", Long.class);
        jdbc.update("INSERT INTO batch_job_execution (job_execution_id, version, job_instance_id, create_time) VALUES (?, 0, ?, now())",
                executionId, instanceId);
        return executionId;
    }

    private void insertRejection(long executionId, int lineNumber, String reason) {
        jdbc.update("INSERT INTO import_rejection (job_execution_id, line_number, raw_line, reason) VALUES (?, ?, 'ligne', ?)",
                executionId, lineNumber, reason);
    }

    private long insertQuestion(long categoryId, String text) {
        return jdbc.queryForObject(
                "INSERT INTO question (category_id, text, correct_option_index) VALUES (?, ?, 0) RETURNING id",
                Long.class, categoryId, text);
    }
}
