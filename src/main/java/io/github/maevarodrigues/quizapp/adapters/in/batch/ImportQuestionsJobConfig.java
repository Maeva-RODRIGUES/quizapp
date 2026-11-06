package io.github.maevarodrigues.quizapp.adapters.in.batch;

import io.github.maevarodrigues.quizapp.domain.model.Question;
import io.github.maevarodrigues.quizapp.domain.port.in.ImportQuestionsUseCase;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.charset.StandardCharsets;

/**
 * Le job d'import : un seul step « orienté chunks » qui lit le CSV, valide chaque ligne et écrit
 * les questions valides par paquets de {@value #CHUNK_SIZE}, chaque paquet dans une transaction.
 * <p>
 * Les beans {@code @StepScope} sont recréés à chaque exécution : c'est ce qui donne à chaque import
 * son propre fichier (paramètre du job), sa propre mémoire de dédoublonnage et son propre id de rapport.
 */
@Configuration(proxyBeanMethods = false)
public class ImportQuestionsJobConfig {

    public static final String JOB_NAME = "importQuestionsJob";
    /** Paramètre du job : chemin du fichier CSV à importer. */
    public static final String INPUT_FILE = "input.file";
    static final int CHUNK_SIZE = 100;

    @Bean
    Job importQuestionsJob(JobRepository jobRepository, Step importQuestionsStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(importQuestionsStep)
                .build();
    }

    @Bean
    Step importQuestionsStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                             FlatFileItemReader<QuestionCsvLine> questionCsvReader,
                             QuestionItemProcessor questionItemProcessor,
                             QuestionItemWriter questionItemWriter,
                             RejectionSkipListener rejectionSkipListener,
                             @Value("${quizapp.import.skip-limit:1000}") long skipLimit) {
        return new StepBuilder("importQuestionsStep", jobRepository)
                .<QuestionCsvLine, Question>chunk(CHUNK_SIZE)
                .transactionManager(transactionManager)
                .reader(questionCsvReader)
                .processor(questionItemProcessor)
                .writer(questionItemWriter)
                .faultTolerant()
                // Seules les erreurs de DONNÉES sont sautées ; une panne technique (base indisponible…) fait échouer le job.
                .skip(FlatFileParseException.class, InvalidQuestionException.class)
                // Au-delà, le fichier est jugé inexploitable et le job échoue.
                .skipLimit(skipLimit)
                .skipListener(rejectionSkipListener)
                // En cas de rejeu d'un chunk, réutiliser les résultats du processor au lieu de le rappeler :
                // le validateur a une mémoire (doublons) et prendrait une ligne déjà vue pour un doublon.
                .processorNonTransactional()
                .build();
    }

    @Bean
    @StepScope
    FlatFileItemReader<QuestionCsvLine> questionCsvReader(@Value("#{jobParameters['" + INPUT_FILE + "']}") String inputFile) {
        return new FlatFileItemReaderBuilder<QuestionCsvLine>()
                .name("questionCsvReader")
                .resource(new FileSystemResource(inputFile))
                .encoding(StandardCharsets.UTF_8.name())
                .linesToSkip(1)
                .skippedLinesCallback(QuestionCsvLineMapper::checkHeader)
                .lineMapper(new QuestionCsvLineMapper())
                .build();
    }

    @Bean
    @StepScope
    QuestionItemProcessor questionItemProcessor(ImportQuestionsUseCase importQuestions) {
        return new QuestionItemProcessor(
                new QuestionImportValidator(importQuestions::categoryExists, importQuestions::questionExists));
    }

    @Bean
    QuestionItemWriter questionItemWriter(ImportQuestionsUseCase importQuestions) {
        return new QuestionItemWriter(importQuestions);
    }

    @Bean
    @StepScope
    RejectionSkipListener rejectionSkipListener(ImportQuestionsUseCase importQuestions,
                                                @Value("#{stepExecution.jobExecution.id}") long jobExecutionId) {
        return new RejectionSkipListener(importQuestions, jobExecutionId);
    }
}
