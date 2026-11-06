package io.github.maevarodrigues.quizapp.adapters.in.web.dto;

import io.github.maevarodrigues.quizapp.domain.model.ImportReport;

/**
 * Bilan d'un import renvoyé par {@code POST /api/imports}.
 *
 * @param executionId identifiant de l'import, à utiliser pour consulter ses rejets
 */
public record ImportReportView(long executionId, String status, long read, long written, long rejected) {

    public static ImportReportView from(ImportReport report) {
        return new ImportReportView(report.importId(), report.status(), report.read(), report.written(), report.rejected());
    }
}
