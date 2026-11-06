package io.github.maevarodrigues.quizapp.adapters.in.batch;

import io.github.maevarodrigues.quizapp.domain.model.Question;

import java.util.List;
import java.util.Objects;

/**
 * Verdict de la validation d'une ligne : acceptée (avec la {@link Question} prête à écrire) <b>ou</b> rejetée
 * (avec ses motifs). {@code sealed} : il n'existe que ces deux cas, et un {@code switch} doit traiter les deux.
 */
public sealed interface ValidatedLine {

    QuestionCsvLine line();

    record Accepted(QuestionCsvLine line, Question question) implements ValidatedLine {

        public Accepted {
            Objects.requireNonNull(line, "line");
            Objects.requireNonNull(question, "question");
        }
    }

    record Rejected(QuestionCsvLine line, List<RejectionReason> reasons) implements ValidatedLine {

        public Rejected {
            Objects.requireNonNull(line, "line");
            if (reasons == null || reasons.isEmpty()) {
                throw new IllegalArgumentException("a rejected line needs at least one reason");
            }
            reasons = List.copyOf(reasons);
        }
    }
}
