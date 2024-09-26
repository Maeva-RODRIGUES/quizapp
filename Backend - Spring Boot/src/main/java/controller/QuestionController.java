// Ce contrôleur REST fournit une API pour récupérer une liste de questions de quiz sous format JSON - QuestionController.java

package com.example.quizapp.controller;

import com.example.quizapp.model.Question;
import com.example.quizapp.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController // Annotation indiquant que cette classe est un contrôleur Spring REST. Elle gère les requêtes HTTP et renvoie des réponses au format JSON.
@RequestMapping("/api/questions") // Spécifie que les requêtes envoyées à "/api/questions" seront traitées par ce contrôleur.
public class QuestionController {

    @Autowired // Injection de dépendance de Spring. Elle permet d'injecter automatiquement une instance de QuestionService sans avoir besoin de l'initialiser manuellement.
    private QuestionService questionService;

    @GetMapping // Cette annotation signifie que cette méthode est liée à une requête HTTP GET. Elle est appelée lorsque l'utilisateur envoie une requête GET à "/api/questions".
    public List<Question> getQuestions() {
        // Appelle la méthode getQuestions() du service pour obtenir la liste de toutes les questions.
        // La liste de questions est renvoyée en tant que réponse JSON au client.
        return questionService.getQuestions();
    }
}
