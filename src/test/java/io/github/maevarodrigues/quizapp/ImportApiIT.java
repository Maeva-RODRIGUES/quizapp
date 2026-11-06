package io.github.maevarodrigues.quizapp;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bout en bout, en asynchrone : envoi du fichier d'exemple → {@code 202} → suivi par {@code GET /api/imports/{id}}
 * jusqu'à la fin du job → rapport de rejets et questions importées, sur PostgreSQL avec les questions de démo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import(TestcontainersConfiguration.class)
class ImportApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    /** Ce contexte est partagé avec d'autres tests (même configuration) : on remet la base dans l'état de démo. */
    @AfterEach
    void restoreDemoData() {
        jdbc.update("DELETE FROM import_rejection");
        jdbc.update("DELETE FROM question WHERE id > 10");
    }

    private static MockMultipartFile sampleFile() throws Exception {
        return new MockMultipartFile("file", "questions-import.csv", "text/csv",
                Files.readAllBytes(Path.of("samples/questions-import.csv")));
    }

    private String importStatus(long executionId) throws Exception {
        String json = mockMvc.perform(get("/api/imports/{id}", executionId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.status");
    }

    @Test
    void importRunsInTheBackground_andCanBeFollowedUntilItCompletes() throws Exception {
        String accepted = mockMvc.perform(multipart("/api/imports").file(sampleFile()))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", matchesPattern(".*/api/imports/\\d+")))
                .andReturn().getResponse().getContentAsString();
        long executionId = ((Number) JsonPath.read(accepted, "$.executionId")).longValue();

        // Polling, comme le ferait un client : sans Thread.sleep, avec un délai maximal.
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(100))
                .until(() -> !importStatus(executionId).startsWith("START"));

        mockMvc.perform(get("/api/imports/{id}", executionId))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.startTime").isNotEmpty())
                .andExpect(jsonPath("$.endTime").isNotEmpty())
                .andExpect(jsonPath("$.read").value(12))
                .andExpect(jsonPath("$.written").value(5))
                .andExpect(jsonPath("$.rejected").value(7));

        mockMvc.perform(get("/api/imports/{id}/rejections", executionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].lineNumber").value(contains(7, 8, 9, 10, 11, 12, 13, 13)))
                .andExpect(jsonPath("$[*].reason").value(contains("ALREADY_EXISTS", "UNKNOWN_CATEGORY",
                        "TOO_FEW_OPTIONS", "CORRECT_OPTION_OUT_OF_RANGE", "DUPLICATE_IN_FILE", "MALFORMED_LINE",
                        "EMPTY_TEXT", "CORRECT_OPTION_NOT_A_NUMBER")));

        // Les questions importées sont aussitôt jouables
        mockMvc.perform(get("/api/questions").param("category", "phobies"))
                .andExpect(jsonPath("$.length()").value(5 + 3));
    }

    @Test
    void temporaryUploadFile_isDeletedOnceTheImportIsOver() throws Exception {
        String accepted = mockMvc.perform(multipart("/api/imports").file(sampleFile()))
                .andReturn().getResponse().getContentAsString();
        long executionId = ((Number) JsonPath.read(accepted, "$.executionId")).longValue();
        String inputFile = jdbc.queryForObject("""
                SELECT parameter_value FROM batch_job_execution_params
                WHERE job_execution_id = ? AND parameter_name = 'input.file'""", String.class, executionId);

        await().atMost(Duration.ofSeconds(30)).until(() -> !importStatus(executionId).startsWith("START"));

        await().atMost(Duration.ofSeconds(5)).until(() -> Files.notExists(Path.of(inputFile)));
    }

    @Test
    void unknownImport_returns404() throws Exception {
        mockMvc.perform(get("/api/imports/{id}", 999_999)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/imports/{id}/rejections", 999_999)).andExpect(status().isNotFound());
    }
}
