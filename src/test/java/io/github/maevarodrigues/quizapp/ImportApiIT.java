package io.github.maevarodrigues.quizapp;

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

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bout en bout : envoi du fichier d'exemple {@code samples/questions-import.csv} → job Spring Batch → PostgreSQL
 * (avec les 10 questions de démo), puis lecture du rapport de rejets et des questions importées.
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

    @Test
    void importingTheSampleFile_writesValidQuestions_andReportsRejectedLines() throws Exception {
        String response = mockMvc.perform(multipart("/api/imports").file(sampleFile()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.read").value(12))
                .andExpect(jsonPath("$.written").value(5))
                .andExpect(jsonPath("$.rejected").value(7))
                .andReturn().getResponse().getContentAsString();
        long executionId = Long.parseLong(response.replaceAll(".*\"executionId\":(\\d+).*", "$1"));

        mockMvc.perform(get("/api/imports/{id}/rejections", executionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].lineNumber").value(contains(7, 8, 9, 10, 11, 12, 13, 13)))
                .andExpect(jsonPath("$[*].reason").value(contains("ALREADY_EXISTS", "UNKNOWN_CATEGORY",
                        "TOO_FEW_OPTIONS", "CORRECT_OPTION_OUT_OF_RANGE", "DUPLICATE_IN_FILE", "MALFORMED_LINE",
                        "EMPTY_TEXT", "CORRECT_OPTION_NOT_A_NUMBER")))
                .andExpect(jsonPath("$[5].rawLine").value("phobies;Ligne incomplète;Oui;Non"));

        // Les questions importées sont aussitôt jouables
        mockMvc.perform(get("/api/questions").param("category", "phobies"))
                .andExpect(jsonPath("$.length()").value(5 + 3));
    }

    @Test
    void rejectionsOfAnUnknownImport_return404() throws Exception {
        mockMvc.perform(get("/api/imports/{id}/rejections", 999_999))
                .andExpect(status().isNotFound());
    }
}
