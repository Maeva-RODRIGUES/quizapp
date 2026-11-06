package io.github.maevarodrigues.quizapp.adapters.out.persistence;

import io.github.maevarodrigues.quizapp.domain.model.ImportRejection;
import io.github.maevarodrigues.quizapp.domain.port.out.ImportRejectionRepository;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;

import static io.github.maevarodrigues.quizapp.adapters.out.persistence.jooq.Tables.IMPORT_REJECTION;

/**
 * Adaptateur de persistance du rapport de rejets, avec jOOQ.
 */
@Repository
public class JooqImportRejectionRepository implements ImportRejectionRepository {

    private final DSLContext dsl;

    public JooqImportRejectionRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /** Un seul INSERT multi-lignes ; {@code created_at} est rempli par la base. */
    @Override
    public void saveAll(List<ImportRejection> rejections) {
        if (rejections.isEmpty()) {
            return;
        }
        var insert = dsl.insertInto(IMPORT_REJECTION,
                IMPORT_REJECTION.JOB_EXECUTION_ID, IMPORT_REJECTION.LINE_NUMBER,
                IMPORT_REJECTION.RAW_LINE, IMPORT_REJECTION.REASON);
        for (ImportRejection rejection : rejections) {
            insert = insert.values(rejection.importId(), rejection.lineNumber(), rejection.rawLine(), rejection.reason());
        }
        insert.execute();
    }
}
