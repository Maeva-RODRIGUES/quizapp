package io.github.maevarodrigues.quizapp.adapters.out.persistence;

import io.github.maevarodrigues.quizapp.domain.exception.UnknownCategoryException;
import io.github.maevarodrigues.quizapp.domain.model.Question;
import io.github.maevarodrigues.quizapp.domain.port.out.QuestionRepository;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Records;
import org.jooq.Result;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.maevarodrigues.quizapp.adapters.out.persistence.jooq.Tables.CATEGORY;
import static io.github.maevarodrigues.quizapp.adapters.out.persistence.jooq.Tables.QUESTION;
import static io.github.maevarodrigues.quizapp.adapters.out.persistence.jooq.Tables.QUESTION_OPTION;
import static org.jooq.impl.DSL.multiset;
import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.select;

/**
 * Adaptateur de persistance : implémente le port {@link QuestionRepository} avec jOOQ.
 * <p>
 * Chaque question est chargée avec ses réponses en UNE requête SQL grâce à {@code MULTISET}
 * (sous-requête corrélée renvoyée sous forme de collection) : pas de problème « N+1 requêtes ».
 */
@Repository
public class JooqQuestionRepository implements QuestionRepository {

    /** Les réponses de la question courante, triées par position. */
    private static final Field<Result<Record2<Short, String>>> OPTIONS = multiset(
            select(QUESTION_OPTION.OPTION_INDEX, QUESTION_OPTION.LABEL)
                    .from(QUESTION_OPTION)
                    .where(QUESTION_OPTION.QUESTION_ID.eq(QUESTION.ID))
                    .orderBy(QUESTION_OPTION.OPTION_INDEX))
            .as("options");

    private final DSLContext dsl;

    public JooqQuestionRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public List<Question> findAll() {
        return fetchQuestions(noCondition());
    }

    @Override
    public List<Question> findByCategoryCode(String categoryCode) {
        return fetchQuestions(CATEGORY.CODE.eq(categoryCode));
    }

    /**
     * Insère chaque question puis ses réponses. {@code @Transactional} : si une seule insertion échoue
     * (doublon, catégorie inconnue…), tout le lot est annulé : aucune question à moitié écrite.
     * Les violations de contraintes remontent traduites par Spring (ex. {@code DuplicateKeyException}).
     */
    @Override
    @Transactional
    public List<Question> saveAll(List<Question> questions) {
        if (questions.isEmpty()) {
            return List.of();
        }
        questions.stream()
                .filter(question -> question.id() != null)
                .findFirst()
                .ifPresent(question -> {
                    throw new IllegalArgumentException("question " + question.id() + " is already saved");
                });
        Map<String, Long> categoryIds = resolveCategoryIds(questions);
        List<Question> saved = new ArrayList<>(questions.size());
        for (Question question : questions) {
            long id = dsl.insertInto(QUESTION, QUESTION.CATEGORY_ID, QUESTION.TEXT, QUESTION.CORRECT_OPTION_INDEX)
                    .values(categoryIds.get(question.categoryCode()), question.text(), (short) question.correctOptionIndex())
                    .returningResult(QUESTION.ID)
                    .fetchSingle(QUESTION.ID);
            insertOptions(id, question.options());
            saved.add(new Question(id, question.categoryCode(), question.text(), question.options(), question.correctOptionIndex()));
        }
        return saved;
    }

    /** Une seule requête pour toutes les catégories du lot ; une catégorie absente arrête tout. */
    private Map<String, Long> resolveCategoryIds(List<Question> questions) {
        Set<String> codes = questions.stream().map(Question::categoryCode).collect(Collectors.toSet());
        Map<String, Long> ids = dsl.select(CATEGORY.CODE, CATEGORY.ID)
                .from(CATEGORY)
                .where(CATEGORY.CODE.in(codes))
                .fetchMap(CATEGORY.CODE, CATEGORY.ID);
        codes.stream()
                .filter(code -> !ids.containsKey(code))
                .findFirst()
                .ifPresent(code -> {
                    throw new UnknownCategoryException(code);
                });
        return ids;
    }

    /** Toutes les réponses d'une question en un seul INSERT multi-lignes, avec leur position (0, 1, 2…). */
    private void insertOptions(long questionId, List<String> options) {
        var insert = dsl.insertInto(QUESTION_OPTION,
                QUESTION_OPTION.QUESTION_ID, QUESTION_OPTION.OPTION_INDEX, QUESTION_OPTION.LABEL);
        for (int index = 0; index < options.size(); index++) {
            insert = insert.values(questionId, (short) index, options.get(index));
        }
        insert.execute();
    }

    private List<Question> fetchQuestions(Condition condition) {
        return dsl.select(QUESTION.ID, CATEGORY.CODE, QUESTION.TEXT, QUESTION.CORRECT_OPTION_INDEX, OPTIONS)
                .from(QUESTION)
                .join(CATEGORY).on(QUESTION.CATEGORY_ID.eq(CATEGORY.ID))
                .where(condition)
                .orderBy(QUESTION.ID)
                .fetch(Records.mapping(JooqQuestionRepository::toDomain));
    }

    /**
     * Traduit une ligne SQL en objet du domaine. Le domaine repère la bonne réponse par sa position
     * dans la liste : les {@code option_index} doivent donc se suivre (0, 1, 2…), ce que la base
     * n'impose pas. Une donnée qui ne le respecte pas est refusée plutôt que mal interprétée.
     */
    private static Question toDomain(Long id, String categoryCode, String text, Short correctOptionIndex,
                                     Result<Record2<Short, String>> optionRecords) {
        // Sur PostgreSQL, MULTISET est émulé avec jsonb_agg : l'ordre de la sous-requête est en pratique conservé,
        // mais la norme SQL ne le garantit pas. On retrie donc par index, ce qui ne coûte rien.
        Result<Record2<Short, String>> sorted = optionRecords.sortAsc(0);
        List<String> options = new ArrayList<>(sorted.size());
        for (int expectedIndex = 0; expectedIndex < sorted.size(); expectedIndex++) {
            Record2<Short, String> option = sorted.get(expectedIndex);
            if (option.value1() != expectedIndex) {
                throw new IllegalStateException("question " + id + ": option indexes must be contiguous from 0, "
                        + "expected " + expectedIndex + " but found " + option.value1());
            }
            options.add(option.value2());
        }
        return new Question(id, categoryCode, text, options, correctOptionIndex);
    }
}
