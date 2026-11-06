package io.github.maevarodrigues.quizapp.adapters.in.batch;

import io.github.maevarodrigues.quizapp.domain.model.ImportRejection;
import io.github.maevarodrigues.quizapp.domain.port.in.ImportQuestionsUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class RejectionSkipListenerTest {

    private static final long IMPORT_ID = 42L;

    @Mock
    private ImportQuestionsUseCase importQuestions;

    private RejectionSkipListener listener() {
        return new RejectionSkipListener(importQuestions, IMPORT_ID);
    }

    @Test
    void unreadableLine_isRecordedAsMalformed_withItsNumberAndContent() {
        listener().onSkipInRead(new FlatFileParseException("parse error", "phobies;Q ?;A;B;1", 4));

        then(importQuestions).should().recordRejections(
                List.of(new ImportRejection(IMPORT_ID, 4, "phobies;Q ?;A;B;1", "MALFORMED_LINE")));
    }

    @Test
    void invalidLine_isRecordedOncePerReason() {
        QuestionCsvLine line = new QuestionCsvLine(9, "inconnue; ;A;a;;;9", "inconnue", " ", List.of("A", "a"), "9");
        var exception = new InvalidQuestionException(new ValidatedLine.Rejected(line,
                List.of(RejectionReason.UNKNOWN_CATEGORY, RejectionReason.EMPTY_TEXT)));

        listener().onSkipInProcess(line, exception);

        then(importQuestions).should().recordRejections(List.of(
                new ImportRejection(IMPORT_ID, 9, "inconnue; ;A;a;;;9", "UNKNOWN_CATEGORY"),
                new ImportRejection(IMPORT_ID, 9, "inconnue; ;A;a;;;9", "EMPTY_TEXT")));
    }

    @Test
    void unexpectedSkippedFailure_isNotSilentlyIgnored() {
        assertThatIllegalStateException().isThrownBy(() -> listener().onSkipInRead(new IllegalArgumentException("?")));
    }
}
