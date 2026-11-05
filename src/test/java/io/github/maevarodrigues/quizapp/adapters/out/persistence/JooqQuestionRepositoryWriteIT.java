package io.github.maevarodrigues.quizapp.adapters.out.persistence;

import io.github.maevarodrigues.quizapp.TestcontainersConfiguration;
import io.github.maevarodrigues.quizapp.domain.exception.UnknownCategoryException;
import io.github.maevarodrigues.quizapp.domain.model.Question;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jooq.test.autoconfigure.JooqTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureDataSourceInitialization;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static io.github.maevarodrigues.quizapp.adapters.out.persistence.jooq.Tables.QUESTION;
import static io.github.maevarodrigues.quizapp.adapters.out.persistence.jooq.Tables.QUESTION_OPTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Écriture des questions, sur les données de démo (10 questions, profil "local").
 */
@JooqTest
@AutoConfigureDataSourceInitialization
@ActiveProfiles("local")
@Import({TestcontainersConfiguration.class, JooqQuestionRepository.class})
class JooqQuestionRepositoryWriteIT {

    private static final Question CLOWNS = newQuestion("phobies", "Comment appelle-t-on la peur des clowns ?",
            List.of("Coulrophobie", "Clinophobie", "Cynophobie"), 0);
    private static final Question TREIZE = newQuestion("superstitions", "Quel nombre porte malheur à table ?",
            List.of("7", "13"), 1);

    @Autowired
    private JooqQuestionRepository repository;

    @Autowired
    private DSLContext dsl;

    private static Question newQuestion(String category, String text, List<String> options, int correct) {
        return new Question(null, category, text, options, correct);
    }

    @Test
    void saveAll_persistsQuestionsWithOptionsAndReturnsTheirNewIds() {
        List<Question> saved = repository.saveAll(List.of(CLOWNS, TREIZE));

        assertThat(saved).extracting(Question::id).doesNotContainNull().doesNotHaveDuplicates()
                .allSatisfy(id -> assertThat(id).isGreaterThan(10L));
        assertThat(saved).extracting(Question::text).containsExactly(CLOWNS.text(), TREIZE.text());

        Question reloaded = repository.findByCategoryCode("phobies").getLast();
        assertThat(reloaded).isEqualTo(saved.getFirst());
        assertThat(reloaded.options()).containsExactly("Coulrophobie", "Clinophobie", "Cynophobie");
        assertThat(reloaded.correctOption()).isEqualTo("Coulrophobie");
    }

    @Test
    void saveAll_emptyList_savesNothing() {
        assertThat(repository.saveAll(List.of())).isEmpty();
        assertThat(questionCount()).isEqualTo(10);
    }

    @Test
    void saveAll_rejectsAlreadySavedQuestion() {
        Question alreadySaved = new Question(6L, "phobies", "Déjà en base ?", List.of("Oui", "Non"), 0);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> repository.saveAll(List.of(CLOWNS, alreadySaved)))
                .withMessageContaining("6");
        assertThat(questionCount()).isEqualTo(10);
    }

    @Test
    void saveAll_rejectsUnknownCategory() {
        Question orphan = newQuestion("inconnue", "Catégorie inconnue ?", List.of("Oui", "Non"), 0);

        assertThatThrownBy(() -> repository.saveAll(List.of(orphan)))
                .isInstanceOf(UnknownCategoryException.class)
                .hasMessageContaining("inconnue");
        assertThat(questionCount()).isEqualTo(10);
    }

    @Test
    void saveAll_duplicateOfExistingQuestion_isRejectedByTheDatabase() {
        // Même texte qu'une question de démo, à la casse et aux espaces près : l'index unique sur text_key le refuse.
        Question duplicate = newQuestion("phobies", "  quel est le nom de la PEUR des araignées ?",
                List.of("Arachnophobie", "Autre"), 0);

        assertThatThrownBy(() -> repository.saveAll(List.of(duplicate)))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("uq_question_category_text_key");
    }

    /**
     * Sans la transaction de test (NOT_SUPPORTED) : on observe le vrai comportement de saveAll,
     * qui ouvre sa propre transaction et doit tout annuler si une insertion échoue.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void saveAll_isAtomic_aFailureRollsBackTheWholeBatch() {
        Question duplicate = newQuestion("phobies", "Quel est le nom de la peur des araignées ?", List.of("A", "B"), 0);
        try {
            assertThatThrownBy(() -> repository.saveAll(List.of(CLOWNS, duplicate)))
                    .isInstanceOf(DuplicateKeyException.class);

            assertThat(questionCount()).isEqualTo(10);
            assertThat(dsl.fetchCount(QUESTION, QUESTION.TEXT.eq(CLOWNS.text()))).isZero();
            assertThat(dsl.fetchCount(QUESTION_OPTION)).isEqualTo(30);
        } finally {
            // Hors transaction de test, rien n'est annulé automatiquement : on remet la base dans son état initial.
            dsl.deleteFrom(QUESTION).where(QUESTION.ID.gt(10L)).execute();
        }
    }

    private int questionCount() {
        return dsl.fetchCount(QUESTION);
    }
}
