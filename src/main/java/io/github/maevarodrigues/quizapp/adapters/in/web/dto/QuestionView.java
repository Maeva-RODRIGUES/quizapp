package io.github.maevarodrigues.quizapp.adapters.in.web.dto;

import io.github.maevarodrigues.quizapp.domain.model.Question;

import java.util.List;

/**
 * Représentation JSON d'une question à jouer.
 * <p>
 * Volontairement SANS la bonne réponse : ce DTO est le contrat public de l'API, distinct du modèle
 * du domaine. Ajouter un champ au domaine ne peut pas le faire fuiter ici par accident.
 */
public record QuestionView(Long id, String category, String text, List<String> options) {

    public static QuestionView from(Question question) {
        return new QuestionView(question.id(), question.categoryCode(), question.text(), question.options());
    }
}
