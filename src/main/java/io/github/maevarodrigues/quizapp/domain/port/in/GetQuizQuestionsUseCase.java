package io.github.maevarodrigues.quizapp.domain.port.in;

import io.github.maevarodrigues.quizapp.domain.exception.UnknownCategoryException;
import io.github.maevarodrigues.quizapp.domain.model.Question;

import java.util.List;

/**
 * Cas d'usage : obtenir les questions à jouer.
 */
public interface GetQuizQuestionsUseCase {

    List<Question> getAllQuestions();

    /**
     * @throws UnknownCategoryException si la catégorie n'existe pas
     */
    List<Question> getQuestionsByCategory(String categoryCode);
}
