package io.github.maevarodrigues.quizapp.domain.exception;

/**
 * Soumission incohérente avec le quiz : réponse à une question qui n'en fait pas partie,
 * ou index de réponse inexistant. C'est une erreur du client, pas une mauvaise réponse.
 */
public class InvalidSubmissionException extends RuntimeException {

    public InvalidSubmissionException(String message) {
        super(message);
    }
}
