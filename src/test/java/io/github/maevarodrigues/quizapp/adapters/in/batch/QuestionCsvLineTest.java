package io.github.maevarodrigues.quizapp.adapters.in.batch;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class QuestionCsvLineTest {

    @Test
    void replacesMissingValuesWithEmptyStrings() {
        QuestionCsvLine line = new QuestionCsvLine(2, null, null, null, Arrays.asList("A", null), null);

        assertThat(line.rawLine()).isEmpty();
        assertThat(line.category()).isEmpty();
        assertThat(line.text()).isEmpty();
        assertThat(line.options()).containsExactly("A", "");
        assertThat(line.correctOption()).isEmpty();
    }

    @Test
    void nullOptions_becomeAnEmptyList() {
        assertThat(new QuestionCsvLine(2, "", "", "", null, "").options()).isEmpty();
    }

    @Test
    void optionsAreADefensiveCopy() {
        List<String> options = new ArrayList<>(List.of("A", "B"));
        QuestionCsvLine line = new QuestionCsvLine(2, "", "", "", options, "");

        options.add("C");

        assertThat(line.options()).containsExactly("A", "B");
    }

    @Test
    void rejectsNonPositiveLineNumber() {
        assertThatIllegalArgumentException().isThrownBy(() -> new QuestionCsvLine(0, "", "", "", List.of(), ""));
    }
}
