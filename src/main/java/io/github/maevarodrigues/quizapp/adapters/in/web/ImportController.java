package io.github.maevarodrigues.quizapp.adapters.in.web;

import io.github.maevarodrigues.quizapp.adapters.in.web.dto.ImportReportView;
import io.github.maevarodrigues.quizapp.adapters.in.web.dto.RejectionView;
import io.github.maevarodrigues.quizapp.domain.model.ImportReport;
import io.github.maevarodrigues.quizapp.domain.port.in.QuestionFileImportUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Adaptateur REST de l'import de questions : reçoit le fichier, déclenche l'import, renvoie son bilan.
 */
@RestController
@RequestMapping("/api/imports")
public class ImportController {

    private final QuestionFileImportUseCase fileImport;

    public ImportController(QuestionFileImportUseCase fileImport) {
        this.fileImport = fileImport;
    }

    /**
     * {@code POST /api/imports} (multipart, champ {@code file}) : importe le CSV et attend la fin du traitement.
     * 200 si l'import est allé au bout (même avec des lignes rejetées), 422 si le fichier est inexploitable.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportReportView> importQuestions(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "the uploaded file is empty");
        }
        // Le nom de fichier envoyé par le client n'est jamais utilisé pour écrire sur le disque :
        // on crée notre propre fichier temporaire (pas de risque de « path traversal »).
        Path tempFile = Files.createTempFile("quizapp-import-", ".csv");
        try {
            file.transferTo(tempFile);
            ImportReport report = fileImport.importFile(tempFile);
            HttpStatus status = report.completed() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_CONTENT;
            return ResponseEntity.status(status).body(ImportReportView.from(report));
        } finally {
            Files.deleteIfExists(tempFile); // import synchrone : le fichier n'est plus utile
        }
    }

    /** {@code GET /api/imports/{id}/rejections} : le rapport de qualité d'un import. */
    @GetMapping("/{id}/rejections")
    public List<RejectionView> rejections(@PathVariable long id) {
        return fileImport.findRejections(id).stream().map(RejectionView::from).toList();
    }
}
