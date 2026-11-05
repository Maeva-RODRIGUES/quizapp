package io.github.maevarodrigues.quizapp.application;

import io.github.maevarodrigues.quizapp.domain.model.Category;
import io.github.maevarodrigues.quizapp.domain.port.out.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Test unitaire du service : le port "out" est simulé par Mockito, aucune base ni Spring.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void listCategories_returnsCategoriesFromRepository() {
        List<Category> categories = List.of(
                new Category(1L, "superstitions", "Superstitions étranges"),
                new Category(2L, "phobies", "Phobies étranges"));
        given(categoryRepository.findAll()).willReturn(categories);

        assertThat(categoryService.listCategories()).isEqualTo(categories);
    }
}
