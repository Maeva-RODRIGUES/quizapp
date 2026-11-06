package io.github.maevarodrigues.quizapp.adapters.in.batch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionCsvLineMapperTest {

    private final QuestionCsvLineMapper mapper = new QuestionCsvLineMapper();

    @Test
    void mapsTheSevenColumns_andKeepsLineNumberAndRawLine() {
        String raw = "phobies; Quelle est la peur des clowns ? ;Coulrophobie;Clinophobie;;;1";

        QuestionCsvLine line = mapper.mapLine(raw, 2);

        assertThat(line.lineNumber()).isEqualTo(2);
        assertThat(line.rawLine()).isEqualTo(raw);
        assertThat(line.category()).isEqualTo("phobies");
        assertThat(line.text()).as("pas de nettoyage ici : c'est le rôle du validateur")
                .isEqualTo(" Quelle est la peur des clowns ? ");
        assertThat(line.options()).containsExactly("Coulrophobie", "Clinophobie", "", "");
        assertThat(line.correctOption()).isEqualTo("1");
    }

    @Test
    void quotedFieldMayContainTheSeparator() {
        QuestionCsvLine line = mapper.mapLine("phobies;\"Peur des nombres ; laquelle ?\";A;B;;;1", 2);

        assertThat(line.text()).isEqualTo("Peur des nombres ; laquelle ?");
    }

    @ParameterizedTest
    @ValueSource(strings = {"phobies;Q ?;A;B;1", "phobies;Q ?;A;B;;;1;en trop", ""})
    void wrongNumberOfColumns_isRejected(String raw) {
        assertThatThrownBy(() -> mapper.mapLine(raw, 2)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void acceptsTheExpectedHeader_withOrWithoutByteOrderMark() {
        assertThatCode(() -> QuestionCsvLineMapper.checkHeader(QuestionCsvLineMapper.EXPECTED_HEADER)).doesNotThrowAnyException();
        assertThatCode(() -> QuestionCsvLineMapper.checkHeader("﻿" + QuestionCsvLineMapper.EXPECTED_HEADER))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsAnUnexpectedHeader() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> QuestionCsvLineMapper.checkHeader("categorie;texte;r1;r2;r3;r4;bonne"))
                .withMessageContaining("unexpected CSV header");
    }
}
