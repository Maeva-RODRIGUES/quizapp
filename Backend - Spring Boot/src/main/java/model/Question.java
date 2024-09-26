// Modèle de données pour une question - Question.java

package com.example.quizapp.model;

import java.util.List;

public class Question {
    private Long id;
    private String questionText;
    private List<String> options;

    // Constructors, getters, and setter
    public Question() {}

    public Question(Long id, String questionText, List<String> options) {
        this.id = id;
        this.questionText = questionText;
        this.options = options;
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
}


