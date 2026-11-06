package io.github.maevarodrigues.quizapp.config;

import io.github.maevarodrigues.quizapp.application.CategoryService;
import io.github.maevarodrigues.quizapp.application.QuestionImportService;
import io.github.maevarodrigues.quizapp.application.QuizService;
import io.github.maevarodrigues.quizapp.domain.port.out.CategoryRepository;
import io.github.maevarodrigues.quizapp.domain.port.out.ImportRejectionRepository;
import io.github.maevarodrigues.quizapp.domain.port.out.QuestionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assemble l'hexagone : déclare les services applicatifs comme beans Spring, en leur injectant
 * les adaptateurs qui implémentent les ports « out ».
 * <p>
 * C'est le seul endroit qui connaît à la fois Spring et les classes de la couche application :
 * celles-ci restent ainsi sans annotation ni dépendance au framework.
 */
@Configuration(proxyBeanMethods = false)
public class BeanConfiguration {

    @Bean
    CategoryService categoryService(CategoryRepository categoryRepository) {
        return new CategoryService(categoryRepository);
    }

    @Bean
    QuizService quizService(CategoryRepository categoryRepository, QuestionRepository questionRepository) {
        return new QuizService(categoryRepository, questionRepository);
    }

    @Bean
    QuestionImportService questionImportService(CategoryRepository categoryRepository,
            QuestionRepository questionRepository, ImportRejectionRepository rejectionRepository) {
        return new QuestionImportService(categoryRepository, questionRepository, rejectionRepository);
    }
}
