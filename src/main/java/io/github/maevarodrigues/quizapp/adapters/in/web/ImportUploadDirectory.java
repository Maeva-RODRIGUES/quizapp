package io.github.maevarodrigues.quizapp.adapters.in.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * Dossier privé où sont déposés les fichiers reçus par {@code POST /api/imports}, en attendant leur import.
 * <p>
 * On n'écrit pas directement dans le dossier temporaire du système ({@code /tmp}), partagé par tous les
 * utilisateurs de la machine : ce dossier-ci est créé au démarrage, réservé à l'utilisateur de l'application
 * (droits {@code rwx------}), et un lien symbolique posé à sa place est refusé.
 * L'application ne démarre pas si le dossier ne peut pas être sécurisé (mieux vaut échouer tôt).
 */
@Component
public class ImportUploadDirectory {

    private static final Set<PosixFilePermission> OWNER_ONLY = PosixFilePermissions.fromString("rwx------");

    private final Path directory;

    public ImportUploadDirectory(@Value("${quizapp.import.upload-dir}") Path directory) {
        this.directory = directory.toAbsolutePath().normalize();
        try {
            prepare(this.directory);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot prepare the import upload directory " + this.directory, e);
        }
    }

    private static void prepare(Path directory) throws IOException {
        if (Files.isSymbolicLink(directory)) {
            throw new IOException("refusing a symbolic link as upload directory");
        }
        Files.createDirectories(directory);
        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            // Échoue si le dossier existe déjà et appartient à un autre utilisateur.
            Files.setPosixFilePermissions(directory, OWNER_ONLY);
        }
    }

    /** Crée un nouveau fichier vide, au nom imprévisible, dans le dossier d'upload. */
    public Path newFile() throws IOException {
        return Files.createTempFile(directory, "quizapp-import-", ".csv");
    }

    Path path() {
        return directory;
    }
}
