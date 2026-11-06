package io.github.maevarodrigues.quizapp.application;

import io.github.maevarodrigues.quizapp.domain.model.ImportRejection;
import io.github.maevarodrigues.quizapp.domain.model.Question;
import io.github.maevarodrigues.quizapp.domain.model.QuestionKey;
import io.github.maevarodrigues.quizapp.domain.port.in.ImportQuestionsUseCase;
import io.github.maevarodrigues.quizapp.domain.port.out.CategoryRepository;
import io.github.maevarodrigues.quizapp.domain.port.out.ImportRejectionRepository;
import io.github.maevarodrigues.quizapp.domain.port.out.QuestionRepository;

import java.util.List;

/**
 * Implémente l'import de questions en déléguant aux ports de sortie. Pas de règle métier ici :
 * les règles de qualité sont appliquées par l'adaptateur d'import, les invariants par le domaine.
 */
public class QuestionImportService implements ImportQuestionsUseCase {

    private final CategoryRepository categoryRepository;
    private final QuestionRepository questionRepository;
    private final ImportRejectionRepository rejectionRepository;

    public QuestionImportService(CategoryRepository categoryRepository, QuestionRepository questionRepository,
                                 ImportRejectionRepository rejectionRepository) {
        this.categoryRepository = categoryRepository;
        this.questionRepository = questionRepository;
        this.rejectionRepository = rejectionRepository;
    }

    @Override
    public boolean categoryExists(String categoryCode) {
        return categoryRepository.existsByCode(categoryCode);
    }

    @Override
    public boolean questionExists(QuestionKey key) {
        return questionRepository.existsByKey(key);
    }

    @Override
    public List<Question> importQuestions(List<Question> questions) {
        if (questions.isEmpty()) {
            return List.of();
        }
        return questionRepository.saveAll(questions);
    }

    @Override
    public void recordRejections(List<ImportRejection> rejections) {
        if (!rejections.isEmpty()) {
            rejectionRepository.saveAll(rejections);
        }
    }
}
