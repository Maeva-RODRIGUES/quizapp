package io.github.maevarodrigues.quizapp.domain.port.in;

import io.github.maevarodrigues.quizapp.domain.model.ImportRejection;
import io.github.maevarodrigues.quizapp.domain.model.Question;
import io.github.maevarodrigues.quizapp.domain.model.QuestionKey;

import java.util.List;

/**
 * Cas d'usage : importer des questions depuis une source externe (fichier CSV via Spring Batch).
 * <p>
 * L'adaptateur d'import décide ligne par ligne (règles de qualité) ; ce port lui donne les informations
 * de référence dont il a besoin, puis enregistre le résultat : les questions acceptées et les rejets.
 */
public interface ImportQuestionsUseCase {

    boolean categoryExists(String categoryCode);

    boolean questionExists(QuestionKey key);

    /** Enregistre des questions validées, tout ou rien ; renvoie les questions avec leur id. */
    List<Question> importQuestions(List<Question> questions);

    void recordRejections(List<ImportRejection> rejections);

    /** Les rejets enregistrés pour un import, triés par numéro de ligne. */
    List<ImportRejection> findRejections(long importId);
}
