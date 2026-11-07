# syntax=docker/dockerfile:1
#
# Image de l'application, en deux étapes (multi-stage).
# Le jar est construit AVANT, par Maven (./mvnw package) : la génération du code jOOQ et les tests
# ont besoin de Docker (Testcontainers), qu'on ne peut pas utiliser simplement pendant un `docker build`.
#
#   ./mvnw package && docker build -t quizapp .

# --- Étape 1 : découper le jar en couches -------------------------------------------------------
FROM eclipse-temurin:21-jre-noble AS extractor
WORKDIR /builder
COPY target/quizapp-*.jar application.jar
# Le mode « tools » de Spring Boot sépare le jar selon layers.idx : dépendances, chargeur Spring Boot,
# dépendances SNAPSHOT, puis notre code. Chaque dossier deviendra une couche Docker distincte.
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# --- Étape 2 : l'image finale, avec un JRE seulement (ni JDK, ni Maven, ni sources) -----------
FROM eclipse-temurin:21-jre-noble
# Utilisateur système dédié, sans droits root : une faille dans l'application ne donne pas la main sur le conteneur.
RUN groupadd --system spring && useradd --system --gid spring --no-create-home spring
WORKDIR /application
# Du moins souvent modifié au plus souvent modifié : quand seul notre code change, Docker réutilise
# les couches des dépendances (≈ 38 Mo) déjà en cache et ne reconstruit que la dernière (quelques centaines de Ko).
COPY --from=extractor /builder/extracted/dependencies/ ./
COPY --from=extractor /builder/extracted/spring-boot-loader/ ./
COPY --from=extractor /builder/extracted/snapshot-dependencies/ ./
COPY --from=extractor /builder/extracted/application/ ./
# Dossier des fichiers importés : l'application tourne sans root, elle n'écrit que là (et dans /tmp pour Tomcat).
RUN install --directory --owner=spring --group=spring --mode=700 /application/imports
ENV QUIZAPP_IMPORT_UPLOAD_DIR=/application/imports
USER spring:spring
EXPOSE 8080
# La JVM s'adapte à la mémoire du conteneur : on lui laisse 75 % de la limite pour le tas.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "application.jar"]
