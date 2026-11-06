package io.github.maevarodrigues.quizapp.adapters.in.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.listener.JobExecutionListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Supprime le fichier importé à la fin du job (succès ou échec), <b>seulement</b> s'il a été marqué
 * comme temporaire au lancement : on ne supprime jamais un fichier qui ne nous appartient pas.
 */
public class TempFileCleanupListener implements JobExecutionListener {

    /** Paramètre non identifiant : « true » si le fichier d'entrée est une copie temporaire à supprimer. */
    public static final String TEMPORARY_INPUT = "input.temporary";

    private static final Logger log = LoggerFactory.getLogger(TempFileCleanupListener.class);

    @Override
    public void afterJob(JobExecution jobExecution) {
        JobParameters parameters = jobExecution.getJobParameters();
        String inputFile = parameters.getString(ImportQuestionsJobConfig.INPUT_FILE);
        if (!"true".equals(parameters.getString(TEMPORARY_INPUT)) || inputFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(inputFile));
        } catch (IOException e) {
            // Un fichier temporaire oublié ne doit pas changer le résultat de l'import.
            log.warn("Could not delete temporary import file {}", inputFile, e);
        }
    }
}
