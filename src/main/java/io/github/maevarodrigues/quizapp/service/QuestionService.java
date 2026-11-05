package io.github.maevarodrigues.quizapp.service;

import io.github.maevarodrigues.quizapp.model.Question;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fournit les questions du quiz.
 * <p>
 * Version provisoire : les questions sont définies en mémoire. Elles seront lues
 * depuis PostgreSQL à partir de la phase 4.
 */
@Service
public class QuestionService {

    /**
     * Renvoie toutes les questions, toutes catégories confondues.
     */
    public List<Question> getAllQuestions() {
        List<Question> questions = new ArrayList<>();

        // Catégorie "Superstitions étranges"
        questions.add(new Question(1L, "Pourquoi dit-on que passer sous une échelle porte malheur ?",
                List.of("L'échelle représente la maison des esprits",
                        "L'échelle appuyée contre un mur forme un triangle, symbole de la Trinité",
                        "Passer dessous attire la foudre"),
                "Superstitions étranges"));

        questions.add(new Question(2L, "Selon la superstition, que provoque un miroir cassé ?",
                List.of("Un an de malchance",
                        "Trois mois de mauvaise chance",
                        "Sept ans de malheur"),
                "Superstitions étranges"));

        questions.add(new Question(3L, "Pourquoi dit-on qu'il est malchanceux de croiser un chat noir ?",
                List.of("Il annonce du mauvais temps",
                        "Il était associé aux sorcières",
                        "Il annonce une mauvaise récolte"),
                "Superstitions étranges"));

        questions.add(new Question(4L, "Selon la tradition populaire américaine, quelle patte de lapin porte chance ?",
                List.of("La patte arrière gauche",
                        "La patte avant droite",
                        "N'importe laquelle"),
                "Superstitions étranges"));

        questions.add(new Question(5L, "Selon la superstition, que ne faut-il pas faire avec un parapluie dans une maison ?",
                List.of("Le laisser sécher fermé",
                        "L'ouvrir",
                        "Le ranger dans l'entrée"),
                "Superstitions étranges"));

        // Catégorie "Phobies étranges"
        questions.add(new Question(6L, "Quel est le nom de la peur des araignées ?",
                List.of("Entomophobie",
                        "Myrmécophobie",
                        "Arachnophobie"),
                "Phobies étranges"));

        questions.add(new Question(7L, "Comment appelle-t-on la peur irrationnelle des nombres ?",
                List.of("Arithmophobie",
                        "Chronophobie",
                        "Logophobie"),
                "Phobies étranges"));

        questions.add(new Question(8L, "Comment appelle-t-on la peur ou le dégoût des amas de petits trous ?",
                List.of("Claustrophobie",
                        "Trypophobie",
                        "Cénophobie"),
                "Phobies étranges"));

        questions.add(new Question(9L, "Quelle phobie désigne la peur de la couleur jaune ?",
                List.of("Chromatophobie",
                        "Mélanophobie",
                        "Xanthophobie"),
                "Phobies étranges"));

        questions.add(new Question(10L, "Comment appelle-t-on la peur des grands espaces ouverts et de la foule ?",
                List.of("Agoraphobie",
                        "Claustrophobie",
                        "Topophobie"),
                "Phobies étranges"));

        return questions;
    }

    /**
     * Renvoie les questions d'une catégorie (comparaison insensible à la casse).
     */
    public List<Question> getQuestionsByCategory(String category) {
        return getAllQuestions().stream()
                .filter(question -> question.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }
}
