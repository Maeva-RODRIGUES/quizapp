package io.github.maevarodrigues.quizapp.domain.port.in;

import io.github.maevarodrigues.quizapp.domain.exception.UnknownImportException;
import io.github.maevarodrigues.quizapp.domain.model.ImportRejection;
import io.github.maevarodrigues.quizapp.domain.model.ImportReport;

import java.nio.file.Path;
import java.util.List;

/**
 * Cas d'usage : importer un fichier CSV de questions et consulter le rapport de qualité d'un import.
 */
public interface QuestionFileImportUseCase {

    /** Importe le fichier et renvoie le bilan une fois l'import terminé (traitement synchrone). */
    ImportReport importFile(Path csvFile);

    /**
     * Les lignes rejetées par un import, triées par numéro de ligne (une entrée par motif).
     *
     * @throws UnknownImportException si aucun import ne porte cet identifiant
     */
    List<ImportRejection> findRejections(long importId);
}
