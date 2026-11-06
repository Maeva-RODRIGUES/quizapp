package io.github.maevarodrigues.quizapp.domain.port.out;

import io.github.maevarodrigues.quizapp.domain.model.ImportRejection;

import java.util.List;

/**
 * Port de sortie : le rapport des lignes rejetées par les imports.
 */
public interface ImportRejectionRepository {

    void saveAll(List<ImportRejection> rejections);
}
