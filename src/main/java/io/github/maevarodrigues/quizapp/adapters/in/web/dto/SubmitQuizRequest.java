package io.github.maevarodrigues.quizapp.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Map;

/**
 * Corps de {@code POST /api/quiz/submit}. Les annotations Bean Validation sont vérifiées par {@code @Valid}
 * avant d'appeler le cas d'usage : une requête malformée reçoit un 400 sans atteindre le domaine.
 *
 * @param category code de la catégorie jouée (ex. {@code phobies})
 * @param answers  id de question → index de la réponse choisie (à partir de 0) ; en JSON, les clés sont des chaînes
 */
public record SubmitQuizRequest(
        @NotBlank String category,
        @NotEmpty Map<@NotNull Long, @NotNull @PositiveOrZero Integer> answers) {
}
