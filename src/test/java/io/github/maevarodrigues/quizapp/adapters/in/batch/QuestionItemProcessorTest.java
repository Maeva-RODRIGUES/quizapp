package io.github.maevarodrigues.quizapp.adapters.in.batch;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionItemProcessorTest {

    private final QuestionItemProcessor processor =
            new QuestionItemProcessor(new QuestionImportValidator("phobies"::equals, key -> false));

    @Test
    void validLine_becomesAQuestion() {
        QuestionCsvLine line = new QuestionCsvLine(2, "raw", "phobies", "Q ?", List.of("A", "B", "", ""), "2");

        assertThat(processor.process(line).correctOption()).isEqualTo("B");
    }

    @Test
    void invalidLine_throwsAnExceptionCarryingTheLineAndItsReasons() {
        QuestionCsvLine line = new QuestionCsvLine(5, "raw", "inconnue", "Q ?", List.of("A", "B", "", ""), "1");

        assertThatThrownBy(() -> processor.process(line))
                .isInstanceOfSatisfying(InvalidQuestionException.class, exception -> {
                    assertThat(exception.line()).isSameAs(line);
                    assertThat(exception.reasons()).containsExactly(RejectionReason.UNKNOWN_CATEGORY);
                });
    }
}
