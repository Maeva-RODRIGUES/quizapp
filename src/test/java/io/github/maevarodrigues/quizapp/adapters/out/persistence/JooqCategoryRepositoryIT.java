package io.github.maevarodrigues.quizapp.adapters.out.persistence;

import io.github.maevarodrigues.quizapp.TestcontainersConfiguration;
import io.github.maevarodrigues.quizapp.domain.model.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jooq.test.autoconfigure.JooqTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureDataSourceInitialization;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @JooqTest} : ne charge que la couche jOOQ (pas le web), sur un PostgreSQL Testcontainers.
 * {@code @AutoConfigureDataSourceInitialization} ajoute Liquibase, que la tranche jOOQ ne charge pas seule.
 */
@JooqTest
@AutoConfigureDataSourceInitialization
@Import({TestcontainersConfiguration.class, JooqCategoryRepository.class})
class JooqCategoryRepositoryIT {

    @Autowired
    private JooqCategoryRepository repository;

    @Test
    void findAll_returnsReferenceCategoriesSortedById() {
        assertThat(repository.findAll()).containsExactly(
                new Category(1L, "superstitions", "Superstitions étranges"),
                new Category(2L, "phobies", "Phobies étranges"));
    }

    @Test
    void existsByCode() {
        assertThat(repository.existsByCode("phobies")).isTrue();
        assertThat(repository.existsByCode("inconnue")).isFalse();
    }
}
