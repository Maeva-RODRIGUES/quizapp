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

    /**
     * Enregistre de nouvelles questions avec leurs réponses, en une seule transaction : tout ou rien.
     *
     * @param questions questions pas encore enregistrées (id {@code null})
     * @return les mêmes questions, avec l'id attribué par la base, dans le même ordre
     * @throws IllegalArgumentException si une question a déjà un id
     * @throws io.github.maevarodrigues.quizapp.domain.exception.UnknownCategoryException si une catégorie n'existe pas
     */
    List<Question> saveAll(List<Question> questions);
}
