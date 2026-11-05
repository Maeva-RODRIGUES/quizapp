package io.github.maevarodrigues.quizapp.model;

import java.util.List;

/**
 * Une question du quiz : son texte, les réponses proposées et sa catégorie.
 * <p>
 * Le constructeur sans argument et les setters sont utilisés par Jackson pour la (dé)sérialisation JSON.
 */
public class Question {

    private Long id;
    private String questionText;
    private List<String> options;
    private String category;

    public Question() {
    }

    public Question(Long id, String questionText, List<String> options, String category) {
        this.id = id;
        this.questionText = questionText;
        this.options = options;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
