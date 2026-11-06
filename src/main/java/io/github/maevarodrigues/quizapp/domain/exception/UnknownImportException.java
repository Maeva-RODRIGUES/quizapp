package io.github.maevarodrigues.quizapp.domain.exception;

/**
 * Aucun import ne porte cet identifiant.
 */
public class UnknownImportException extends RuntimeException {

    public UnknownImportException(long importId) {
        super("unknown import: " + importId);
    }
}
