package io.github.maevarodrigues.quizapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.batch.autoconfigure.BatchTaskExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Rend le lancement des jobs asynchrone : {@code @BatchTaskExecutor} désigne à Spring Boot l'exécuteur
 * à donner au {@code JobOperator}. Le job lui-même ne change pas, seul son déclenchement change.
 */
@Configuration(proxyBeanMethods = false)
public class BatchAsyncConfiguration {

    /**
     * Pool borné : au plus {@code max-concurrent} imports en parallèle, les suivants attendent dans une file.
     * {@code defaultCandidate = false} : ce pool est réservé à Spring Batch et ne remplace pas l'exécuteur
     * par défaut de l'application.
     */
    @Bean(defaultCandidate = false)
    @BatchTaskExecutor
    TaskExecutor importTaskExecutor(@Value("${quizapp.import.max-concurrent:2}") int maxConcurrent,
                                    @Value("${quizapp.import.queue-capacity:20}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(maxConcurrent);
        executor.setMaxPoolSize(maxConcurrent);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("import-");
        // À l'arrêt de l'application, laisser une chance aux imports en cours de se terminer.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
