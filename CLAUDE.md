# apps (code et CI)

Contexte, décisions et règles critiques : voir le CLAUDE.md du workspace parent. Ce fichier ne contient que les règles propres à ce repo. Décisions détaillées et plan d'initialisation : `../docs/plan-apps.md`.

## Périmètre
- Monorepo Gradle en Kotlin DSL, conventions dans `build-logic` (precompiled script plugins).
- Un seul déployable pour l'instant : `services/api` (Spring Boot + Spring Modulith). Front : `services/api-app` (Angular, construit par Gradle).
- Produit des images Docker publiées sur GHCR.
- Ne contient aucune infra : manifests et OpenTofu vivent dans `platform`.
- Aucune référence à un employeur ni code repris d'un projet professionnel.

## Commandes
- `./gradlew check` : vérification complète, y compris les tests TestKit des convention plugins (`build-logic`).
- `./gradlew :build-logic:functionalTest` : seulement les tests des convention plugins.
- `./gradlew :build-logic:spotlessApply` : formate le code Kotlin de `build-logic` (ktlint).
- `./gradlew pitest` : mutation testing, hors `check`.
- Le daemon Gradle tourne sur Temurin 25 (`gradle/gradle-daemon-jvm.properties`), téléchargé par foojay si absent. Toute modification d'un convention plugin s'accompagne d'un test dans `build-logic/src/functionalTest`.

## Architecture
- Spring Modulith obligatoire : un module applicatif = un package de premier niveau sous `dev.ylp`. `ApplicationModules.verify()` doit passer.
- Dans un module, organisation hexagonale (skill `java-conventions`) : `domain` (modèle, ports), `application` (un cas d'usage par classe), `adapter/in/<tech>` et `adapter/out/<tech>`. Les rôles sont déclarés avec les annotations hexagonales et DDD de jMolecules (`@SecondaryPort`, `@SecondaryAdapter` sur `package-info.java`, `@AggregateRoot`) et vérifiés par `jmolecules-archunit`, plus une règle ArchUnit : `domain` et `application` ne dépendent ni de Spring (sauf `@Transactional`) ni des adapters.
- Les cas d'usage sont déclarés dans la `@Configuration` du module, sans stéréotype Spring. Chaque port a un test de contrat abstrait dans `src/test`, étendu par chaque adapter dans `src/integrationTest`.
- `shared` est un module Modulith ouvert (identifiants, `BusinessException`, `TechnicalException`) ; `config` porte le transverse (`Clock`, `UUIDGenerator`).
- Le domaine ne dépend d'aucun framework.
- Un schéma Postgres par module, aucune jointure entre schémas. Un module ne lit les données d'un autre que par son API Java ou ses événements.
- Migrations Liquibase, un changelog par module inclus par le master.

## API
- Le contrat `contracts/api/openapi.yaml` est la source de vérité : on modifie le contrat d'abord, puis le code généré (openapi-generator `interfaceOnly`) impose sa forme. Un tag par module, fichiers `$ref` par module, préfixe `/api/v1`.
- Le client du front (hey-api) et la future CLI sont générés depuis ce même contrat.
- Tout changement d'API se reflète dans la collection Bruno (`bruno/`) : requêtes régénérées par `generateBruno`, la CI échoue si la collection diffère. L'auth OAuth2 vit dans `collection.bru`, écrite à la main.
- Spring Security en resource server, jetons JWT d'un fournisseur OIDC. Aucune route anonyme. Les ressources appartiennent au `sub` du jeton ; l'accès à celle d'un autre renvoie 404.
- Erreurs en `ProblemDetail` (RFC 9457).
- Actuator sur le port de gestion 8081, seulement `health` (liveness, readiness), `info` et `prometheus`. Jamais routé par l'Ingress.
- CORS strict, validation des entrées, limites de taille des requêtes.

## Build et images
- API : Jib, base distroless `nonroot` (sinon Temurin JRE non-root) épinglée par digest. Front : Dockerfile minimal `nginx-unprivileged`, seule exception à Jib.
- Versions : bibliothèques dans `gradle/libs.versions.toml`, outils de build dans `gradle.properties`, bibliothèques gérées par le BOM Boot sans version.
- CI GitHub Actions filtrée par chemin (API ou front), avec cache Gradle.
- Étapes de la CI : `check`, e2e Playwright, scan Trivy, SBOM Syft, push sur GHCR, signature cosign keyless. PIT chaque nuit.
- Authentification de la CI à GHCR avec le jeton GitHub intégré, pas de secret longue durée.
- Tags d'images immuables (SHA du commit). Pas de tag `latest` pour un déploiement.

## Branches et versions
- `develop` est la branche par défaut et la seule déployée. Les PR ciblent `develop`.
- `main` reçoit les releases ; release-please calcule les tags `vX.Y.Z` depuis les Conventional Commits.

## Qualité
- Tests obligatoires avant fusion. Domaine testé sans Spring. Tests Spring et slices JPA sur Testcontainers Postgres, jamais H2.
- `check` inclut JaCoCo (80 %), Spotless, ErrorProne et NullAway, Modulith et jMolecules, lint et tests du front, check Bruno.
- Dépendances mises à jour par Renovate.
- Pas de secret dans le code, les tests ni les fichiers de config commités.

## Développement local
- `compose.yaml` racine : postgres, mock-oauth2-server, image Jib de l'API, image nginx du front.
- `bootRun` démarre postgres et le mock OIDC via spring-boot-docker-compose.

## CLI (plus tard)
- Si elle est en Java ou Kotlin, elle peut vivre ici. Autre langage : repo séparé.
