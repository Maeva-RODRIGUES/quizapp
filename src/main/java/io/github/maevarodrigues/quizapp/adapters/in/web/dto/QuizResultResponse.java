package io.github.maevarodrigues.quizapp.adapters.in.web.dto;

import io.github.maevarodrigues.quizapp.domain.model.QuestionCorrection;
import io.github.maevarodrigues.quizapp.domain.model.QuizResult;

import java.util.List;

/**
 * Réponse de {@code POST /api/quiz/submit} : le score et la correction de chaque question du quiz.
 * Après soumission, la bonne réponse peut être révélée.
 */
public record QuizResultResponse(int score, int total, List<CorrectionView> corrections) {

    public static QuizResultResponse from(QuizResult result) {
        return new QuizResultResponse(result.score(), result.total(),
                result.corrections().stream().map(CorrectionView::from).toList());
    }

    /**
     * @param givenOptionIndex {@code null} si la question est restée sans réponse
     */
    public record CorrectionView(long questionId, Integer givenOptionIndex, int correctOptionIndex, boolean correct) {

        static CorrectionView from(QuestionCorrection correction) {
            return new CorrectionView(correction.questionId(), correction.givenOptionIndex(),
                    correction.correctOptionIndex(), correction.correct());
        }
    }
}
