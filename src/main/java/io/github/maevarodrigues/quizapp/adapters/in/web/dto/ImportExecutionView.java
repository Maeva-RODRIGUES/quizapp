package io.github.maevarodrigues.quizapp.adapters.in.web.dto;

import io.github.maevarodrigues.quizapp.domain.model.ImportReport;

import java.time.LocalDateTime;

/**
 * État d'un import, renvoyé par {@code POST /api/imports} (au lancement) et {@code GET /api/imports/{id}} (suivi).
 *
 * @param executionId identifiant de l'import, à utiliser pour le suivre et consulter ses rejets
 * @param startTime   {@code null} tant que l'import n'a pas commencé
 * @param endTime     {@code null} tant que l'import n'est pas terminé
 */
public record ImportExecutionView(long executionId, String status, LocalDateTime startTime, LocalDateTime endTime,
                                  long read, long written, long rejected) {

    public static ImportExecutionView from(ImportReport report) {
        return new ImportExecutionView(report.importId(), report.status(), report.startTime(), report.endTime(),
                report.read(), report.written(), report.rejected());
    }
}
