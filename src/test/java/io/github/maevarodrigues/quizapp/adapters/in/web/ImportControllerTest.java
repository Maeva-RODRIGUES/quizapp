package io.github.maevarodrigues.quizapp.adapters.in.web;

import io.github.maevarodrigues.quizapp.domain.exception.UnknownImportException;
import io.github.maevarodrigues.quizapp.domain.model.ImportRejection;
import io.github.maevarodrigues.quizapp.domain.model.ImportReport;
import io.github.maevarodrigues.quizapp.domain.port.in.QuestionFileImportUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImportController.class)
@Import(ImportUploadDirectory.class)
class ImportControllerTest {

    private static final String CSV = """
            category;question;option_1;option_2;option_3;option_4;correct_option
            phobies;Comment appelle-t-on la peur des clowns ?;Coulrophobie;Clinophobie;;;1
            """;
    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 6, 10, 0, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ImportUploadDirectory uploadDirectory;

    @MockitoBean
    private QuestionFileImportUseCase fileImport;

    private static MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "questions.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void startImport_returns202WithLocation_andHandsTheFileOverToTheImport() throws Exception {
        AtomicReference<Path> receivedFile = new AtomicReference<>();
        given(fileImport.startImport(any())).willAnswer(invocation -> {
            receivedFile.set(invocation.getArgument(0));
            assertThat(Files.readString(receivedFile.get())).isEqualTo(CSV); // le job reçoit bien le contenu envoyé
            return new ImportReport(42L, "STARTING", null, null, 0, 0, 0);
        });

        mockMvc.perform(multipart("/api/imports").file(csv(CSV)))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", "http://localhost/api/imports/42"))
                .andExpect(jsonPath("$.executionId").value(42))
                .andExpect(jsonPath("$.status").value("STARTING"))
                .andExpect(jsonPath("$.endTime").isEmpty());

        // Le controller ne supprime plus le fichier : l'import tourne encore, c'est lui qui le supprimera.
        assertThat(receivedFile.get()).exists().hasParent(uploadDirectory.path());
        Files.deleteIfExists(receivedFile.get());
    }

    @Test
    void importThatCannotStart_deletesTheTemporaryFile() throws Exception {
        AtomicReference<Path> receivedFile = new AtomicReference<>();
        given(fileImport.startImport(any())).willAnswer(invocation -> {
            receivedFile.set(invocation.getArgument(0));
            throw new IllegalStateException("could not start the import job");
        });

        mockMvc.perform(multipart("/api/imports").file(csv(CSV)))
                .andExpect(status().isInternalServerError());

        assertThat(receivedFile.get()).doesNotExist();
    }

    @Test
    void emptyFile_isRejectedWith400WithoutStartingTheImport() throws Exception {
        mockMvc.perform(multipart("/api/imports").file(csv("")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("the uploaded file is empty"));

        then(fileImport).should(never()).startImport(any());
    }

    @Test
    void missingFilePart_isRejectedWith400() throws Exception {
        mockMvc.perform(multipart("/api/imports"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        then(fileImport).should(never()).startImport(any());
    }

    @Test
    void importStatus_returnsStatusDatesAndCounters() throws Exception {
        given(fileImport.findImport(42L))
                .willReturn(new ImportReport(42L, "COMPLETED", START, START.plusSeconds(2), 12, 5, 7));

        mockMvc.perform(get("/api/imports/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.startTime").value("2026-11-06T10:00:00"))
                .andExpect(jsonPath("$.endTime").value("2026-11-06T10:00:02"))
                .andExpect(jsonPath("$.read").value(12))
                .andExpect(jsonPath("$.written").value(5))
                .andExpect(jsonPath("$.rejected").value(7));
    }

    @Test
    void statusOfAnUnknownImport_returns404() throws Exception {
        given(fileImport.findImport(999L)).willThrow(new UnknownImportException(999L));

        mockMvc.perform(get("/api/imports/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("unknown import: 999"));
    }

    @Test
    void rejections_areListedWithLineNumberReasonAndRawLine() throws Exception {
        given(fileImport.findRejections(42L)).willReturn(List.of(
                new ImportRejection(42L, 8, "phobies;Q ?;A;;;;1", "TOO_FEW_OPTIONS")));

        mockMvc.perform(get("/api/imports/42/rejections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lineNumber").value(8))
                .andExpect(jsonPath("$[0].reason").value("TOO_FEW_OPTIONS"))
                .andExpect(jsonPath("$[0].rawLine").value("phobies;Q ?;A;;;;1"));
    }

    @Test
    void rejectionsOfAnUnknownImport_return404() throws Exception {
        given(fileImport.findRejections(999L)).willThrow(new UnknownImportException(999L));

        mockMvc.perform(get("/api/imports/999/rejections"))
                .andExpect(status().isNotFound());
    }
}
