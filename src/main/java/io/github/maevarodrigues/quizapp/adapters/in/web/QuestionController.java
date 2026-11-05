package io.github.maevarodrigues.quizapp.adapters.in.web;

import io.github.maevarodrigues.quizapp.adapters.in.web.dto.QuestionView;
import io.github.maevarodrigues.quizapp.domain.model.Question;
import io.github.maevarodrigues.quizapp.domain.port.in.GetQuizQuestionsUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptateur REST des questions à jouer.
 */
@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final GetQuizQuestionsUseCase getQuizQuestions;

    public QuestionController(GetQuizQuestionsUseCase getQuizQuestions) {
        this.getQuizQuestions = getQuizQuestions;
    }

    /**
     * {@code GET /api/questions} : toutes les questions ;
     * {@code GET /api/questions?category=phobies} : celles d'une catégorie.
     */
    @GetMapping
    public List<QuestionView> getQuestions(@RequestParam(required = false) String category) {
        List<Question> questions = category == null
                ? getQuizQuestions.getAllQuestions()
                : getQuizQuestions.getQuestionsByCategory(category);
        return questions.stream().map(QuestionView::from).toList();
    }
}
