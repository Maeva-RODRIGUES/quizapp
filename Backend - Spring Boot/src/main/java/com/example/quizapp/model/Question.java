// Modèle de données pour une question - Question.java

package com.example.quizapp.model;

// Importation de la classe List du package java.util pour manipuler des collections de type liste
import java.util.List;

// Déclaration de la classe "Question", qui représente un modèle pour une question dans une application de quiz
public class Question {

    // Déclaration des attributs privés de la classe
    // "id" est de type Long et représente l'identifiant unique de chaque question
    private Long id;

    // "questionText" est de type String et stocke le texte de la question
    private String questionText;

    // "options" est une liste de chaînes de caractères, qui contient les différentes options de réponse proposées pour la question
    private List<String> options;

    // Constructeur sans argument (par défaut)
    // Il permet de créer un objet Question vide sans initialiser les valeurs directement
    public Question() {}

    // Constructeur avec trois paramètres : id, questionText, et options
    // Il permet d'initialiser un objet Question avec des valeurs fournies pour chaque attribut
    public Question(Long id, String questionText, List<String> options) {
        this.id = id; // initialisation de l'attribut id avec la valeur passée en paramètre
        this.questionText = questionText; // initialisation de l'attribut questionText
        this.options = options; // initialisation de l'attribut options
    }

    // Getter pour l'attribut "id"
    // Permet de récupérer la valeur de l'id d'une question
    public Long getId() {
        return id;
    }

    // Setter pour l'attribut "id"
    // Permet de modifier la valeur de l'id d'une question
    public void setId(Long id) {
        this.id = id;
    }

    // Getter pour l'attribut "questionText"
    // Permet de récupérer le texte de la question
    public String getQuestionText() {
        return questionText;
    }

    // Setter pour l'attribut "questionText"
    // Permet de modifier le texte de la question
    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    // Getter pour l'attribut "options"
    // Permet de récupérer la liste des options de réponses pour la question
    public List<String> getOptions() {
        return options;
    }

    // Setter pour l'attribut "options"
    // Permet de modifier la liste des options de réponses pour la question
    public void setOptions(List<String> options) {
        this.options = options;
    }
}


