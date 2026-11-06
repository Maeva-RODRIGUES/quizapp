package io.github.maevarodrigues.quizapp.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ImportReportTest {

    @Test
    void completedOnlyForTheCompletedStatus() {
        assertThat(new ImportReport(1L, "COMPLETED", 12, 5, 7).completed()).isTrue();
        assertThat(new ImportReport(1L, "FAILED", 0, 0, 0).completed()).isFalse();
    }

    @Test
    void rejectsNegativeCounters() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ImportReport(1L, "COMPLETED", -1, 0, 0));
    }

    @Test
    void writtenPlusRejectedCannotExceedRead() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ImportReport(1L, "COMPLETED", 10, 6, 5));
    }
}
