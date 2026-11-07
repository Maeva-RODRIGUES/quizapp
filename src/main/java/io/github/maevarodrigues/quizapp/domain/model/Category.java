package io.github.maevarodrigues.quizapp.domain.model;

import java.util.regex.Pattern;

/**
 * Catégorie de questions (donnée de référence).
 *
 * @param id    identifiant technique, {@code null} tant que la catégorie n'est pas enregistrée
 * @param code  identifiant métier stable, utilisé dans l'API et les fichiers d'import (ex. {@code phobies})
 * @param label libellé affiché
 */
public record Category(Long id, String code, String label) {

    /** Minuscules, chiffres et tirets internes : même règle que la contrainte {@code ck_category_code_format}. */
    private static final Pattern CODE_FORMAT = Pattern.compile("^[a-z0-9]++(?:-[a-z0-9]++)*+$");

    public Category {
        code = requireValidCode(code, "code");
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        label = label.strip();
    }

    /**
     * Vérifie le format d'un code de catégorie. Partagée avec {@link Question}, qui référence sa catégorie par son code.
     */
    static String requireValidCode(String code, String fieldName) {
        if (code == null || !CODE_FORMAT.matcher(code).matches()) {
            throw new IllegalArgumentException(
                    fieldName + " must contain only lowercase letters, digits and inner hyphens, but was: " + code);
        }
        return code;
    }
}
