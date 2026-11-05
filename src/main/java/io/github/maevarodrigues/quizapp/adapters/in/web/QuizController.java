package io.github.maevarodrigues.quizapp.adapters.in.web;

import io.github.maevarodrigues.quizapp.adapters.in.web.dto.QuizResultResponse;
import io.github.maevarodrigues.quizapp.adapters.in.web.dto.SubmitQuizRequest;
import io.github.maevarodrigues.quizapp.domain.model.QuizSubmission;
import io.github.maevarodrigues.quizapp.domain.port.in.SubmitQuizUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur REST de la soumission d'un quiz : traduit le JSON en objets du domaine et inversement.
 * La correction elle-même est faite par le domaine.
 */
@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    private final SubmitQuizUseCase submitQuiz;

    public QuizController(SubmitQuizUseCase submitQuiz) {
        this.submitQuiz = submitQuiz;
    }

    /** {@code POST /api/quiz/submit} : corrige les réponses données au quiz d'une catégorie. */
    @PostMapping("/submit")
    public QuizResultResponse submit(@Valid @RequestBody SubmitQuizRequest request) {
        var result = submitQuiz.submitQuiz(request.category(), new QuizSubmission(request.answers()));
        return QuizResultResponse.from(result);
    }
}
