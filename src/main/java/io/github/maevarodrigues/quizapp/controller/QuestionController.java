package io.github.maevarodrigues.quizapp.controller;

import io.github.maevarodrigues.quizapp.model.Question;
import io.github.maevarodrigues.quizapp.service.QuestionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API REST des questions du quiz. Les réponses sont sérialisées en JSON.
 */
@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;

    // Injection par constructeur : la dépendance est obligatoire et immuable (final).
    // Avec un seul constructeur, Spring l'utilise automatiquement, sans @Autowired.
    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    /**
     * {@code GET /api/questions} : renvoie toutes les questions.
     */
    @GetMapping
    public List<Question> getQuestions() {
        return questionService.getAllQuestions();
    }
}
