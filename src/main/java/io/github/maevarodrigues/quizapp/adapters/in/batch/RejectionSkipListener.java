package io.github.maevarodrigues.quizapp.adapters.in.batch;

import io.github.maevarodrigues.quizapp.domain.model.ImportRejection;
import io.github.maevarodrigues.quizapp.domain.model.Question;
import io.github.maevarodrigues.quizapp.domain.port.in.ImportQuestionsUseCase;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;

import java.util.List;

/**
 * Écrit le rapport de rejets : chaque ligne sautée par le step devient une ou plusieurs lignes
 * de {@code import_rejection} (une par motif), rattachées à l'exécution du job.
 * Appelé dans la transaction du chunk : si le chunk est annulé, ses rejets le sont aussi.
 */
public class RejectionSkipListener implements SkipListener<QuestionCsvLine, Question> {

    private final ImportQuestionsUseCase importQuestions;
    private final long importId;

    public RejectionSkipListener(ImportQuestionsUseCase importQuestions, long importId) {
        this.importQuestions = importQuestions;
        this.importId = importId;
    }

    /** Ligne illisible : numéro et contenu viennent de l'exception, puisqu'aucun item n'a pu être construit. */
    @Override
    public void onSkipInRead(Throwable failure) {
        if (!(failure instanceof FlatFileParseException parseException)) {
            throw new IllegalStateException("only parse errors are skipped in read", failure);
        }
        importQuestions.recordRejections(List.of(new ImportRejection(importId, parseException.getLineNumber(),
                parseException.getInput(), RejectionReason.MALFORMED_LINE.name())));
    }

    /** Ligne lisible mais invalide : l'item porte son numéro de ligne et sa ligne brute. */
    @Override
    public void onSkipInProcess(QuestionCsvLine line, Throwable failure) {
        if (!(failure instanceof InvalidQuestionException invalid)) {
            throw new IllegalStateException("only data quality errors are skipped in process", failure);
        }
        importQuestions.recordRejections(invalid.reasons().stream()
                .map(reason -> new ImportRejection(importId, line.lineNumber(), line.rawLine(), reason.name()))
                .toList());
    }
}
