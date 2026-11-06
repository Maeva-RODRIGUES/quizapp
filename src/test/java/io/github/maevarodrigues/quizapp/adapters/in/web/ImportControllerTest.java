package io.github.maevarodrigues.quizapp.adapters.in.web;

import io.github.maevarodrigues.quizapp.domain.exception.UnknownImportException;
import io.github.maevarodrigues.quizapp.domain.model.ImportRejection;
import io.github.maevarodrigues.quizapp.domain.model.ImportReport;
import io.github.maevarodrigues.quizapp.domain.port.in.QuestionFileImportUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImportController.class)
class ImportControllerTest {

    private static final String CSV = """
            category;question;option_1;option_2;option_3;option_4;correct_option
            phobies;Comment appelle-t-on la peur des clowns ?;Coulrophobie;Clinophobie;;;1
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuestionFileImportUseCase fileImport;

    private static MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "questions.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void completedImport_returns200WithTheReport_andDeletesTheTemporaryFile() throws Exception {
        AtomicReference<Path> receivedFile = new AtomicReference<>();
        given(fileImport.importFile(any())).willAnswer(invocation -> {
            Path file = invocation.getArgument(0);
            receivedFile.set(file);
            assertThat(Files.readString(file)).isEqualTo(CSV); // le job reçoit bien le contenu envoyé
            return new ImportReport(42L, "COMPLETED", 12, 5, 7);
        });

        mockMvc.perform(multipart("/api/imports").file(csv(CSV)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value(42))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.read").value(12))
                .andExpect(jsonPath("$.written").value(5))
                .andExpect(jsonPath("$.rejected").value(7));

        assertThat(receivedFile.get()).doesNotExist();
    }

    @Test
    void failedImport_returns422WithTheReport() throws Exception {
        given(fileImport.importFile(any())).willReturn(new ImportReport(43L, "FAILED", 0, 0, 0));

        mockMvc.perform(multipart("/api/imports").file(csv(CSV)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void emptyFile_isRejectedWith400WithoutRunningTheImport() throws Exception {
        mockMvc.perform(multipart("/api/imports").file(csv("")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("the uploaded file is empty"));

        then(fileImport).should(never()).importFile(any());
    }

    @Test
    void missingFilePart_isRejectedWith400() throws Exception {
        mockMvc.perform(multipart("/api/imports"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        then(fileImport).should(never()).importFile(any());
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
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("unknown import: 999"));
    }
}
