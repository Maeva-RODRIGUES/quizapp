package io.github.maevarodrigues.quizapp.domain.model;

/**
 * Correction d'une question.
 *
 * @param questionId         question corrigée
 * @param givenOptionIndex   réponse donnée, {@code null} si la question est restée sans réponse
 * @param correctOptionIndex bonne réponse
 */
public record QuestionCorrection(long questionId, Integer givenOptionIndex, int correctOptionIndex) {

    public QuestionCorrection {
        if (givenOptionIndex != null && givenOptionIndex < 0) {
            throw new IllegalArgumentException("givenOptionIndex must not be negative, but was: " + givenOptionIndex);
        }
        if (correctOptionIndex < 0) {
            throw new IllegalArgumentException("correctOptionIndex must not be negative, but was: " + correctOptionIndex);
        }
    }

    public boolean answered() {
        return givenOptionIndex != null;
    }

    public boolean correct() {
        return answered() && givenOptionIndex == correctOptionIndex;
    }
}
