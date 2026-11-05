package io.github.maevarodrigues.quizapp.adapters.out.persistence;

import org.jooq.ExecuteContext;
import org.jooq.ExecuteListener;
import org.jooq.ExecuteListenerProvider;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Compte les requêtes SQL exécutées par jOOQ, pour vérifier qu'une lecture ne déclenche pas N+1 requêtes.
 */
@TestConfiguration(proxyBeanMethods = false)
public class QueryCounter {

    private final AtomicInteger count = new AtomicInteger();

    @Bean
    ExecuteListenerProvider queryCountingListener() {
        return new DefaultExecuteListenerProvider(new ExecuteListener() {
            @Override
            public void executeStart(ExecuteContext ctx) {
                count.incrementAndGet();
            }
        });
    }

    public void reset() {
        count.set(0);
    }

    public int count() {
        return count.get();
    }
}
