package io.github.maevarodrigues.quizapp;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le schéma Spring Batch est une copie du script officiel (changeset 005). Si une montée de version
 * de Spring Batch change ce script, ce test échoue : il faut alors écrire un nouveau changeset de migration,
 * et ne jamais modifier le 005 déjà appliqué.
 */
class SpringBatchSchemaTest {

    private static final String OFFICIAL = "org/springframework/batch/core/schema-postgresql.sql";
    private static final String VENDORED = "db/changelog/sql/005-spring-batch-6.0-schema-postgresql.sql";

    @Test
    void vendoredSchemaMatchesTheOneShippedWithSpringBatch() throws IOException {
        assertThat(withoutOurHeader(read(VENDORED))).isEqualTo(read(OFFICIAL));
    }

    private static List<String> read(String path) throws IOException {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8).lines().toList();
    }

    /** Retire les 2 lignes de provenance ajoutées en tête de la copie. */
    private static List<String> withoutOurHeader(List<String> lines) {
        return lines.subList(2, lines.size());
    }
}
