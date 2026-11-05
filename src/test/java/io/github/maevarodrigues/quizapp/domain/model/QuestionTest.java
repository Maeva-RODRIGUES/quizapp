package io.github.maevarodrigues.quizapp.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionTest {

    private static final List<String> OPTIONS = List.of("Entomophobie", "Myrmécophobie", "Arachnophobie");

    private static Question question(String text, List<String> options, int correctOptionIndex) {
        return new Question(6L, "phobies", text, options, correctOptionIndex);
    }

    @Test
    void createsValidQuestion() {
        Question question = question("Quel est le nom de la peur des araignées ?", OPTIONS, 2);

        assertThat(question.id()).isEqualTo(6L);
        assertThat(question.categoryCode()).isEqualTo("phobies");
        assertThat(question.text()).isEqualTo("Quel est le nom de la peur des araignées ?");
        assertThat(question.options()).containsExactly("Entomophobie", "Myrmécophobie", "Arachnophobie");
        assertThat(question.correctOptionIndex()).isEqualTo(2);
    }

    @Test
    void allowsNullIdForQuestionNotYetSaved() {
        assertThat(new Question(null, "phobies", "Question ?", OPTIONS, 0).id()).isNull();
    }

    // --- Texte -------------------------------------------------------------------------------

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void rejectsBlankText(String text) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> question(text, OPTIONS, 0))
                .withMessageContaining("text");
    }

    @Test
    void stripsText() {
        assertThat(question("  Question ?  ", OPTIONS, 0).text()).isEqualTo("Question ?");
    }

    @Test
    void acceptsTextOfMaximumLength() {
        String text = "a".repeat(Question.TEXT_MAX_LENGTH);

        assertThat(question(text, OPTIONS, 0).text()).hasSize(Question.TEXT_MAX_LENGTH);
    }

    @Test
    void rejectsTooLongText() {
        String text = "a".repeat(Question.TEXT_MAX_LENGTH + 1);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> question(text, OPTIONS, 0))
                .withMessageContaining("text");
    }

    // --- Catégorie ---------------------------------------------------------------------------

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Phobies", "pas valide"})
    void rejectsInvalidCategoryCode(String categoryCode) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Question(null, categoryCode, "Question ?", OPTIONS, 0))
                .withMessageContaining("category");
    }

    // --- Réponses proposées ------------------------------------------------------------------

    @Test
    void rejectsNullOptions() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> question("Question ?", null, 0))
                .withMessageContaining("options");
    }

    @Test
    void rejectsFewerThanTwoOptions() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> question("Question ?", List.of("Seule réponse"), 0))
                .withMessageContaining("at least " + Question.MIN_OPTIONS);
    }

    @Test
    void rejectsNullOption() {
        List<String> options = Arrays.asList("Arachnophobie", null);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> question("Question ?", options, 0))
                .withMessageContaining("option");
    }

    @Test
    void rejectsBlankOption() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> question("Question ?", List.of("Arachnophobie", "  "), 0))
                .withMessageContaining("option");
    }

    @Test
    void rejectsTooLongOption() {
        String tooLong = "a".repeat(Question.OPTION_MAX_LENGTH + 1);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> question("Question ?", List.of("Arachnophobie", tooLong), 0))
                .withMessageContaining("option");
    }

    @Test
    void rejectsDuplicateOptionsIgnoringCaseAndSpaces() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> question("Question ?", List.of("Arachnophobie", " arachnophobie "), 0))
                .withMessageContaining("duplicate");
    }

    @Test
    void stripsOptions() {
        assertThat(question("Question ?", List.of(" Oui ", "Non  "), 0).options()).containsExactly("Oui", "Non");
    }

    // --- Bonne réponse -----------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(ints = {-1, 3, 42})
    void rejectsCorrectOptionIndexOutOfBounds(int correctOptionIndex) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> question("Question ?", OPTIONS, correctOptionIndex))
                .withMessageContaining("correctOptionIndex");
    }

    @Test
    void acceptsFirstAndLastIndex() {
        assertThat(question("Question ?", OPTIONS, 0).correctOptionIndex()).isZero();
        assertThat(question("Question ?", OPTIONS, 2).correctOptionIndex()).isEqualTo(2);
    }

    @Test
    void exposesCorrectOption() {
        assertThat(question("Question ?", OPTIONS, 2).correctOption()).isEqualTo("Arachnophobie");
    }

    // --- Immutabilité ------------------------------------------------------------------------

    @Test
    void isNotAffectedByLaterChangesToTheSourceList() {
        List<String> source = new ArrayList<>(OPTIONS);
        Question question = question("Question ?", source, 0);

        source.set(0, "Modifiée après coup");

        assertThat(question.options()).containsExactly("Entomophobie", "Myrmécophobie", "Arachnophobie");
    }

    @Test
    void exposesUnmodifiableOptions() {
        Question question = question("Question ?", OPTIONS, 0);

        assertThatThrownBy(() -> question.options().add("Intrus"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void twoQuestionsWithSameValuesAreEqual() {
        assertThat(question("Question ?", OPTIONS, 1)).isEqualTo(question("Question ?", OPTIONS, 1));
    }
}
