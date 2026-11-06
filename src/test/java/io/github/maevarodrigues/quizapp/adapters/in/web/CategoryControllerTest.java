package io.github.maevarodrigues.quizapp.adapters.in.web;

import io.github.maevarodrigues.quizapp.domain.model.Category;
import io.github.maevarodrigues.quizapp.domain.port.in.ListCategoriesUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListCategoriesUseCase listCategories;

    @Test
    void returnsCategoriesWithCodeAndLabelButNotTheTechnicalId() throws Exception {
        given(listCategories.listCategories()).willReturn(List.of(
                new Category(1L, "superstitions", "Superstitions étranges"),
                new Category(2L, "phobies", "Phobies étranges")));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("superstitions"))
                .andExpect(jsonPath("$[0].label").value("Superstitions étranges"))
                .andExpect(jsonPath("$[1].code").value("phobies"))
                .andExpect(jsonPath("$[0].id").doesNotExist());
    }
}
