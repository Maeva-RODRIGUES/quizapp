package io.github.maevarodrigues.quizapp.adapters.in.batch;

import io.github.maevarodrigues.quizapp.adapters.in.batch.ValidatedLine.Accepted;
import io.github.maevarodrigues.quizapp.adapters.in.batch.ValidatedLine.Rejected;
import io.github.maevarodrigues.quizapp.domain.model.Question;
import io.github.maevarodrigues.quizapp.domain.model.QuestionKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static io.github.maevarodrigues.quizapp.adapters.in.batch.RejectionReason.*;
import static org.assertj.core.api.Assertions.assertThat;

class QuestionImportValidatorTest {

    private static final Set<String> CATEGORIES = Set.of("phobies", "superstitions");
    private static final QuestionKey ARAIGNEES_EN_BASE =
            QuestionKey.of("phobies", "Quel est le nom de la peur des araignées ?");

    private QuestionImportValidator validator;
    private int nextLineNumber;

    @BeforeEach
    void newImport() {
        validator = new QuestionImportValidator(CATEGORIES::contains, ARAIGNEES_EN_BASE::equals);
        nextLineNumber = 2;
    }

    /** Construit une ligne : catégorie, énoncé, 4 colonnes de réponses, numéro de la bonne réponse. */
    private QuestionCsvLine line(String category, String text, String o1, String o2, String o3, String o4, String correct) {
        String raw = String.join(";", category, text, o1, o2, o3, o4, correct);
        return new QuestionCsvLine(nextLineNumber++, raw, category, text, Arrays.asList(o1, o2, o3, o4), correct);
    }

    private QuestionCsvLine validLine() {
        return line("phobies", "Quelle est la peur des clowns ?", "Coulrophobie", "Clinophobie", "Cynophobie", "", "1");
    }

    private List<RejectionReason> reasonsFor(QuestionCsvLine line) {
        ValidatedLine result = validator.validate(line);
        assertThat(result).isInstanceOf(Rejected.class);
        return ((Rejected) result).reasons();
    }

    @Nested
    class AcceptedLines {

        @Test
        void validLine_becomesAQuestion_withZeroBasedCorrectIndex() {
            ValidatedLine result = validator.validate(validLine());

            assertThat(result).isInstanceOf(Accepted.class);
            Question question = ((Accepted) result).question();
            assertThat(question.id()).isNull();
            assertThat(question.categoryCode()).isEqualTo("phobies");
            assertThat(question.options()).containsExactly("Coulrophobie", "Clinophobie", "Cynophobie");
            assertThat(question.correctOptionIndex()).isZero();
            assertThat(question.correctOption()).isEqualTo("Coulrophobie");
        }

        @Test
        void keepsLineNumberAndRawLineForTraceability() {
            QuestionCsvLine line = validLine();

            assertThat(validator.validate(line).line()).isSameAs(line);
        }

        @Test
        void stripsSurroundingSpaces() {
            ValidatedLine result = validator.validate(
                    line(" phobies ", "  Quelle est la peur des clowns ?  ", " Coulrophobie ", "Clinophobie", "", "", " 2 "));

            Question question = ((Accepted) result).question();
            assertThat(question.text()).isEqualTo("Quelle est la peur des clowns ?");
            assertThat(question.options()).containsExactly("Coulrophobie", "Clinophobie");
            assertThat(question.correctOptionIndex()).isEqualTo(1);
        }

        @Test
        void twoOptionsAreEnough_trailingEmptyColumnsAreIgnored() {
            ValidatedLine result = validator.validate(line("phobies", "Q ?", "Oui", "Non", "", "", "2"));

            assertThat(((Accepted) result).question().options()).containsExactly("Oui", "Non");
        }
    }

    @Nested
    class OneTestPerRule {

        @ParameterizedTest
        @ValueSource(strings = {"inconnue", "", "PHOBIES"})
        void unknownCategory(String category) {
            assertThat(reasonsFor(line(category, "Q ?", "A", "B", "", "", "1"))).containsExactly(UNKNOWN_CATEGORY);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void emptyText(String text) {
            assertThat(reasonsFor(line("phobies", text, "A", "B", "", "", "1"))).containsExactly(EMPTY_TEXT);
        }

        @Test
        void textTooLong() {
            String text = "x".repeat(Question.TEXT_MAX_LENGTH + 1);

            assertThat(reasonsFor(line("phobies", text, "A", "B", "", "", "1"))).containsExactly(TEXT_TOO_LONG);
        }

        @Test
        void textAtMaxLength_isAccepted() {
            String text = "x".repeat(Question.TEXT_MAX_LENGTH);

            assertThat(validator.validate(line("phobies", text, "A", "B", "", "", "1"))).isInstanceOf(Accepted.class);
        }

        @Test
        void tooFewOptions() {
            assertThat(reasonsFor(line("phobies", "Q ?", "A", "", "", "", "1"))).containsExactly(TOO_FEW_OPTIONS);
        }

        @Test
        void gapBetweenOptions() {
            assertThat(reasonsFor(line("phobies", "Q ?", "A", "", "C", "", "1"))).containsExactly(OPTIONS_NOT_CONTIGUOUS);
        }

        @Test
        void optionTooLong() {
            String option = "x".repeat(Question.OPTION_MAX_LENGTH + 1);

            assertThat(reasonsFor(line("phobies", "Q ?", "A", option, "", "", "1"))).containsExactly(OPTION_TOO_LONG);
        }

        @Test
        void duplicateOptions_ignoringCase() {
            assertThat(reasonsFor(line("phobies", "Q ?", "Arachnophobie", "ARACHNOPHOBIE", "", "", "1")))
                    .containsExactly(DUPLICATE_OPTIONS);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "un", "1.5", "a1"})
        void correctOptionNotANumber(String correct) {
            assertThat(reasonsFor(line("phobies", "Q ?", "A", "B", "", "", correct))).containsExactly(CORRECT_OPTION_NOT_A_NUMBER);
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "3", "-1"})
        void correctOptionOutOfRange(String correct) {
            // 2 réponses : seuls 1 et 2 sont valides (0 serait un index « développeur », pas un numéro humain)
            assertThat(reasonsFor(line("phobies", "Q ?", "A", "B", "", "", correct))).containsExactly(CORRECT_OPTION_OUT_OF_RANGE);
        }

        @Test
        void alreadyExistsInDatabase_comparedOnNormalizedText() {
            assertThat(reasonsFor(line("phobies", "  quel est LE nom de la   peur des araignées ? ", "A", "B", "", "", "1")))
                    .containsExactly(ALREADY_EXISTS);
        }

        @Test
        void sameTextInAnotherCategory_isNotADuplicate() {
            assertThat(validator.validate(line("superstitions", "Quel est le nom de la peur des araignées ?", "A", "B", "", "", "1")))
                    .isInstanceOf(Accepted.class);
        }
    }

    @Nested
    class DuplicatesInFile {

        @Test
        void secondOccurrence_isRejected() {
            validator.validate(validLine());

            assertThat(reasonsFor(line("phobies", "QUELLE est la peur  des clowns ?", "X", "Y", "", "", "2")))
                    .containsExactly(DUPLICATE_IN_FILE);
        }

        @Test
        void rejectedFirstOccurrence_doesNotBlockACorrectCopyLater() {
            validator.validate(line("phobies", "Quelle est la peur des clowns ?", "Coulrophobie", "", "", "", "1"));

            assertThat(validator.validate(validLine())).isInstanceOf(Accepted.class);
        }

        @Test
        void eachImportStartsWithAnEmptyMemory() {
            validator.validate(validLine());
            QuestionImportValidator nextImport = new QuestionImportValidator(CATEGORIES::contains, key -> false);

            assertThat(nextImport.validate(validLine())).isInstanceOf(Accepted.class);
        }
    }

    @Test
    void reportsEveryProblemOfTheLineAtOnce() {
        assertThat(reasonsFor(line("inconnue", " ", "A", "a", "", "", "9")))
                .containsExactlyInAnyOrder(UNKNOWN_CATEGORY, EMPTY_TEXT, DUPLICATE_OPTIONS, CORRECT_OPTION_OUT_OF_RANGE);
    }

    @Test
    void reasonCodesMatchTheDatabaseFormat() {
        assertThat(RejectionReason.values())
                .allSatisfy(reason -> assertThat(reason.name()).matches("^[A-Z]+(_[A-Z]+)*$")
                        .hasSizeLessThanOrEqualTo(50));
    }
}
