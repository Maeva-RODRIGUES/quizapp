package io.github.maevarodrigues.quizapp.adapters.in.web;

import io.github.maevarodrigues.quizapp.adapters.in.web.dto.ImportExecutionView;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Adaptateur REST de l'import de questions : reçoit le fichier, lance l'import en arrière-plan,
 * puis permet d'en suivre l'état et d'en lire le rapport.
 */
@RestController
@RequestMapping("/api/imports")
public class ImportController {

    private final QuestionFileImportUseCase fileImport;
    private final ImportUploadDirectory uploadDirectory;

    public ImportController(QuestionFileImportUseCase fileImport, ImportUploadDirectory uploadDirectory) {
        this.fileImport = fileImport;
        this.uploadDirectory = uploadDirectory;
    }

    /**
     * {@code POST /api/imports} (multipart, champ {@code file}) : lance l'import et répond aussitôt
     * {@code 202 Accepted}, avec l'en-tête {@code Location} vers l'URL de suivi.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportExecutionView> startImport(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "the uploaded file is empty");
        }
        // Le nom de fichier envoyé par le client n'est jamais utilisé pour écrire sur le disque :
        // on crée notre propre fichier, dans un dossier privé (pas de risque de « path traversal »).
        Path tempFile = uploadDirectory.newFile();
        ImportReport report;
        try {
            file.transferTo(tempFile);
            report = fileImport.startImport(tempFile); // le fichier appartient désormais à l'import
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(tempFile); // l'import n'a pas démarré : personne d'autre ne le supprimera
            throw e;
        }
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(report.importId()).toUri();
        return ResponseEntity.accepted().location(location).body(ImportExecutionView.from(report));
    }

    /** {@code GET /api/imports/{id}} : état actuel de l'import (statut, dates, compteurs). */
    @GetMapping("/{id}")
    public ImportExecutionView importStatus(@PathVariable long id) {
        return ImportExecutionView.from(fileImport.findImport(id));
    }

    /** {@code GET /api/imports/{id}/rejections} : le rapport de qualité d'un import. */
    @GetMapping("/{id}/rejections")
    public List<RejectionView> rejections(@PathVariable long id) {
        return fileImport.findRejections(id).stream().map(RejectionView::from).toList();
    }
}
