package io.github.maevarodrigues.quizapp.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ImportRejectionTest {

    @Test
    void validRejection() {
        assertThat(new ImportRejection(7L, 3, "phobies;;A;B;;;1", "EMPTY_TEXT").reason()).isEqualTo("EMPTY_TEXT");
    }

    @Test
    void rejectsNonPositiveLineNumber() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ImportRejection(7L, 0, "", "EMPTY_TEXT"));
    }

    @Test
    void rejectsNullRawLine() {
        assertThatNullPointerException().isThrownBy(() -> new ImportRejection(7L, 2, null, "EMPTY_TEXT"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"empty_text", "EMPTY TEXT", "_EMPTY", "EMPTY_"})
    void rejectsReasonThatIsNotAnUpperSnakeCaseCode(String reason) {
        assertThatIllegalArgumentException().isThrownBy(() -> new ImportRejection(7L, 2, "", reason));
    }
}
