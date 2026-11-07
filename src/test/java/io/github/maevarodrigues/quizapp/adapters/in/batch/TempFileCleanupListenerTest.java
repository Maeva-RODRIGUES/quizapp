package io.github.maevarodrigues.quizapp.adapters.in.batch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class TempFileCleanupListenerTest {

    private final TempFileCleanupListener listener = new TempFileCleanupListener();

    private static JobExecution executionFor(Path file, Boolean temporary) {
        JobParametersBuilder builder = new JobParametersBuilder()
                .addString(ImportQuestionsJobConfig.INPUT_FILE, file.toString());
        if (temporary != null) {
            builder.addString(TempFileCleanupListener.TEMPORARY_INPUT, temporary.toString(), false);
        }
        JobParameters parameters = builder.toJobParameters();
        return new JobExecution(1L, new JobInstance(1L, ImportQuestionsJobConfig.JOB_NAME), parameters);
    }

    @Test
    void deletesTheFileWhenItIsMarkedTemporary(@TempDir Path dir) throws Exception {
        Path file = Files.writeString(dir.resolve("upload.csv"), "x");

        listener.afterJob(executionFor(file, true));

        assertThat(file).doesNotExist();
    }

    @Test
    void neverDeletesAFileThatIsNotOurs(@TempDir Path dir) throws Exception {
        Path file = Files.writeString(dir.resolve("source.csv"), "x");

        listener.afterJob(executionFor(file, null));
        listener.afterJob(executionFor(file, false));

        assertThat(file).exists();
    }

    @Test
    void anAlreadyDeletedFile_isNotAnError(@TempDir Path dir) {
        Path absent = dir.resolve("absent.csv");

        assertThatNoException().isThrownBy(() -> listener.afterJob(executionFor(absent, true)));
    }
}
