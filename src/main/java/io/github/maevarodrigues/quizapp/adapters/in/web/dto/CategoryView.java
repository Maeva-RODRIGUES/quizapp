package io.github.maevarodrigues.quizapp.adapters.in.web.dto;

import io.github.maevarodrigues.quizapp.domain.model.Category;

/**
 * Représentation JSON d'une catégorie. Le code sert d'identifiant public (stable, lisible) :
 * l'id technique de la base n'est pas exposé.
 */
public record CategoryView(String code, String label) {

    public static CategoryView from(Category category) {
        return new CategoryView(category.code(), category.label());
    }
}
