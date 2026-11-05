package io.github.maevarodrigues.quizapp.domain.exception;

/**
 * La catégorie demandée n'existe pas dans le référentiel.
 */
public class UnknownCategoryException extends RuntimeException {

    public UnknownCategoryException(String categoryCode) {
        super("unknown category: " + categoryCode);
    }
}
