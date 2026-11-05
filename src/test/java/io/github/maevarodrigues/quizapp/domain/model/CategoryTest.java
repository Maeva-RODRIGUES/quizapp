package io.github.maevarodrigues.quizapp.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CategoryTest {

    @Test
    void createsValidCategory() {
        Category category = new Category(1L, "phobies", "Phobies étranges");

        assertThat(category.id()).isEqualTo(1L);
        assertThat(category.code()).isEqualTo("phobies");
        assertThat(category.label()).isEqualTo("Phobies étranges");
    }

    @Test
    void allowsNullIdForCategoryNotYetSaved() {
        assertThat(new Category(null, "phobies", "Phobies étranges").id()).isNull();
    }

    @Test
    void acceptsCodeWithDigitsAndInnerHyphens() {
        assertThat(new Category(null, "culture-g-2", "Culture G").code()).isEqualTo("culture-g-2");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Phobies", "pas valide", "-phobies", "phobies-", "pho--bies", "phobies_etranges", "phobiés"})
    void rejectsInvalidCode(String code) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Category(null, code, "Phobies étranges"))
                .withMessageContaining("code");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void rejectsBlankLabel(String label) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Category(null, "phobies", label))
                .withMessageContaining("label");
    }

    @Test
    void stripsLabel() {
        assertThat(new Category(null, "phobies", "  Phobies étranges ").label()).isEqualTo("Phobies étranges");
    }
}
