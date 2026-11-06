package io.github.maevarodrigues.quizapp.adapters.in.batch;

import io.github.maevarodrigues.quizapp.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

/**
 * Le job de bout en bout, sur un vrai PostgreSQL : fichier CSV → règles de qualité → tables {@code question}
 * et {@code import_rejection}, avec les compteurs de Spring Batch.
 * <p>
 * Pas de {@code @Transactional} ici : le job gère ses propres transactions (une par chunk).
 * On nettoie donc à la main après chaque test.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ImportQuestionsJobIT {

    private static final String MIXED_FILE = "import/questions-mixed.csv";

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private Job importQuestionsJob;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM import_rejection");
        jdbc.update("DELETE FROM question"); // les réponses suivent (ON DELETE CASCADE)
    }

    private JobExecution runImport(Path file) throws Exception {
        JobParameters parameters = new JobParametersBuilder()
                .addString(ImportQuestionsJobConfig.INPUT_FILE, file.toString())
                .addLong("run.at", System.nanoTime()) // rend chaque exécution unique (sinon : « déjà faite »)
                .toJobParameters();
        long executionId = jobOperator.start(importQuestionsJob, parameters).getId();
        // Le lancement est asynchrone : on attend la fin du job (sans Thread.sleep), puis on relit son état en base.
        await().atMost(Duration.ofSeconds(30))
                .until(() -> !jobRepository.getJobExecution(executionId).isRunning());
        return jobRepository.getJobExecution(executionId);
    }

    private static Path mixedFile() throws Exception {
        return new ClassPathResource(MIXED_FILE).getFile().toPath();
    }

    private static StepExecution step(JobExecution execution) {
        return execution.getStepExecutions().iterator().next();
    }

    @Test
    void importsValidLines_skipsInvalidOnes_andCountsEverything() throws Exception {
        JobExecution execution = runImport(mixedFile());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        StepExecution step = step(execution);
        assertThat(step.getReadCount()).as("lignes lues correctement").isEqualTo(9);
        assertThat(step.getReadSkipCount()).as("lignes illisibles").isEqualTo(1);
        assertThat(step.getProcessSkipCount()).as("lignes refusées par les règles").isEqualTo(6);
        assertThat(step.getSkipCount()).as("total des lignes rejetées").isEqualTo(7);
        assertThat(step.getWriteCount()).as("questions écrites").isEqualTo(3);

        assertThat(jdbc.queryForList("SELECT text FROM question ORDER BY id", String.class)).containsExactly(
                "Quelle est la peur des clowns ?",
                "Quel chiffre porte malheur au Japon ?",
                "Peur des nombres ; laquelle ?");
    }

    @Test
    void writesOneRejectionPerReason_withLineNumberAndRawLine() throws Exception {
        JobExecution execution = runImport(mixedFile());

        List<Map<String, Object>> rejections = jdbc.queryForList(
                "SELECT line_number, reason, raw_line FROM import_rejection WHERE job_execution_id = ? ORDER BY line_number, reason",
                execution.getId());

        assertThat(rejections).extracting(row -> row.get("line_number"), row -> row.get("reason")).containsExactly(
                tuple(4, "MALFORMED_LINE"),
                tuple(5, "UNKNOWN_CATEGORY"),
                tuple(6, "TOO_FEW_OPTIONS"),
                tuple(7, "CORRECT_OPTION_OUT_OF_RANGE"),
                tuple(8, "DUPLICATE_IN_FILE"),
                tuple(9, "CORRECT_OPTION_OUT_OF_RANGE"),
                tuple(9, "DUPLICATE_OPTIONS"),
                tuple(9, "EMPTY_TEXT"),
                tuple(9, "UNKNOWN_CATEGORY"),
                tuple(11, "OPTIONS_NOT_CONTIGUOUS"));
        assertThat(rejections.getFirst().get("raw_line")).isEqualTo("phobies;Ligne avec trop peu de colonnes ?;A;B;1");
    }

    @Test
    void importingTheSameFileTwice_writesNothingTheSecondTime() throws Exception {
        runImport(mixedFile());

        JobExecution second = runImport(mixedFile());

        assertThat(step(second).getWriteCount()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM question", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForList(
                "SELECT line_number FROM import_rejection WHERE job_execution_id = ? AND reason = 'ALREADY_EXISTS' ORDER BY line_number",
                Integer.class, second.getId()))
                .as("les 3 questions déjà importées, et la ligne 8 qui duplique la ligne 2")
                .containsExactly(2, 3, 8, 10);
    }

    @Test
    void wrongHeader_failsTheJobWithoutWritingAnything(@TempDir Path dir) throws Exception {
        Path file = Files.writeString(dir.resolve("bad-header.csv"), """
                categorie;texte;r1;r2;r3;r4;bonne
                phobies;Quelle est la peur des clowns ?;Coulrophobie;Clinophobie;;;1
                """);

        JobExecution execution = runImport(file);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        // Relue depuis la base : la cause est conservée dans le message de sortie (colonne exit_message).
        assertThat(step(execution).getExitStatus().getExitDescription()).contains("unexpected CSV header");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM question", Integer.class)).isZero();
    }

    @Test
    void headerWithByteOrderMark_isAccepted(@TempDir Path dir) throws Exception {
        Path file = Files.writeString(dir.resolve("bom.csv"), "﻿" + QuestionCsvLineMapper.EXPECTED_HEADER + "\n"
                + "phobies;Quelle est la peur des clowns ?;Coulrophobie;Clinophobie;;;1\n");

        JobExecution execution = runImport(file);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(step(execution).getWriteCount()).isEqualTo(1);
    }
}
