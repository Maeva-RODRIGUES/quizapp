package io.github.maevarodrigues.quizapp.domain.model;

import io.github.maevarodrigues.quizapp.domain.exception.InvalidSubmissionException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuizSubmissionTest {

    // Bonnes réponses : question 1 → index 1, question 2 → index 2, question 3 → index 0
    private static final Question Q1 = new Question(1L, "superstitions", "Échelle ?", List.of("Esprits", "Trinité", "Foudre"), 1);
    private static final Question Q2 = new Question(2L, "superstitions", "Miroir ?", List.of("Un an", "Trois mois", "Sept ans"), 2);
    private static final Question Q3 = new Question(3L, "phobies", "Nombres ?", List.of("Arithmophobie", "Chronophobie"), 0);
    private static final List<Question> QUIZ = List.of(Q1, Q2, Q3);

    @Nested
    class Grading {

        @Test
        void allCorrect_scoresFullMarks() {
            QuizResult result = new QuizSubmission(Map.of(1L, 1, 2L, 2, 3L, 0)).grade(QUIZ);

            assertThat(result.score()).isEqualTo(3);
            assertThat(result.total()).isEqualTo(3);
        }

        @Test
        void allWrong_scoresZero() {
            QuizResult result = new QuizSubmission(Map.of(1L, 0, 2L, 0, 3L, 1)).grade(QUIZ);

            assertThat(result.score()).isZero();
            assertThat(result.total()).isEqualTo(3);
        }

        @Test
        void mixedAnswers_giveOneCorrectionPerQuestionInQuizOrder() {
            QuizResult result = new QuizSubmission(Map.of(3L, 0, 1L, 2, 2L, 2)).grade(QUIZ);

            assertThat(result.score()).isEqualTo(2);
            assertThat(result.corrections()).containsExactly(
                    new QuestionCorrection(1L, 2, 1),
                    new QuestionCorrection(2L, 2, 2),
                    new QuestionCorrection(3L, 0, 0));
            assertThat(result.corrections()).extracting(QuestionCorrection::correct)
                    .containsExactly(false, true, true);
        }

        @Test
        void missingAnswer_countsAsWrongAndStillShowsTheCorrectAnswer() {
            QuizResult result = new QuizSubmission(Map.of(1L, 1)).grade(QUIZ);

            assertThat(result.score()).isEqualTo(1);
            assertThat(result.total()).isEqualTo(3);
            QuestionCorrection unanswered = result.corrections().get(1);
            assertThat(unanswered.answered()).isFalse();
            assertThat(unanswered.correct()).isFalse();
            assertThat(unanswered.givenOptionIndex()).isNull();
            assertThat(unanswered.correctOptionIndex()).isEqualTo(2);
        }

        @Test
        void emptySubmission_scoresZeroOnEveryQuestion() {
            QuizResult result = new QuizSubmission(Map.of()).grade(QUIZ);

            assertThat(result.score()).isZero();
            assertThat(result.total()).isEqualTo(3);
        }

        @Test
        void answerToQuestionOutsideTheQuiz_isRejected() {
            QuizSubmission submission = new QuizSubmission(Map.of(1L, 1, 99L, 0));

            assertThatThrownBy(() -> submission.grade(QUIZ))
                    .isInstanceOf(InvalidSubmissionException.class)
                    .hasMessageContaining("99");
        }

        @Test
        void answerIndexBeyondTheQuestionOptions_isRejected() {
            QuizSubmission submission = new QuizSubmission(Map.of(3L, 2)); // Q3 n'a que 2 réponses : index 0 ou 1

            assertThatThrownBy(() -> submission.grade(QUIZ))
                    .isInstanceOf(InvalidSubmissionException.class)
                    .hasMessageContaining("3")
                    .hasMessageContaining("2");
        }

        @Test
        void questionWithoutId_cannotBeGraded() {
            Question unsaved = new Question(null, "phobies", "Pas encore enregistrée ?", List.of("Oui", "Non"), 0);

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new QuizSubmission(Map.of()).grade(List.of(unsaved)))
                    .withMessageContaining("id");
        }

        @Test
        void sameQuestionTwiceInQuiz_isRejected() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new QuizSubmission(Map.of()).grade(List.of(Q1, Q1)))
                    .withMessageContaining("1");
        }
    }

    @Nested
    class Validation {

        @Test
        void rejectsNullAnswers() {
            assertThatIllegalArgumentException().isThrownBy(() -> new QuizSubmission(null));
        }

        @Test
        void rejectsNullQuestionId() {
            Map<Long, Integer> answers = new HashMap<>();
            answers.put(null, 0);

            assertThatIllegalArgumentException().isThrownBy(() -> new QuizSubmission(answers));
        }

        @Test
        void rejectsNullOptionIndex() {
            Map<Long, Integer> answers = new HashMap<>();
            answers.put(1L, null);

            assertThatIllegalArgumentException().isThrownBy(() -> new QuizSubmission(answers));
        }

        @Test
        void rejectsNegativeOptionIndex() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new QuizSubmission(Map.of(1L, -1)))
                    .withMessageContaining("-1");
        }

        @Test
        void isNotAffectedByLaterChangesToTheSourceMap() {
            Map<Long, Integer> source = new HashMap<>(Map.of(1L, 1));
            QuizSubmission submission = new QuizSubmission(source);

            source.put(2L, 2);

            assertThat(submission.answers()).containsOnlyKeys(1L);
        }
    }
}
