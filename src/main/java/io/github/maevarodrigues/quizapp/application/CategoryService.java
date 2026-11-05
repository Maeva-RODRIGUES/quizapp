package io.github.maevarodrigues.quizapp.application;

import io.github.maevarodrigues.quizapp.domain.model.Category;
import io.github.maevarodrigues.quizapp.domain.port.in.ListCategoriesUseCase;
import io.github.maevarodrigues.quizapp.domain.port.out.CategoryRepository;

import java.util.List;

/**
 * Implémente les cas d'usage liés aux catégories.
 * <p>
 * Volontairement sans annotation Spring : le bean est déclaré dans la configuration (phase 5),
 * pour que la couche application reste indépendante du framework.
 */
public class CategoryService implements ListCategoriesUseCase {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<Category> listCategories() {
        return categoryRepository.findAll();
    }
}
