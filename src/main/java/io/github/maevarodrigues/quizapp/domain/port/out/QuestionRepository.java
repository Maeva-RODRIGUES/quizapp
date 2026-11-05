package io.github.maevarodrigues.quizapp.domain.port.out;

import io.github.maevarodrigues.quizapp.domain.model.Question;

import java.util.List;

/**
 * Port de sortie : accès aux questions (avec leurs réponses proposées).
 */
public interface QuestionRepository {

    /** Toutes les questions, triées par id. */
    List<Question> findAll();

    /** Les questions d'une catégorie, triées par id ; liste vide si la catégorie n'en a aucune. */
    List<Question> findByCategoryCode(String categoryCode);
}
