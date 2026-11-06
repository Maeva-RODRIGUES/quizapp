package io.github.maevarodrigues.quizapp.adapters.in.batch;

import java.util.List;
import java.util.Objects;

/**
 * Une ligne du fichier CSV d'import, telle que lue, <b>avant</b> toute validation : aucun champ n'est garanti
 * correct. Les valeurs absentes sont ramenées à une chaîne vide, pour que les règles n'aient pas à gérer {@code null}.
 *
 * @param lineNumber    numéro de la ligne dans le fichier (l'en-tête est la ligne 1)
 * @param rawLine       ligne exacte du fichier, pour la retrouver et la corriger (traçabilité)
 * @param category      code de catégorie annoncé
 * @param text          énoncé de la question
 * @param options       colonnes {@code option_1} à {@code option_n}, vides comprises, dans l'ordre du fichier
 * @param correctOption numéro de la bonne réponse, <b>à partir de 1</b> (convention lisible par un humain),
 *                      encore sous forme de texte : sa conversion fait partie de la validation
 */
public record QuestionCsvLine(int lineNumber, String rawLine, String category, String text,
                              List<String> options, String correctOption) {

    public QuestionCsvLine {
        if (lineNumber < 1) {
            throw new IllegalArgumentException("lineNumber must be positive, but was: " + lineNumber);
        }
        rawLine = Objects.requireNonNullElse(rawLine, "");
        category = Objects.requireNonNullElse(category, "");
        text = Objects.requireNonNullElse(text, "");
        options = options == null ? List.of()
                : options.stream().map(option -> Objects.requireNonNullElse(option, "")).toList();
        correctOption = Objects.requireNonNullElse(correctOption, "");
    }
}
