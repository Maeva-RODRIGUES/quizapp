package io.github.maevarodrigues.quizapp.domain.model;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Question du quiz. Une instance est toujours valide : le constructeur refuse tout état incohérent.
 *
 * @param id                 identifiant technique, {@code null} tant que la question n'est pas enregistrée
 * @param categoryCode       code de la catégorie (voir {@link Category#code()})
 * @param text               énoncé, sans espaces superflus en début et fin
 * @param options            réponses proposées, dans l'ordre d'affichage (liste immuable)
 * @param correctOptionIndex position de la bonne réponse dans {@code options}, à partir de 0
 */
public record Question(Long id, String categoryCode, String text, List<String> options, int correctOptionIndex) {

    public static final int TEXT_MAX_LENGTH = 500;
    public static final int OPTION_MAX_LENGTH = 200;
    public static final int MIN_OPTIONS = 2;

    public Question {
        categoryCode = Category.requireValidCode(categoryCode, "categoryCode");
        text = requireText(text);
        options = requireOptions(options);
        if (correctOptionIndex < 0 || correctOptionIndex >= options.size()) {
            throw new IllegalArgumentException("correctOptionIndex must be between 0 and " + (options.size() - 1)
                    + ", but was: " + correctOptionIndex);
        }
    }

    /** La bonne réponse. */
    public String correctOption() {
        return options.get(correctOptionIndex);
    }

    private static String requireText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        String stripped = text.strip();
        if (stripped.length() > TEXT_MAX_LENGTH) {
            throw new IllegalArgumentException("text must not exceed " + TEXT_MAX_LENGTH + " characters");
        }
        return stripped;
    }

    private static List<String> requireOptions(List<String> options) {
        if (options == null) {
            throw new IllegalArgumentException("options must not be null");
        }
        if (options.size() < MIN_OPTIONS) {
            throw new IllegalArgumentException("a question needs at least " + MIN_OPTIONS + " options, but had: " + options.size());
        }
        Set<String> seen = new HashSet<>();
        List<String> stripped = options.stream().map(Question::requireOption).toList();
        for (String option : stripped) {
            if (!seen.add(option.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("options must not contain duplicates, but found: " + option);
            }
        }
        // toList() renvoie une liste non modifiable et indépendante de la liste reçue (copie défensive).
        return stripped;
    }

    private static String requireOption(String option) {
        if (option == null || option.isBlank()) {
            throw new IllegalArgumentException("an option must not be blank");
        }
        String stripped = option.strip();
        if (stripped.length() > OPTION_MAX_LENGTH) {
            throw new IllegalArgumentException("an option must not exceed " + OPTION_MAX_LENGTH + " characters");
        }
        return stripped;
    }
}
