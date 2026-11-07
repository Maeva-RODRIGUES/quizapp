# quizapp

[![CI](https://github.com/Maeva-RODRIGUES/quizapp/actions/workflows/ci.yml/badge.svg)](https://github.com/Maeva-RODRIGUES/quizapp/actions/workflows/ci.yml)

API REST de quiz en Java / Spring Boot, autour de deux catégories décalées :
**Superstitions étranges** et **Phobies étranges**.

> 🚧 **Projet en cours de refonte.**
> La première version (2024) est un prototype minimal. Je la fais évoluer vers une application
> structurée autour d'une stack orientée données : PostgreSQL, Liquibase, jOOQ, Spring Batch
> et architecture hexagonale. La feuille de route ci-dessous indique ce qui est fait et ce qui reste à faire.

## État actuel

| | Aujourd'hui |
|---|---|
| Langage / framework | Java 21, Spring Boot 4.1, image Docker (multi-stage, non-root) |
| Base de données | PostgreSQL 18, schéma versionné avec Liquibase (catégories, questions, réponses, contraintes de qualité), catégories de référence et 10 questions de démo, tables de suivi Spring Batch et rapport des lignes rejetées à l'import (préparés pour l'import CSV) |
| Domaine | modèle métier en Java pur (questions, catégories, correction et score), cas d'usage et ports, sans dépendance au framework |
| Persistance | adaptateurs jOOQ (code généré depuis le schéma Liquibase) : lecture des questions avec leurs réponses en une requête (`MULTISET`), écriture transactionnelle |
| API | `GET /api/categories`, `GET /api/questions` (filtre `?category=`) sans jamais exposer la bonne réponse, `POST /api/quiz/submit` (score et correction), entrées validées, erreurs au format standard *Problem Details* (RFC 9457) |
| Import de données | `POST /api/imports` : job Spring Batch d'import CSV **asynchrone** avec règles de qualité (validation, dédoublonnage), suivi de l'avancement, rapport des lignes rejetées et compteurs |
| Tests | tests unitaires (domaine, services avec Mockito), règles d'architecture (ArchUnit), tests d'intégration sur un vrai PostgreSQL (Testcontainers), exécutés en CI à chaque push |

Limites connues : un import interrompu par un arrêt brutal de l'application reste au statut `STARTED`
(pas encore de reprise ni de nettoyage automatique de ces exécutions).

Prochaine étape : enrichissement de la CI (construction de l'image Docker) et analyse de qualité SonarCloud.

## Architecture

Architecture hexagonale (*ports & adapters*) : le métier est au centre, les détails techniques autour.
Les dépendances pointent toujours vers le centre, et une règle ArchUnit le vérifie à chaque build.

```
io.github.maevarodrigues.quizapp
├── domain                  cœur métier, Java pur
│   ├── model               Category, Question, QuizSubmission, QuizResult…
│   ├── exception           erreurs métier
│   └── port
│       ├── in              cas d'usage offerts (ListCategoriesUseCase, SubmitQuizUseCase…)
│       └── out             besoins vers l'extérieur (QuestionRepository, CategoryRepository)
├── application             services qui implémentent les ports « in » à l'aide des ports « out »
├── adapters
│   ├── in/web              controllers REST et DTO : n'appellent que les ports « in »
│   └── out/persistence     implémentations jOOQ des ports « out » (+ code jOOQ généré)
│                           (à venir : in/batch pour l'import)
└── config                  assemblage des beans Spring (services applicatifs)
```

## Stack cible

- **Java 21**, **Spring Boot** (dernière version maintenue), Maven
- **PostgreSQL** (via Docker Compose en local), schéma versionné avec **Liquibase**
- **jOOQ** pour l'accès aux données, avec du code généré à partir du schéma
- **Architecture hexagonale** : domaine indépendant du framework, adaptateurs REST / persistance / batch
- **Spring Batch** : import de questions depuis un fichier CSV avec des règles de qualité des données
  (validation, dédoublonnage, rapport des lignes rejetées)
- **Tests** : JUnit 5, Mockito, `@WebMvcTest`, Testcontainers
- **Docker** (image multi-stage), **CI GitHub Actions**, analyse **SonarCloud**

## Feuille de route

- [x] **Phase 0** : réparer le build, documenter l'état du projet
- [x] **Phase 1** : passer à Java 21 et à une version maintenue de Spring Boot, nettoyer le contenu des questions, intégration continue
- [x] **Phase 2** : PostgreSQL avec Docker Compose, schéma et données de référence avec Liquibase
- [x] **Phase 3** : modèle de domaine hexagonal, tests unitaires et règles d'architecture
- [x] **Phase 4** : persistance avec jOOQ, tests d'intégration Testcontainers
- [x] **Phase 5** : API complète (catégories, questions par catégorie, soumission et score), validation, gestion d'erreurs
- [x] **Phase 6** : job Spring Batch d'import CSV avec règles de qualité des données
- [ ] **Phase 7** : Dockerfile, intégration continue, SonarCloud
- [ ] **Phase 8** : documentation finale
- [ ] **Bonus** : front Angular minimal, déploiement AWS décrit en Terraform

## Lancer le projet

Prérequis :

- un JDK 21 ou plus récent (Maven n'est pas nécessaire, le wrapper `./mvnw` est fourni) ;
- Docker avec Docker Compose, **y compris pour compiler** :
  - la génération du code jOOQ démarre un PostgreSQL éphémère et y applique les changelogs Liquibase ;
  - les tests d'intégration utilisent un PostgreSQL éphémère (Testcontainers) ;
  - `./mvnw spring-boot:run` démarre automatiquement PostgreSQL (`compose.yaml`, port local 5433).

```bash
# Compiler et lancer les tests
./mvnw verify

# Démarrer l'application (port 8080) avec les questions de démo
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Le profil `local` ajoute les 10 questions de démonstration en base. Sans lui, seules les catégories
de référence sont chargées.

Pour explorer la base :

```bash
docker compose exec postgres psql -U quizapp -d quizapp
```

### Lancer l'application dans Docker

L'image est construite à partir du jar produit par Maven (multi-stage : JRE seul, utilisateur non-root,
couches séparées pour que les dépendances restent en cache) :

```bash
./mvnw package
docker compose --profile app up --build    # application (port 8080) + PostgreSQL, avec les questions de démo
docker compose --profile app down          # arrêt (les données sont conservées dans le volume)
```

Sans `--profile app`, `docker compose up` ne démarre que PostgreSQL, comme `./mvnw spring-boot:run`.

## API

| Méthode | URL | Description |
|---|---|---|
| `GET` | `/api/categories` | catégories disponibles |
| `GET` | `/api/questions` | toutes les questions |
| `GET` | `/api/questions?category=phobies` | questions d'une catégorie (par son code) |
| `POST` | `/api/quiz/submit` | corrige les réponses au quiz d'une catégorie |
| `POST` | `/api/imports` | lance l'import d'un fichier CSV de questions, en arrière-plan (voir [Import et qualité des données](#import-et-qualité-des-données)) |
| `GET` | `/api/imports/{id}` | état d'un import : statut, dates, compteurs |
| `GET` | `/api/imports/{id}/rejections` | lignes rejetées par un import, avec leur motif |

Les questions sont renvoyées **sans la bonne réponse** : elle n'est révélée qu'après soumission.

```bash
curl -s http://localhost:8080/api/categories
```

```json
[
  {"code": "superstitions", "label": "Superstitions étranges"},
  {"code": "phobies", "label": "Phobies étranges"}
]
```

```bash
curl -s "http://localhost:8080/api/questions?category=phobies"
```

```json
[
  {
    "id": 6,
    "category": "phobies",
    "text": "Quel est le nom de la peur des araignées ?",
    "options": ["Entomophobie", "Myrmécophobie", "Arachnophobie"]
  }
]
```

Soumettre un quiz : `answers` associe l'id de chaque question à l'index de la réponse choisie (à partir de 0).
Le quiz comprend **toutes** les questions de la catégorie : une question sans réponse compte comme fausse.

```bash
curl -s -X POST http://localhost:8080/api/quiz/submit \
  -H 'Content-Type: application/json' \
  -d '{"category": "phobies", "answers": {"6": 2, "7": 1}}'
```

```json
{
  "score": 1,
  "total": 5,
  "corrections": [
    {"questionId": 6, "givenOptionIndex": 2, "correctOptionIndex": 2, "correct": true},
    {"questionId": 7, "givenOptionIndex": 1, "correctOptionIndex": 0, "correct": false},
    {"questionId": 8, "givenOptionIndex": null, "correctOptionIndex": 1, "correct": false}
  ]
}
```

(extrait : une correction par question de la catégorie)

### Erreurs

Toutes les erreurs suivent le format *Problem Details* (RFC 9457, `Content-Type: application/problem+json`) :

| Cas | Statut |
|---|---|
| requête invalide (champ manquant, index négatif, JSON illisible…) | `400`, avec la liste des champs en cause dans `errors` |
| soumission incohérente (question hors du quiz, réponse inexistante) | `400` |
| catégorie ou import inconnu, route inexistante | `404` |
| `Content-Type` autre que JSON | `415` |
| erreur inattendue | `500`, sans détail technique (la cause est dans les logs) |

```bash
curl -s "http://localhost:8080/api/questions?category=inconnue"
```

```json
{
  "title": "Unknown category",
  "status": 404,
  "detail": "unknown category: inconnue",
  "instance": "/api/questions"
}
```

```bash
curl -s -X POST http://localhost:8080/api/quiz/submit \
  -H 'Content-Type: application/json' \
  -d '{"category": " ", "answers": {}}'
```

```json
{
  "title": "Invalid request",
  "status": 400,
  "detail": "The request contains invalid fields.",
  "instance": "/api/quiz/submit",
  "errors": [
    {"field": "answers", "message": "must not be empty"},
    {"field": "category", "message": "must not be blank"}
  ]
}
```

Les messages de validation sont traduits selon l'en-tête `Accept-Language` (par défaut, la langue du serveur).

## Import et qualité des données

L'import de questions est un **job Spring Batch** : le fichier est lu ligne par ligne, chaque ligne passe
des **règles de qualité**, et les questions valides sont écrites en base par paquets de 100, chacun dans une transaction.
Une ligne invalide n'arrête pas l'import : elle est **rejetée**, et consignée avec son numéro, son contenu et son motif.

Format attendu (UTF-8, séparateur `;`, en-tête obligatoire, de 2 à 4 réponses, bonne réponse numérotée **à partir de 1**) :

```
category;question;option_1;option_2;option_3;option_4;correct_option
phobies;Comment appelle-t-on la peur des clowns ?;Coulrophobie;Clinophobie;Cynophobie;;1
```

| Motif | Règle |
|---|---|
| `MALFORMED_LINE` | la ligne n'a pas exactement 7 colonnes |
| `UNKNOWN_CATEGORY` | la catégorie n'existe pas dans le référentiel |
| `EMPTY_TEXT` / `TEXT_TOO_LONG` | énoncé vide / de plus de 500 caractères |
| `TOO_FEW_OPTIONS` | moins de 2 réponses |
| `OPTIONS_NOT_CONTIGUOUS` | colonne de réponse vide suivie d'une réponse remplie |
| `OPTION_TOO_LONG` / `DUPLICATE_OPTIONS` | réponse de plus de 200 caractères / deux réponses identiques |
| `CORRECT_OPTION_NOT_A_NUMBER` / `CORRECT_OPTION_OUT_OF_RANGE` | numéro de bonne réponse illisible / sans réponse correspondante |
| `DUPLICATE_IN_FILE` | même question plus haut dans le fichier (casse et espaces ignorés) |
| `ALREADY_EXISTS` | même question déjà en base |

Une ligne peut être rejetée pour plusieurs motifs à la fois. Un en-tête incorrect fait échouer tout l'import (statut `FAILED`).

Essai avec le fichier d'exemple [`samples/questions-import.csv`](samples/questions-import.csv), qui contient des erreurs volontaires :

L'import est **asynchrone** : la requête répond tout de suite `202 Accepted`, avec l'URL de suivi dans l'en-tête
`Location`, et le fichier est traité en arrière-plan (au plus 2 imports en parallèle, les suivants attendent leur tour).

```bash
curl -si -F file=@samples/questions-import.csv http://localhost:8080/api/imports
```

```
HTTP/1.1 202
Location: http://localhost:8080/api/imports/1

{"executionId": 1, "status": "STARTING", "startTime": null, "endTime": null, "read": 0, "written": 0, "rejected": 0}
```

On suit l'avancement (les compteurs progressent à chaque paquet de 100 lignes) jusqu'à `COMPLETED` ou `FAILED` :

```bash
curl -s http://localhost:8080/api/imports/1
```

```json
{"executionId": 1, "status": "COMPLETED", "startTime": "2026-11-06T19:25:45", "endTime": "2026-11-06T19:25:45",
 "read": 12, "written": 5, "rejected": 7}
```

Puis on consulte le rapport de qualité :

```bash
curl -s http://localhost:8080/api/imports/1/rejections
```

```json
[
  {"lineNumber": 7, "reason": "ALREADY_EXISTS", "rawLine": "phobies;QUEL est le nom de la peur des araignées ?;Arachnophobie;Entomophobie;;;1"},
  {"lineNumber": 8, "reason": "UNKNOWN_CATEGORY", "rawLine": "astrologie;Quel est l'élément du signe du Bélier ?;Feu;Eau;Air;Terre;1"},
  {"lineNumber": 12, "reason": "MALFORMED_LINE", "rawLine": "phobies;Ligne incomplète;Oui;Non"}
]
```

(extrait ; réimporter le même fichier n'écrit rien : toutes les lignes valides sont alors `ALREADY_EXISTS`)

Le statut et les compteurs ne sont pas recalculés : ils sont lus dans les tables de Spring Batch (`batch_job_execution`,
`batch_step_execution`), et le rapport dans `import_rejection`. Le fichier reçu est supprimé à la fin de l'import.
