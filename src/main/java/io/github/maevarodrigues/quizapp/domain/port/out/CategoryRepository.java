package io.github.maevarodrigues.quizapp.domain.port.out;

import io.github.maevarodrigues.quizapp.domain.model.Category;

import java.util.List;

/**
 * Port de sortie : accès au référentiel des catégories.
 * <p>
 * Le domaine définit ce dont il a besoin ; l'adaptateur de persistance (jOOQ) l'implémente.
 */
public interface CategoryRepository {

    /** Toutes les catégories, triées par id. */
    List<Category> findAll();

    boolean existsByCode(String code);
}
