package io.github.maevarodrigues.quizapp.adapters.in.batch;

/**
 * Motifs de rejet d'une ligne d'import. Le nom de la constante est le code stocké dans
 * {@code import_rejection.reason} : il ne doit pas changer une fois des rejets enregistrés.
 */
public enum RejectionReason {

    MALFORMED_LINE("la ligne n'a pas le bon nombre de colonnes (ou des guillemets mal fermés)"),
    UNKNOWN_CATEGORY("la catégorie n'existe pas dans le référentiel"),
    EMPTY_TEXT("l'énoncé est vide"),
    TEXT_TOO_LONG("l'énoncé dépasse la longueur maximale"),
    TOO_FEW_OPTIONS("moins de 2 réponses proposées"),
    OPTIONS_NOT_CONTIGUOUS("une colonne de réponse vide est suivie d'une réponse remplie"),
    OPTION_TOO_LONG("une réponse dépasse la longueur maximale"),
    DUPLICATE_OPTIONS("deux réponses identiques (sans tenir compte de la casse)"),
    CORRECT_OPTION_NOT_A_NUMBER("le numéro de la bonne réponse n'est pas un nombre entier"),
    CORRECT_OPTION_OUT_OF_RANGE("le numéro de la bonne réponse ne correspond à aucune réponse proposée"),
    DUPLICATE_IN_FILE("la même question apparaît plus haut dans le fichier"),
    ALREADY_EXISTS("la question existe déjà en base");

    private final String description;

    RejectionReason(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
