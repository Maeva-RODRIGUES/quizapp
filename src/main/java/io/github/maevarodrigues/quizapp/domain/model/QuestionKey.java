package io.github.maevarodrigues.quizapp.domain.model;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Identité métier d'une question : deux questions de la même catégorie dont les textes ne diffèrent
 * que par la casse ou les espaces sont des doublons.
 * <p>
 * La normalisation est la même que celle de la colonne générée {@code question.text_key}
 * ({@code lower(regexp_replace(btrim(text), '\s+', ' ', 'g'))}) : le dédoublonnage en Java et la
 * contrainte d'unicité en base donnent le même verdict.
 *
 * @param categoryCode   code de la catégorie
 * @param normalizedText texte sans espaces en début et fin, espaces internes réduits à un seul, en minuscules
 */
public record QuestionKey(String categoryCode, String normalizedText) {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public QuestionKey {
        if (categoryCode == null || categoryCode.isBlank()) {
            throw new IllegalArgumentException("categoryCode must not be blank");
        }
        if (normalizedText == null || normalizedText.isBlank()) {
            throw new IllegalArgumentException("normalizedText must not be blank");
        }
    }

    public static QuestionKey of(String categoryCode, String text) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        return new QuestionKey(categoryCode, normalize(text));
    }

    public static QuestionKey of(Question question) {
        return of(question.categoryCode(), question.text());
    }

    static String normalize(String text) {
        return WHITESPACE.matcher(text.strip()).replaceAll(" ").toLowerCase(Locale.ROOT);
    }
}
