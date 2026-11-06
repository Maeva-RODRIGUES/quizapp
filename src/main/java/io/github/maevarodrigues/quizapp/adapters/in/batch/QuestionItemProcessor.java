package io.github.maevarodrigues.quizapp.adapters.in.batch;

import io.github.maevarodrigues.quizapp.domain.model.Question;
import org.springframework.batch.infrastructure.item.ItemProcessor;

/**
 * Étape « transformer » : applique les règles de qualité. Une ligne valide devient une {@link Question} ;
 * une ligne invalide lève {@link InvalidQuestionException}, que le step saute et fait rapporter.
 */
public class QuestionItemProcessor implements ItemProcessor<QuestionCsvLine, Question> {

    private final QuestionImportValidator validator;

    public QuestionItemProcessor(QuestionImportValidator validator) {
        this.validator = validator;
    }

    @Override
    public Question process(QuestionCsvLine line) {
        return switch (validator.validate(line)) {
            case ValidatedLine.Accepted accepted -> accepted.question();
            case ValidatedLine.Rejected rejected -> throw new InvalidQuestionException(rejected);
        };
    }
}
