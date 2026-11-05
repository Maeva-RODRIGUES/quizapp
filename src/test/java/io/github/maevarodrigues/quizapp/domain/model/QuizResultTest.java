package io.github.maevarodrigues.quizapp.domain.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuizResultTest {

    @Test
    void scoreCountsCorrectAnswersAndTotalCountsQuestions() {
        QuizResult result = new QuizResult(List.of(
                new QuestionCorrection(1L, 1, 1),
                new QuestionCorrection(2L, 0, 2),
                new QuestionCorrection(3L, null, 0)));

        assertThat(result.score()).isEqualTo(1);
        assertThat(result.total()).isEqualTo(3);
    }

    @Test
    void emptyResult_scoresZeroOutOfZero() {
        QuizResult result = new QuizResult(List.of());

        assertThat(result.score()).isZero();
        assertThat(result.total()).isZero();
    }

    @Test
    void rejectsNullCorrections() {
        assertThatIllegalArgumentException().isThrownBy(() -> new QuizResult(null));
    }

    @Test
    void exposesUnmodifiableCorrectionsIndependentOfTheSourceList() {
        List<QuestionCorrection> source = new ArrayList<>(List.of(new QuestionCorrection(1L, 1, 1)));
        QuizResult result = new QuizResult(source);

        source.clear();

        assertThat(result.total()).isEqualTo(1);
        assertThatThrownBy(() -> result.corrections().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void correction_isCorrectOnlyWhenAnsweredWithTheRightIndex() {
        assertThat(new QuestionCorrection(1L, 2, 2).correct()).isTrue();
        assertThat(new QuestionCorrection(1L, 1, 2).correct()).isFalse();
        assertThat(new QuestionCorrection(1L, null, 2).correct()).isFalse();
    }

    @Test
    void correction_rejectsNegativeIndexes() {
        assertThatIllegalArgumentException().isThrownBy(() -> new QuestionCorrection(1L, -1, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new QuestionCorrection(1L, 0, -1));
    }
}
