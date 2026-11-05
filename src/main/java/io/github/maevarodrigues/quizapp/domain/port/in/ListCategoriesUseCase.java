package io.github.maevarodrigues.quizapp.domain.port.in;

import io.github.maevarodrigues.quizapp.domain.model.Category;

import java.util.List;

/**
 * Cas d'usage : lister les catégories de quiz disponibles.
 */
public interface ListCategoriesUseCase {

    List<Category> listCategories();
}
