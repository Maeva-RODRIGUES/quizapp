package io.github.maevarodrigues.quizapp.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * État d'un import de fichier, pendant ou après son exécution.
 *
 * @param importId  identifiant de l'import (l'exécution du job)
 * @param status    état (ex. {@code STARTING}, {@code STARTED}, {@code COMPLETED}, {@code FAILED})
 * @param startTime début du traitement, {@code null} tant qu'il n'a pas commencé
 * @param endTime   fin du traitement, {@code null} tant qu'il n'est pas terminé
 * @param read      lignes de données lues jusqu'ici (en-tête exclu), y compris celles rejetées
 * @param written   questions enregistrées
 * @param rejected  lignes rejetées (une ligne rejetée pour plusieurs motifs compte une fois)
 */
public record ImportReport(long importId, String status, LocalDateTime startTime, LocalDateTime endTime,
                           long read, long written, long rejected) {

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
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("endTime must not be before startTime");
        }
    }

    public boolean completed() {
        return COMPLETED.equals(status);
    }

    /** Terminé, quel que soit le résultat. */
    public boolean finished() {
        return endTime != null;
    }
}
