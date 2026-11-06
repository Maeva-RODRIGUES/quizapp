package io.github.maevarodrigues.quizapp.adapters.in.batch;

import io.github.maevarodrigues.quizapp.domain.model.Question;
import io.github.maevarodrigues.quizapp.domain.model.QuestionKey;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import static io.github.maevarodrigues.quizapp.adapters.in.batch.RejectionReason.*;

/**
 * Règles de qualité des données de l'import : transforme une {@link QuestionCsvLine} en question valide
 * <b>ou</b> en liste de motifs de rejet. Toutes les règles sont évaluées, pour qu'un rejet liste tous les problèmes
 * de la ligne en une seule fois.
 * <p>
 * <b>Avec état</b> : il mémorise les questions acceptées pour détecter les doublons dans le fichier.
 * Il faut donc <b>une instance par import</b>, et il n'est pas thread-safe.
 */
public class QuestionImportValidator {

    private final Predicate<String> categoryExists;
    private final Predicate<QuestionKey> questionAlreadyExists;
    private final Set<QuestionKey> acceptedInThisFile = new HashSet<>();

    /**
     * @param categoryExists        la catégorie (par son code) existe-t-elle dans le référentiel ?
     * @param questionAlreadyExists une question de même clé est-elle déjà en base ?
     */
    public QuestionImportValidator(Predicate<String> categoryExists, Predicate<QuestionKey> questionAlreadyExists) {
        this.categoryExists = Objects.requireNonNull(categoryExists, "categoryExists");
        this.questionAlreadyExists = Objects.requireNonNull(questionAlreadyExists, "questionAlreadyExists");
    }

    public ValidatedLine validate(QuestionCsvLine line) {
        List<RejectionReason> reasons = new ArrayList<>();

        String category = line.category().strip();
        boolean knownCategory = !category.isEmpty() && categoryExists.test(category);
        if (!knownCategory) {
            reasons.add(UNKNOWN_CATEGORY);
        }

        String text = line.text().strip();
        if (text.isEmpty()) {
            reasons.add(EMPTY_TEXT);
        } else if (text.length() > Question.TEXT_MAX_LENGTH) {
            reasons.add(TEXT_TOO_LONG);
        }

        List<String> options = checkOptions(line.options(), reasons);
        Integer correctIndex = checkCorrectOption(line.correctOption(), options.size(), reasons);

        // Le dédoublonnage n'a de sens que si la clé (catégorie + énoncé) est fiable.
        QuestionKey key = knownCategory && !text.isEmpty() ? QuestionKey.of(category, text) : null;
        if (key != null) {
            if (acceptedInThisFile.contains(key)) {
                reasons.add(DUPLICATE_IN_FILE);
            } else if (questionAlreadyExists.test(key)) {
                reasons.add(ALREADY_EXISTS);
            }
        }

        if (!reasons.isEmpty()) {
            return new ValidatedLine.Rejected(line, reasons);
        }
        // Seule une ligne acceptée compte pour le dédoublonnage : si la 1re occurrence est rejetée,
        // une copie correcte plus bas dans le fichier doit pouvoir passer.
        acceptedInThisFile.add(key);
        // Les invariants du domaine restent le dernier filet : si une règle ci-dessus en oubliait un,
        // le constructeur lèverait une exception (un bug de notre code, pas une ligne à rejeter).
        return new ValidatedLine.Accepted(line, new Question(null, category, text, options, correctIndex));
    }

    /**
     * Les colonnes de réponse vides en fin de ligne sont ignorées (toutes les questions n'ont pas 4 réponses).
     * Un trou au milieu est refusé : il décalerait la numérotation de la bonne réponse.
     */
    private static List<String> checkOptions(List<String> columns, List<RejectionReason> reasons) {
        int last = columns.size() - 1;
        while (last >= 0 && columns.get(last).isBlank()) {
            last--;
        }
        List<String> options = columns.subList(0, last + 1).stream().map(String::strip).toList();

        if (options.stream().anyMatch(String::isEmpty)) {
            reasons.add(OPTIONS_NOT_CONTIGUOUS);
        }
        List<String> filled = options.stream().filter(option -> !option.isEmpty()).toList();
        if (filled.size() < Question.MIN_OPTIONS) {
            reasons.add(TOO_FEW_OPTIONS);
        }
        if (filled.stream().anyMatch(option -> option.length() > Question.OPTION_MAX_LENGTH)) {
            reasons.add(OPTION_TOO_LONG);
        }
        if (filled.stream().map(option -> option.toLowerCase(Locale.ROOT)).distinct().count() < filled.size()) {
            reasons.add(DUPLICATE_OPTIONS);
        }
        return options;
    }

    /**
     * Convertit le numéro lisible par un humain (à partir de 1) en index (à partir de 0).
     *
     * @return l'index, ou {@code null} si le numéro est invalide (le motif est alors ajouté)
     */
    private static Integer checkCorrectOption(String value, int optionCount, List<RejectionReason> reasons) {
        int number;
        try {
            number = Integer.parseInt(value.strip());
        } catch (NumberFormatException e) {
            reasons.add(CORRECT_OPTION_NOT_A_NUMBER);
            return null;
        }
        if (number < 1 || number > optionCount) {
            reasons.add(CORRECT_OPTION_OUT_OF_RANGE);
            return null;
        }
        return number - 1;
    }
}
