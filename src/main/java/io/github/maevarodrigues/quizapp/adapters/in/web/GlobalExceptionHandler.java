package io.github.maevarodrigues.quizapp.adapters.in.web;

import io.github.maevarodrigues.quizapp.domain.exception.InvalidSubmissionException;
import io.github.maevarodrigues.quizapp.domain.exception.UnknownCategoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Comparator;
import java.util.List;

/**
 * Traduit les exceptions en réponses HTTP au format Problem Details (RFC 9457, {@code application/problem+json}),
 * pour tous les controllers.
 * <p>
 * {@link ResponseEntityExceptionHandler} gère déjà les erreurs de Spring MVC (JSON illisible, 415, 405, route
 * inconnue…) ; on y ajoute les exceptions du domaine et un filet de sécurité pour le reste.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Une erreur de champ : quel champ, et quelle règle n'est pas respectée. */
    public record FieldViolation(String field, String message) {
    }

    @ExceptionHandler(UnknownCategoryException.class)
    ProblemDetail handleUnknownCategory(UnknownCategoryException exception) {
        return problem(HttpStatus.NOT_FOUND, "Unknown category", exception.getMessage());
    }

    @ExceptionHandler(InvalidSubmissionException.class)
    ProblemDetail handleInvalidSubmission(InvalidSubmissionException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid submission", exception.getMessage());
    }

    /**
     * Filet de sécurité : toute exception imprévue devient un 500 sans détail technique.
     * La cause complète va dans les logs (pour nous), jamais dans la réponse (pour le client).
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unexpected error", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", "An unexpected error occurred.");
    }

    /** Erreurs de {@code @Valid} : on garde le format standard et on y ajoute la liste des champs en cause. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldViolation> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldViolation::field))
                .toList();

        ProblemDetail body = exception.getBody();
        body.setTitle("Invalid request");
        body.setDetail("The request contains invalid fields.");
        body.setProperty("errors", errors);
        return handleExceptionInternal(exception, body, headers, status, request);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
