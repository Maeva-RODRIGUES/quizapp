package io.github.maevarodrigues.quizapp.domain.port.in;

import io.github.maevarodrigues.quizapp.domain.exception.UnknownImportException;
import io.github.maevarodrigues.quizapp.domain.model.ImportRejection;
import io.github.maevarodrigues.quizapp.domain.model.ImportReport;

import java.nio.file.Path;
import java.util.List;

/**
 * Cas d'usage : importer un fichier CSV de questions, suivre l'import et consulter son rapport de qualité.
 */
public interface QuestionFileImportUseCase {

    /**
     * Démarre l'import <b>en arrière-plan</b> et rend la main aussitôt.
     * <p>
     * Le fichier appartient désormais à l'import : il sera supprimé quand celui-ci se termine (succès ou échec).
     *
     * @return l'état initial de l'import, dont son identifiant pour le suivre
     */
    ImportReport startImport(Path csvFile);

    /**
     * L'état actuel d'un import : statut, dates et compteurs.
     *
     * @throws UnknownImportException si aucun import ne porte cet identifiant
     */
    ImportReport findImport(long importId);

    /**
     * Les lignes rejetées par un import, triées par numéro de ligne (une entrée par motif).
     *
     * @throws UnknownImportException si aucun import ne porte cet identifiant
     */
    List<ImportRejection> findRejections(long importId);
}
