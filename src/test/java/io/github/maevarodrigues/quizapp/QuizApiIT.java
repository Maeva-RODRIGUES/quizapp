package io.github.maevarodrigues.quizapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bout en bout : HTTP → controller → service → adaptateur jOOQ → PostgreSQL (données de démo).
 * Vérifie que tout l'hexagone est bien assemblé, ce que les tests par couche ne prouvent pas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import(TestcontainersConfiguration.class)
class QuizApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getCategories_readsTheReferenceCategoriesFromTheDatabase() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code").value(contains("superstitions", "phobies")));
    }

    @Test
    void getQuestions_readsTheTenDemoQuestionsFromTheDatabase() throws Exception {
        mockMvc.perform(get("/api/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[5].text").value("Quel est le nom de la peur des araignées ?"))
                .andExpect(jsonPath("$[5].options.length()").value(3));
    }

    @Test
    void getQuestionsByCategory_filtersOnTheCategoryCode() throws Exception {
        mockMvc.perform(get("/api/questions").param("category", "phobies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[*].category").value(everyItem(is("phobies"))));
    }

    @Test
    void submitQuiz_allCorrect_scoresFiveOutOfFive() throws Exception {
        // Bonnes réponses des phobies de démo : 6 → 2, 7 → 0, 8 → 1, 9 → 2, 10 → 0
        mockMvc.perform(post("/api/quiz/submit").contentType(MediaType.APPLICATION_JSON).content("""
                        {"category": "phobies", "answers": {"6": 2, "7": 0, "8": 1, "9": 2, "10": 0}}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(5))
                .andExpect(jsonPath("$.total").value(5));
    }

    @Test
    void submitQuiz_partialAnswers_countsUnansweredQuestionsInTheTotal() throws Exception {
        mockMvc.perform(post("/api/quiz/submit").contentType(MediaType.APPLICATION_JSON).content("""
                        {"category": "phobies", "answers": {"6": 2, "7": 1}}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(1))
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.corrections[1].correct").value(false))
                .andExpect(jsonPath("$.corrections[1].correctOptionIndex").value(0))
                .andExpect(jsonPath("$.corrections[2].givenOptionIndex").isEmpty());
    }

    @Test
    void getQuestions_unknownCategory_returns404ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/questions").param("category", "inconnue"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("unknown category: inconnue"));
    }

    @Test
    void submitQuiz_questionFromAnotherCategory_returns400() throws Exception {
        // la question 1 est une superstition : elle ne fait pas partie du quiz des phobies
        mockMvc.perform(post("/api/quiz/submit").contentType(MediaType.APPLICATION_JSON).content("""
                        {"category": "phobies", "answers": {"1": 0}}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid submission"));
    }

    @Test
    void unknownRoute_returns404ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/inexistant"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void responsesNeverContainTheCorrectAnswer() throws Exception {
        mockMvc.perform(get("/api/questions"))
                .andExpect(content().string(not(containsStringIgnoringCase("correct"))));
    }
}
