// Logique pour générer et renvoyer les questions - QuestionService.java

package com.example.quizapp.service;

import com.example.quizapp.model.Question;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    // Méthode pour obtenir toutes les questions
    public List<Question> getAllQuestions() {
        List<Question> questions = new ArrayList<>();

        // Questions pour la catégorie "Superstitions étranges"
        questions.add(new Question(1L, "Quelle superstition populaire suggère qu'il est malchanceux de passer sous une échelle ?",
                List.of("a) Parce que cela symbolise un triangle sacré",
                        "b) Parce que l'échelle représente la maison des esprits",
                        "c) Parce que cela brise un miroir"),
                "Superstitions étranges"));

        questions.add(new Question(2L, "Quel est le superstitieux associé au fait de casser un miroir ?",
                List.of("a) Sept ans de malheur",
                        "b) Un an de malchance",
                        "c) Trois mois de mauvaise chance"),
                "Superstitions étranges"));

        questions.add(new Question(3L, "Pourquoi dit-on qu'il est malchanceux de croiser un chat noir ?",
                List.of("a) Parce qu'ils sont associés aux sorcières",
                        "b) Parce qu'ils portent des esprits malins",
                        "c) Parce qu'ils annoncent un mauvais temps"),
                "Superstitions étranges"));

        questions.add(new Question(4L, "Quelle superstition affirme que trouver une patte de lapin porte chance ?",
                List.of("a) L'origine est dans les croyances médiévales",
                        "b) C'est une coutume des anciens Égyptiens",
                        "c) C'est lié aux cérémonies druidiques"),
                "Superstitions étranges"));

        questions.add(new Question(5L, "Quelle est l'origine de la superstition selon laquelle on ne doit pas ouvrir un parapluie à l'intérieur ?",
                List.of("a) Cela attire la pluie",
                        "b) Cela est lié aux esprits maléfiques",
                        "c) Cela porte malheur selon les traditions anciennes"),
                "Superstitions étranges"));

        // Questions pour la catégorie "Phobies étranges"
        questions.add(new Question(6L, "Quelle est la phobie des araignées ?",
                List.of("a) Arachnophobie",
                        "b) Entomophobie",
                        "c) Myrmécophobie"),
                "Phobies étranges"));

        questions.add(new Question(7L, "Quel est le terme pour la peur irrationnelle des chiffres ?",
                List.of("a) Numerophobie",
                        "b) Arithmophobie",
                        "c) Digitophobie"),
                "Phobies étranges"));

        questions.add(new Question(8L, "Comment appelle-t-on la peur des trous ou des motifs répétitifs ?",
                List.of("a) Trépanophobie",
                        "b) Trypophobie",
                        "c) Clastophobie"),
                "Phobies étranges"));

        questions.add(new Question(9L, "Quelle phobie est associée à la peur de la couleur jaune ?",
                List.of("a) Xanthophobie",
                        "b) Chromatophobie",
                        "c) Melanophobie"),
                "Phobies étranges"));

        questions.add(new Question(10L, "Quel est le nom de la phobie des ouvertures ou des espaces ouverts ?",
                List.of("a) Claustrophobie",
                        "b) Agoraphobie",
                        "c) Topophobie"),
                "Phobies étranges"));

        return questions;
    }

    // Méthode pour filtrer les questions par catégorie
    public List<Question> getQuestionsByCategory(String category) {
        return getAllQuestions().stream()
                .filter(question -> question.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }
}
