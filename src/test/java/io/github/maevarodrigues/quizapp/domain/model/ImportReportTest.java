package io.github.maevarodrigues.quizapp.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ImportReportTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 6, 10, 0);

    @Test
    void justStarted_isNeitherFinishedNorCompleted() {
        ImportReport report = new ImportReport(1L, "STARTING", null, null, 0, 0, 0);

        assertThat(report.finished()).isFalse();
        assertThat(report.completed()).isFalse();
    }

    @Test
    void completed_isFinished() {
        ImportReport report = new ImportReport(1L, "COMPLETED", START, START.plusSeconds(2), 12, 5, 7);

        assertThat(report.finished()).isTrue();
        assertThat(report.completed()).isTrue();
    }

    @Test
    void failed_isFinishedButNotCompleted() {
        ImportReport report = new ImportReport(1L, "FAILED", START, START.plusSeconds(1), 0, 0, 0);

        assertThat(report.finished()).isTrue();
        assertThat(report.completed()).isFalse();
    }

    @Test
    void rejectsNegativeCounters() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ImportReport(1L, "COMPLETED", null, null, -1, 0, 0));
    }

    @Test
    void writtenPlusRejectedCannotExceedRead() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ImportReport(1L, "COMPLETED", null, null, 10, 6, 5));
    }

    @Test
    void endCannotBeBeforeStart() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ImportReport(1L, "COMPLETED", START, START.minusSeconds(1), 0, 0, 0));
    }
}
