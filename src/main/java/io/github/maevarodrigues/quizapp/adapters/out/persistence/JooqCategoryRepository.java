package io.github.maevarodrigues.quizapp.adapters.out.persistence;

import io.github.maevarodrigues.quizapp.domain.model.Category;
import io.github.maevarodrigues.quizapp.domain.port.out.CategoryRepository;
import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;

import java.util.List;

import static io.github.maevarodrigues.quizapp.adapters.out.persistence.jooq.Tables.CATEGORY;

/**
 * Adaptateur de persistance : implémente le port {@link CategoryRepository} avec jOOQ.
 */
@Repository
public class JooqCategoryRepository implements CategoryRepository {

    private final DSLContext dsl;

    public JooqCategoryRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public List<Category> findAll() {
        return dsl.select(CATEGORY.ID, CATEGORY.CODE, CATEGORY.LABEL)
                .from(CATEGORY)
                .orderBy(CATEGORY.ID)
                .fetch(Records.mapping(Category::new));
    }

    @Override
    public boolean existsByCode(String code) {
        return dsl.fetchExists(CATEGORY, CATEGORY.CODE.eq(code));
    }
}
