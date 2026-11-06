package io.github.maevarodrigues.quizapp.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class QuestionKeyTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "Quelle est la peur des clowns ?",
            "  quelle est la peur des clowns ?  ",
            "QUELLE   EST LA PEUR DES CLOWNS ?",
            "Quelle\test la\npeur des clowns ?"})
    void sameQuestionWrittenDifferently_hasTheSameKey(String text) {
        assertThat(QuestionKey.of("phobies", text))
                .isEqualTo(new QuestionKey("phobies", "quelle est la peur des clowns ?"));
    }

    @Test
    void keepsAccentsAndPunctuation() {
        assertThat(QuestionKey.of("phobies", "Peur des ARAIGNÉES ?").normalizedText()).isEqualTo("peur des araignées ?");
    }

    @Test
    void categoryIsPartOfTheKey() {
        assertThat(QuestionKey.of("phobies", "Q ?")).isNotEqualTo(QuestionKey.of("superstitions", "Q ?"));
    }

    @Test
    void canBeBuiltFromAQuestion() {
        Question question = new Question(null, "phobies", "Peur  des clowns ?", List.of("A", "B"), 0);

        assertThat(QuestionKey.of(question)).isEqualTo(new QuestionKey("phobies", "peur des clowns ?"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rejectsBlankText(String text) {
        assertThatIllegalArgumentException().isThrownBy(() -> QuestionKey.of("phobies", text));
    }
}
