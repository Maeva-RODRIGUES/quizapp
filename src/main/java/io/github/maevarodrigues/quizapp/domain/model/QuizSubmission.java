package io.github.maevarodrigues.quizapp.domain.model;

import io.github.maevarodrigues.quizapp.domain.exception.InvalidSubmissionException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Réponses d'un joueur : pour chaque question répondue, l'index de la réponse choisie.
 *
 * @param answers id de question → index de la réponse choisie (à partir de 0)
 */
public record QuizSubmission(Map<Long, Integer> answers) {

    public QuizSubmission {
        if (answers == null) {
            throw new IllegalArgumentException("answers must not be null");
        }
        answers.forEach((questionId, optionIndex) -> {
            if (questionId == null) {
                throw new IllegalArgumentException("question id must not be null");
            }
            if (optionIndex == null || optionIndex < 0) {
                throw new IllegalArgumentException(
                        "answer to question " + questionId + " must be a non-negative index, but was: " + optionIndex);
            }
        });
        answers = Map.copyOf(answers);
    }

    /**
     * Corrige la soumission. Règles :
     * <ul>
     *   <li>le total est le nombre de questions du quiz ;</li>
     *   <li>une question sans réponse compte comme fausse ;</li>
     *   <li>une réponse à une question hors du quiz, ou un index de réponse inexistant,
     *       rend la soumission invalide ({@link InvalidSubmissionException}).</li>
     * </ul>
     *
     * @param quizQuestions les questions du quiz, dans l'ordre d'affichage (déjà enregistrées : avec un id)
     * @return une correction par question, dans l'ordre du quiz
     */
    public QuizResult grade(List<Question> quizQuestions) {
        Set<Long> quizQuestionIds = requireDistinctIds(quizQuestions);

        for (Long answeredId : answers.keySet()) {
            if (!quizQuestionIds.contains(answeredId)) {
                throw new InvalidSubmissionException("question " + answeredId + " is not part of this quiz");
            }
        }

        List<QuestionCorrection> corrections = quizQuestions.stream()
                .map(this::correct)
                .toList();
        return new QuizResult(corrections);
    }

    private QuestionCorrection correct(Question question) {
        Integer given = answers.get(question.id());
        if (given != null && given >= question.options().size()) {
            throw new InvalidSubmissionException("answer to question " + question.id() + " must be between 0 and "
                    + (question.options().size() - 1) + ", but was: " + given);
        }
        return new QuestionCorrection(question.id(), given, question.correctOptionIndex());
    }

    private static Set<Long> requireDistinctIds(List<Question> quizQuestions) {
        if (quizQuestions == null) {
            throw new IllegalArgumentException("quiz questions must not be null");
        }
        Set<Long> ids = new HashSet<>();
        for (Question question : quizQuestions) {
            if (question.id() == null) {
                throw new IllegalArgumentException("only saved questions (with an id) can be graded");
            }
            if (!ids.add(question.id())) {
                throw new IllegalArgumentException("question " + question.id() + " appears twice in the quiz");
            }
        }
        return ids;
    }
}
