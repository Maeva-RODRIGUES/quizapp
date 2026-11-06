package io.github.maevarodrigues.quizapp.adapters.in.batch;

import io.github.maevarodrigues.quizapp.domain.exception.UnknownImportException;
import io.github.maevarodrigues.quizapp.domain.model.ImportRejection;
import io.github.maevarodrigues.quizapp.domain.model.ImportReport;
import io.github.maevarodrigues.quizapp.domain.port.in.ImportQuestionsUseCase;
import io.github.maevarodrigues.quizapp.domain.port.in.QuestionFileImportUseCase;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Implémente l'import de fichier avec Spring Batch : lance le job (en arrière-plan, voir
 * {@code BatchAsyncConfiguration}) et relit son état dans les tables de Spring Batch.
 * Traduit le vocabulaire Batch ({@code JobExecution}, {@code readCount}…) en {@link ImportReport} du domaine :
 * le controller REST ne connaît que le port.
 */
@Component
public class BatchQuestionFileImport implements QuestionFileImportUseCase {

    private final JobOperator jobOperator;
    private final Job importQuestionsJob;
    private final JobRepository jobRepository;
    private final ImportQuestionsUseCase importQuestions;

    public BatchQuestionFileImport(JobOperator jobOperator, Job importQuestionsJob, JobRepository jobRepository,
                                   ImportQuestionsUseCase importQuestions) {
        this.jobOperator = jobOperator;
        this.importQuestionsJob = importQuestionsJob;
        this.jobRepository = jobRepository;
        this.importQuestions = importQuestions;
    }

    @Override
    public ImportReport startImport(Path csvFile) {
        JobParameters parameters = new JobParametersBuilder()
                .addString(ImportQuestionsJobConfig.INPUT_FILE, csvFile.toAbsolutePath().toString())
                // Spring Batch refuse de relancer une exécution terminée avec les MÊMES paramètres :
                // l'horodatage fait de chaque import une nouvelle instance du job.
                .addLong("run.at", System.currentTimeMillis())
                // Non identifiant (false) : c'est une consigne de ménage, pas une caractéristique de l'import.
                .addString(TempFileCleanupListener.TEMPORARY_INPUT, "true", false)
                .toJobParameters();
        JobExecution execution;
        try {
            execution = jobOperator.start(importQuestionsJob, parameters);
        } catch (Exception e) {
            deleteQuietly(csvFile);
            throw new IllegalStateException("could not start the import job", e);
        }
        if (!execution.isRunning()) {
            // Refusé avant même de démarrer (file d'attente pleine) : le listener de fin ne passera jamais.
            deleteQuietly(csvFile);
        }
        return toReport(execution);
    }

    @Override
    public ImportReport findImport(long importId) {
        return toReport(requireImportExecution(importId));
    }

    @Override
    public List<ImportRejection> findRejections(long importId) {
        requireImportExecution(importId);
        return importQuestions.findRejections(importId);
    }

    /** Relit l'exécution en base ; elle doit exister et être bien un import (pas un autre job). */
    private JobExecution requireImportExecution(long executionId) {
        JobExecution execution;
        try {
            execution = jobRepository.getJobExecution(executionId);
        } catch (EmptyResultDataAccessException e) {
            // Spring Batch 6.0 lève cette exception pour un id inconnu au lieu de renvoyer null.
            execution = null;
        }
        if (execution == null || !ImportQuestionsJobConfig.JOB_NAME.equals(execution.getJobInstance().getJobName())) {
            throw new UnknownImportException(executionId);
        }
        return execution;
    }

    private static ImportReport toReport(JobExecution execution) {
        long read = 0;
        long written = 0;
        long rejected = 0;
        for (StepExecution step : execution.getStepExecutions()) {
            read += step.getReadCount() + step.getReadSkipCount(); // illisibles comprises : ce sont des lignes du fichier
            written += step.getWriteCount();
            rejected += step.getSkipCount();
        }
        return new ImportReport(execution.getId(), execution.getStatus().name(),
                execution.getStartTime(), execution.getEndTime(), read, written, rejected);
    }

    /** Ménage « au mieux » : ne doit jamais masquer l'erreur ou le résultat qui l'a déclenché. */
    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // fichier temporaire oublié : sans conséquence sur l'import
        }
    }
}
