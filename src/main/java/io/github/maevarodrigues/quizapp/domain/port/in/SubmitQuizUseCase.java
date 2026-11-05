package io.github.maevarodrigues.quizapp.domain.port.in;

import io.github.maevarodrigues.quizapp.domain.exception.InvalidSubmissionException;
import io.github.maevarodrigues.quizapp.domain.exception.UnknownCategoryException;
import io.github.maevarodrigues.quizapp.domain.model.QuizResult;
import io.github.maevarodrigues.quizapp.domain.model.QuizSubmission;

/**
 * Cas d'usage : soumettre ses réponses au quiz d'une catégorie et obtenir sa correction.
 * <p>
 * Le quiz est formé de toutes les questions de la catégorie : une question sans réponse compte comme fausse.
 */
public interface SubmitQuizUseCase {

    /**
     * @throws UnknownCategoryException   si la catégorie n'existe pas
     * @throws InvalidSubmissionException si une réponse vise une question hors de la catégorie ou un index inexistant
     */
    QuizResult submitQuiz(String categoryCode, QuizSubmission submission);
}
