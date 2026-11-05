package io.github.maevarodrigues.quizapp.domain.model;

import java.util.List;

/**
 * Résultat d'un quiz corrigé : une correction par question, dans l'ordre du quiz.
 */
public record QuizResult(List<QuestionCorrection> corrections) {

    public QuizResult {
        if (corrections == null) {
            throw new IllegalArgumentException("corrections must not be null");
        }
        corrections = List.copyOf(corrections);
    }

    /** Nombre de bonnes réponses. */
    public int score() {
        return (int) corrections.stream().filter(QuestionCorrection::correct).count();
    }

    /** Nombre de questions du quiz. */
    public int total() {
        return corrections.size();
    }
}
