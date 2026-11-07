package io.github.maevarodrigues.quizapp.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Une ligne d'un fichier importé refusée par une règle de qualité : de quoi la retrouver et la corriger.
 * Une ligne refusée pour plusieurs motifs donne plusieurs rejets.
 *
 * @param importId   identifiant de l'import (l'exécution du job) qui a lu la ligne
 * @param lineNumber numéro de la ligne dans le fichier source (l'en-tête est la ligne 1)
 * @param rawLine    ligne telle que lue dans le fichier
 * @param reason     code du motif (majuscules et tirets bas, ex. {@code TOO_FEW_OPTIONS})
 */
public record ImportRejection(long importId, int lineNumber, String rawLine, String reason) {

    /** Quantificateurs possessifs (++, *+) : pas de retour arrière, donc pas de récursion profonde sur une longue chaîne. */
    private static final Pattern REASON_FORMAT = Pattern.compile("^[A-Z]++(?:_[A-Z]++)*+$");

    public ImportRejection {
        if (lineNumber < 1) {
            throw new IllegalArgumentException("lineNumber must be positive, but was: " + lineNumber);
        }
        Objects.requireNonNull(rawLine, "rawLine");
        if (reason == null || !REASON_FORMAT.matcher(reason).matches()) {
            throw new IllegalArgumentException("reason must be an UPPER_SNAKE_CASE code, but was: " + reason);
        }
    }
}
