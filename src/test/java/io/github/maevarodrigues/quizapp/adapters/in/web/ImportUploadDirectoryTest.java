package io.github.maevarodrigues.quizapp.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImportUploadDirectoryTest {

    @Test
    void createsTheMissingDirectory_andFilesGoInside(@TempDir Path root) throws Exception {
        Path dir = root.resolve("uploads/nested");

        ImportUploadDirectory uploads = new ImportUploadDirectory(dir);
        Path file = uploads.newFile();

        assertThat(dir).isDirectory();
        assertThat(file).exists().hasParent(dir);
        assertThat(file.getFileName().toString()).startsWith("quizapp-import-").endsWith(".csv");
    }

    @Test
    void twoFiles_neverShareAName(@TempDir Path root) throws Exception {
        ImportUploadDirectory uploads = new ImportUploadDirectory(root.resolve("uploads"));

        assertThat(uploads.newFile()).isNotEqualTo(uploads.newFile());
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void directoryIsReservedToItsOwner_evenIfItAlreadyExistedWithWiderRights(@TempDir Path root) throws Exception {
        Path dir = Files.createDirectory(root.resolve("uploads"),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxrwxrwx")));

        new ImportUploadDirectory(dir);

        assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(dir))).isEqualTo("rwx------");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void symbolicLink_isRefused(@TempDir Path root) throws Exception {
        Path target = Files.createDirectory(root.resolve("elsewhere"));
        Path link = Files.createSymbolicLink(root.resolve("uploads"), target);

        assertThatThrownBy(() -> new ImportUploadDirectory(link))
                .isInstanceOf(UncheckedIOException.class)
                .hasRootCauseMessage("refusing a symbolic link as upload directory");
    }
}
