package io.github.maevarodrigues.quizapp.adapters.in.web;

import io.github.maevarodrigues.quizapp.adapters.in.web.dto.CategoryView;
import io.github.maevarodrigues.quizapp.domain.port.in.ListCategoriesUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptateur REST : traduit HTTP ↔ cas d'usage. Ne connaît que le port « in », pas son implémentation.
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final ListCategoriesUseCase listCategories;

    public CategoryController(ListCategoriesUseCase listCategories) {
        this.listCategories = listCategories;
    }

    /** {@code GET /api/categories} : les catégories disponibles. */
    @GetMapping
    public List<CategoryView> getCategories() {
        return listCategories.listCategories().stream().map(CategoryView::from).toList();
    }
}
