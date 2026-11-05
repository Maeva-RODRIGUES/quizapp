package io.github.maevarodrigues.quizapp.application;

import io.github.maevarodrigues.quizapp.domain.exception.InvalidSubmissionException;
import io.github.maevarodrigues.quizapp.domain.exception.UnknownCategoryException;
import io.github.maevarodrigues.quizapp.domain.model.Question;
import io.github.maevarodrigues.quizapp.domain.model.QuizResult;
import io.github.maevarodrigues.quizapp.domain.model.QuizSubmission;
import io.github.maevarodrigues.quizapp.domain.port.out.CategoryRepository;
import io.github.maevarodrigues.quizapp.domain.port.out.QuestionRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    private static final Question PHOBIE_1 = new Question(6L, "phobies", "Araignées ?", List.of("Entomophobie", "Arachnophobie"), 1);
    private static final Question PHOBIE_2 = new Question(7L, "phobies", "Nombres ?", List.of("Arithmophobie", "Logophobie"), 0);
    private static final Question SUPERSTITION = new Question(2L, "superstitions", "Miroir ?", List.of("Un an", "Sept ans"), 1);

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private QuizService quizService;

    @Nested
    class GetQuestions {

        @Test
        void getAllQuestions_returnsAllQuestionsFromRepository() {
            given(questionRepository.findAll()).willReturn(List.of(SUPERSTITION, PHOBIE_1, PHOBIE_2));

            assertThat(quizService.getAllQuestions()).containsExactly(SUPERSTITION, PHOBIE_1, PHOBIE_2);
        }

        @Test
        void getQuestionsByCategory_returnsQuestionsOfThatCategory() {
            given(categoryRepository.existsByCode("phobies")).willReturn(true);
            given(questionRepository.findByCategoryCode("phobies")).willReturn(List.of(PHOBIE_1, PHOBIE_2));

            assertThat(quizService.getQuestionsByCategory("phobies")).containsExactly(PHOBIE_1, PHOBIE_2);
        }

        @Test
        void getQuestionsByCategory_unknownCategory_throwsWithoutQueryingQuestions() {
            given(categoryRepository.existsByCode("inconnue")).willReturn(false);

            assertThatThrownBy(() -> quizService.getQuestionsByCategory("inconnue"))
                    .isInstanceOf(UnknownCategoryException.class)
                    .hasMessageContaining("inconnue");
            then(questionRepository).should(never()).findByCategoryCode(anyString());
        }

        @Test
        void getQuestionsByCategory_knownCategoryWithoutQuestions_returnsEmptyList() {
            given(categoryRepository.existsByCode("phobies")).willReturn(true);
            given(questionRepository.findByCategoryCode("phobies")).willReturn(List.of());

            assertThat(quizService.getQuestionsByCategory("phobies")).isEmpty();
        }
    }

    @Nested
    class SubmitQuiz {

        @Test
        void gradesAgainstAllQuestionsOfTheCategory() {
            given(categoryRepository.existsByCode("phobies")).willReturn(true);
            given(questionRepository.findByCategoryCode("phobies")).willReturn(List.of(PHOBIE_1, PHOBIE_2));

            QuizResult result = quizService.submitQuiz("phobies", new QuizSubmission(Map.of(6L, 1, 7L, 1)));

            assertThat(result.score()).isEqualTo(1);
            assertThat(result.total()).isEqualTo(2);
        }

        @Test
        void unansweredQuestionOfTheCategory_countsInTheTotal() {
            given(categoryRepository.existsByCode("phobies")).willReturn(true);
            given(questionRepository.findByCategoryCode("phobies")).willReturn(List.of(PHOBIE_1, PHOBIE_2));

            QuizResult result = quizService.submitQuiz("phobies", new QuizSubmission(Map.of(6L, 1)));

            assertThat(result.score()).isEqualTo(1);
            assertThat(result.total()).isEqualTo(2);
        }

        @Test
        void answerToQuestionOfAnotherCategory_isRejected() {
            given(categoryRepository.existsByCode("phobies")).willReturn(true);
            given(questionRepository.findByCategoryCode("phobies")).willReturn(List.of(PHOBIE_1, PHOBIE_2));

            assertThatThrownBy(() -> quizService.submitQuiz("phobies", new QuizSubmission(Map.of(2L, 1))))
                    .isInstanceOf(InvalidSubmissionException.class);
        }

        @Test
        void unknownCategory_throwsWithoutQueryingQuestions() {
            given(categoryRepository.existsByCode("inconnue")).willReturn(false);

            assertThatThrownBy(() -> quizService.submitQuiz("inconnue", new QuizSubmission(Map.of(6L, 1))))
                    .isInstanceOf(UnknownCategoryException.class);
            then(questionRepository).shouldHaveNoInteractions();
        }
    }
}
