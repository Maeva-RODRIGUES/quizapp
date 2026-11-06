package io.github.maevarodrigues.quizapp.adapters.in.batch;

import io.github.maevarodrigues.quizapp.domain.model.Question;
import io.github.maevarodrigues.quizapp.domain.port.in.ImportQuestionsUseCase;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

import java.util.List;

/**
 * Étape « écrire » : enregistre les questions valides d'un chunk via le cas d'usage d'import,
 * dans la transaction du chunk (tout le chunk ou rien).
 */
public class QuestionItemWriter implements ItemWriter<Question> {

    private final ImportQuestionsUseCase importQuestions;

    public QuestionItemWriter(ImportQuestionsUseCase importQuestions) {
        this.importQuestions = importQuestions;
    }

    @Override
    public void write(Chunk<? extends Question> chunk) {
        importQuestions.importQuestions(List.copyOf(chunk.getItems()));
    }
}
