package io.github.maevarodrigues.quizapp.application;

import io.github.maevarodrigues.quizapp.domain.model.ImportRejection;
import io.github.maevarodrigues.quizapp.domain.model.Question;
import io.github.maevarodrigues.quizapp.domain.model.QuestionKey;
import io.github.maevarodrigues.quizapp.domain.port.out.CategoryRepository;
import io.github.maevarodrigues.quizapp.domain.port.out.ImportRejectionRepository;
import io.github.maevarodrigues.quizapp.domain.port.out.QuestionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Test unitaire du service d'import : les ports « out » sont simulés par Mockito.
 */
@ExtendWith(MockitoExtension.class)
class QuestionImportServiceTest {

    private static final Question CLOWNS = new Question(null, "phobies", "Quelle est la peur des clowns ?",
            List.of("Coulrophobie", "Clinophobie"), 0);

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private ImportRejectionRepository rejectionRepository;

    @InjectMocks
    private QuestionImportService service;

    @Test
    void categoryExists_delegatesToTheCategoryRepository() {
        given(categoryRepository.existsByCode("phobies")).willReturn(true);

        assertThat(service.categoryExists("phobies")).isTrue();
        assertThat(service.categoryExists("inconnue")).isFalse();
    }

    @Test
    void questionExists_delegatesToTheQuestionRepository() {
        QuestionKey key = QuestionKey.of(CLOWNS);
        given(questionRepository.existsByKey(key)).willReturn(true);

        assertThat(service.questionExists(key)).isTrue();
    }

    @Test
    void importQuestions_savesThemAndReturnsThemWithIds() {
        Question saved = new Question(11L, CLOWNS.categoryCode(), CLOWNS.text(), CLOWNS.options(), 0);
        given(questionRepository.saveAll(List.of(CLOWNS))).willReturn(List.of(saved));

        assertThat(service.importQuestions(List.of(CLOWNS))).containsExactly(saved);
    }

    @Test
    void importQuestions_withNothingToSave_doesNotHitTheDatabase() {
        assertThat(service.importQuestions(List.of())).isEmpty();

        then(questionRepository).should(never()).saveAll(any());
    }

    @Test
    void recordRejections_savesThem() {
        List<ImportRejection> rejections = List.of(new ImportRejection(7L, 3, "ligne", "EMPTY_TEXT"));

        service.recordRejections(rejections);

        then(rejectionRepository).should().saveAll(rejections);
    }

    @Test
    void recordRejections_withNothingToSave_doesNotHitTheDatabase() {
        service.recordRejections(List.of());

        then(rejectionRepository).should(never()).saveAll(anyList());
    }
}
