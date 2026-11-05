package io.github.maevarodrigues.quizapp.application;

import io.github.maevarodrigues.quizapp.domain.exception.UnknownCategoryException;
import io.github.maevarodrigues.quizapp.domain.model.Question;
import io.github.maevarodrigues.quizapp.domain.model.QuizResult;
import io.github.maevarodrigues.quizapp.domain.model.QuizSubmission;
import io.github.maevarodrigues.quizapp.domain.port.in.GetQuizQuestionsUseCase;
import io.github.maevarodrigues.quizapp.domain.port.in.SubmitQuizUseCase;
import io.github.maevarodrigues.quizapp.domain.port.out.CategoryRepository;
import io.github.maevarodrigues.quizapp.domain.port.out.QuestionRepository;

import java.util.List;

/**
 * Implémente les cas d'usage du quiz : il orchestre (charger, vérifier, déléguer au domaine)
 * sans contenir de règle métier. La correction elle-même est dans {@link QuizSubmission#grade}.
 */
public class QuizService implements GetQuizQuestionsUseCase, SubmitQuizUseCase {

    private final CategoryRepository categoryRepository;
    private final QuestionRepository questionRepository;

    public QuizService(CategoryRepository categoryRepository, QuestionRepository questionRepository) {
        this.categoryRepository = categoryRepository;
        this.questionRepository = questionRepository;
    }

    @Override
    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }

    @Override
    public List<Question> getQuestionsByCategory(String categoryCode) {
        requireExistingCategory(categoryCode);
        return questionRepository.findByCategoryCode(categoryCode);
    }

    @Override
    public QuizResult submitQuiz(String categoryCode, QuizSubmission submission) {
        List<Question> quizQuestions = getQuestionsByCategory(categoryCode);
        return submission.grade(quizQuestions);
    }

    private void requireExistingCategory(String categoryCode) {
        if (!categoryRepository.existsByCode(categoryCode)) {
            throw new UnknownCategoryException(categoryCode);
        }
    }
}
