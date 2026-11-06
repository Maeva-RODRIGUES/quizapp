package io.github.maevarodrigues.quizapp.adapters.in.batch;

import java.util.List;

/**
 * Levée par le processor quand une ligne enfreint une règle de qualité. Spring Batch la « saute »
 * (skip) : la ligne n'est pas écrite, le job continue, et le {@link RejectionSkipListener} enregistre le rejet.
 */
public class InvalidQuestionException extends RuntimeException {

    private final transient ValidatedLine.Rejected rejected;

    public InvalidQuestionException(ValidatedLine.Rejected rejected) {
        super("line " + rejected.line().lineNumber() + " rejected: " + rejected.reasons());
        this.rejected = rejected;
    }

    public QuestionCsvLine line() {
        return rejected.line();
    }

    public List<RejectionReason> reasons() {
        return rejected.reasons();
    }
}
