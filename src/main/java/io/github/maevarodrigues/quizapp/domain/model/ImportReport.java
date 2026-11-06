package io.github.maevarodrigues.quizapp.domain.model;

import java.util.Objects;

/**
 * Bilan d'un import de fichier.
 *
 * @param importId identifiant de l'import (l'exécution du job), pour consulter ensuite ses rejets
 * @param status   état final de l'import (ex. {@code COMPLETED}, {@code FAILED})
 * @param read     lignes de données lues (en-tête exclu), y compris celles rejetées
 * @param written  questions enregistrées
 * @param rejected lignes rejetées (une ligne rejetée pour plusieurs motifs compte une fois)
 */
public record ImportReport(long importId, String status, long read, long written, long rejected) {

    public static final String COMPLETED = "COMPLETED";

    public ImportReport {
        Objects.requireNonNull(status, "status");
        if (read < 0 || written < 0 || rejected < 0) {
            throw new IllegalArgumentException("counters must not be negative");
        }
        if (written + rejected > read) {
            throw new IllegalArgumentException("written + rejected (" + (written + rejected)
                    + ") cannot exceed read (" + read + ")");
        }
    }

    public boolean completed() {
        return COMPLETED.equals(status);
    }
}
