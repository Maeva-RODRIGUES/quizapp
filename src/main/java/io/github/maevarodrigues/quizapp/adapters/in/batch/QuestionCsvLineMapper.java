package io.github.maevarodrigues.quizapp.adapters.in.batch;

import org.springframework.batch.infrastructure.item.file.LineMapper;
import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.infrastructure.item.file.transform.FieldSet;

import java.util.List;

/**
 * Découpe une ligne du CSV ({@code ;}, guillemets acceptés) en {@link QuestionCsvLine}, en gardant
 * le numéro de ligne et la ligne brute que Spring Batch fournit.
 * <p>
 * Une ligne qui n'a pas exactement 7 colonnes fait échouer le découpage : le reader l'enveloppe dans une
 * {@code FlatFileParseException} (avec numéro et contenu de la ligne), sautée et rapportée en {@code MALFORMED_LINE}.
 */
public class QuestionCsvLineMapper implements LineMapper<QuestionCsvLine> {

    static final String[] COLUMNS =
            {"category", "question", "option_1", "option_2", "option_3", "option_4", "correct_option"};
    static final String EXPECTED_HEADER = String.join(";", COLUMNS);

    private static final char BYTE_ORDER_MARK = '﻿';

    private final DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(";");

    public QuestionCsvLineMapper() {
        tokenizer.setNames(COLUMNS);
        tokenizer.setStrict(true); // nombre de colonnes exact, sinon exception
    }

    @Override
    public QuestionCsvLine mapLine(String line, int lineNumber) {
        FieldSet fields = tokenizer.tokenize(line);
        return new QuestionCsvLine(lineNumber, line,
                fields.readRawString("category"),
                fields.readRawString("question"),
                List.of(fields.readRawString("option_1"), fields.readRawString("option_2"),
                        fields.readRawString("option_3"), fields.readRawString("option_4")),
                fields.readRawString("correct_option"));
    }

    /**
     * Vérifie l'en-tête (ligne 1) : un fichier au mauvais format fait échouer l'import tout de suite,
     * au lieu de produire autant de rejets que de lignes. Tolère le BOM ajouté par certains tableurs.
     */
    public static void checkHeader(String header) {
        String normalized = header.isEmpty() || header.charAt(0) != BYTE_ORDER_MARK ? header : header.substring(1);
        if (!normalized.strip().equals(EXPECTED_HEADER)) {
            throw new IllegalArgumentException("unexpected CSV header: expected '" + EXPECTED_HEADER
                    + "' but was '" + header + "'");
        }
    }
}
