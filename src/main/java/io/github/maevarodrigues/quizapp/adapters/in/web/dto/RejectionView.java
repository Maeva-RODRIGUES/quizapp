package io.github.maevarodrigues.quizapp.adapters.in.web.dto;

import io.github.maevarodrigues.quizapp.domain.model.ImportRejection;

/**
 * Une ligne rejetée, telle que renvoyée par {@code GET /api/imports/{id}/rejections}.
 */
public record RejectionView(int lineNumber, String reason, String rawLine) {

    public static RejectionView from(ImportRejection rejection) {
        return new RejectionView(rejection.lineNumber(), rejection.reason(), rejection.rawLine());
    }
}
