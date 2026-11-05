package io.github.maevarodrigues.quizapp.adapters.out.persistence;

import io.github.maevarodrigues.quizapp.TestcontainersConfiguration;
import io.github.maevarodrigues.quizapp.domain.model.Question;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jooq.test.autoconfigure.JooqTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureDataSourceInitialization;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static io.github.maevarodrigues.quizapp.adapters.out.persistence.jooq.Tables.QUESTION;
import static io.github.maevarodrigues.quizapp.adapters.out.persistence.jooq.Tables.QUESTION_OPTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Lit les 10 questions de démo (profil "local") sur un vrai PostgreSQL.
 * {@code @JooqTest} est transactionnel : les insertions faites dans un test sont annulées à la fin.
 */
@JooqTest
@AutoConfigureDataSourceInitialization
@ActiveProfiles("local")
@Import({TestcontainersConfiguration.class, JooqQuestionRepository.class, QueryCounter.class})
class JooqQuestionRepositoryIT {

    private static final Question ARAIGNEES = new Question(6L, "phobies", "Quel est le nom de la peur des araignées ?",
            List.of("Entomophobie", "Myrmécophobie", "Arachnophobie"), 2);

    @Autowired
    private JooqQuestionRepository repository;

    @Autowired
    private DSLContext dsl;

    @Autowired
    private QueryCounter queryCounter;

    @BeforeEach
    void resetQueryCounter() {
        queryCounter.reset();
    }

    @Test
    void findAll_returnsAllDemoQuestionsSortedById() {
        List<Question> questions = repository.findAll();

        assertThat(questions).extracting(Question::id).containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
        assertThat(questions).contains(ARAIGNEES);
    }

    @Test
    void findAll_loadsQuestionsWithTheirOptionsInASingleQuery() {
        repository.findAll();

        assertThat(queryCounter.count()).isEqualTo(1);
    }

    @Test
    void findByCategoryCode_returnsOnlyThatCategory() {
        List<Question> phobies = repository.findByCategoryCode("phobies");

        assertThat(phobies).extracting(Question::id).containsExactly(6L, 7L, 8L, 9L, 10L);
        assertThat(phobies).allSatisfy(question -> assertThat(question.categoryCode()).isEqualTo("phobies"));
        assertThat(phobies.getFirst()).isEqualTo(ARAIGNEES);
    }

    @Test
    void findByCategoryCode_unknownCategory_returnsEmptyList() {
        assertThat(repository.findByCategoryCode("inconnue")).isEmpty();
    }

    @Test
    void optionsAreOrderedByIndexWhateverTheInsertionOrder() {
        long id = insertQuestion("Dans quel ordre ?", 1);
        insertOption(id, 2, "Troisième");
        insertOption(id, 0, "Première");
        insertOption(id, 1, "Deuxième");

        Question question = findById(id);

        assertThat(question.options()).containsExactly("Première", "Deuxième", "Troisième");
        assertThat(question.correctOption()).isEqualTo("Deuxième");
    }

    @Test
    void gapInOptionIndexes_isRejectedInsteadOfShiftingTheCorrectAnswer() {
        long id = insertQuestion("Question avec un trou ?", 1);
        insertOption(id, 0, "Index 0");
        insertOption(id, 2, "Index 2");

        assertThatIllegalStateException()
                .isThrownBy(() -> repository.findByCategoryCode("phobies"))
                .withMessageContaining("question " + id)
                .withMessageContaining("expected 1 but found 2");
    }

    private long insertQuestion(String text, int correctOptionIndex) {
        return dsl.insertInto(QUESTION, QUESTION.CATEGORY_ID, QUESTION.TEXT, QUESTION.CORRECT_OPTION_INDEX)
                .values(2L, text, (short) correctOptionIndex)
                .returningResult(QUESTION.ID)
                .fetchSingle(QUESTION.ID);
    }

    private void insertOption(long questionId, int index, String label) {
        dsl.insertInto(QUESTION_OPTION, QUESTION_OPTION.QUESTION_ID, QUESTION_OPTION.OPTION_INDEX, QUESTION_OPTION.LABEL)
                .values(questionId, (short) index, label)
                .execute();
    }

    private Question findById(long id) {
        return repository.findAll().stream().filter(q -> q.id() == id).findFirst().orElseThrow();
    }
}
