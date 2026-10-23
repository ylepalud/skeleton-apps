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
- `./gradlew pitest` : mutation testing, hors `check`, seuil 80 % sur `domain` et `application` (lancé chaque nuit en CI).
- Le daemon Gradle tourne sur Temurin 25 (`gradle/gradle-daemon-jvm.properties`), téléchargé par foojay si absent. Toute modification d'un convention plugin s'accompagne d'un test dans `build-logic/src/functionalTest`.

## Front (`services/api-app`)
- Angular 22 zoneless, Tailwind 4, ESLint `strict`, Vitest + Testing Library, Playwright. Construit par Gradle (`frontend-convention`, Node épinglé, `npm ci`) : `lint` et `test` dans `check`, `build` dans `assemble`.
- Client HTTP généré depuis `contracts/api` (hey-api, `@hey-api/client-angular`) dans `src/app/api/generated`, non commité. Il passe par `HttpClient` : l'intercepteur OIDC ajoute le jeton, uniquement vers `/api/`.
- Connexion Authorization Code + PKCE (angular-auth-oidc-client), renouvellement par refresh token. Réglages lus à l'exécution dans `/config.json` (`authority`, `clientId`) : `public/config.json` en dev, généré par nginx dans l'image.
- Dev : `npm start` (`ng serve`, proxy `/api` vers `localhost:8080`) avec l'API en `bootRun`. E2E : `./gradlew :services:api-app:npmRunE2e -PrunE2e=true`, `E2E_BASE_URL` pour viser une autre stack ; toute erreur de console fait échouer un test.
- Image : `./gradlew :services:api-app:dockerBuild` (`apps/api-app:local`), nginx-unprivileged sur 8080. Variables : `API_UPSTREAM`, `OIDC_AUTHORITY`, `OIDC_CLIENT_ID`, `OIDC_ORIGIN` (CSP).
- `overrides` npm : js-yaml et vite forcés en versions corrigées ; à retirer quand Angular et hey-api les embarquent.

## Architecture
- Spring Modulith obligatoire : un module applicatif = un package de premier niveau sous `dev.ylp`. `ApplicationModules.verify()` doit passer.
- Dans un module, organisation hexagonale (skill `java-conventions`) : `domain` (modèle, ports), `application` (un cas d'usage par classe), `adapter/in/<tech>` et `adapter/out/<tech>`. Les rôles sont déclarés avec les annotations hexagonales et DDD de jMolecules (`@SecondaryPort`, `@SecondaryAdapter` sur `package-info.java`, `@AggregateRoot`) et vérifiés par `jmolecules-archunit`, plus une règle ArchUnit : `domain` et `application` ne dépendent ni de Spring (sauf `@Transactional`) ni des adapters.
- Les cas d'usage sont déclarés dans la `@Configuration` du module, sans stéréotype Spring. Chaque port a un test de contrat abstrait dans `src/test`, étendu par chaque adapter dans `src/integrationTest`.
- `shared` est un module Modulith ouvert (identifiants, `BusinessException`, `TechnicalException`) ; `config` porte le transverse (`Clock`, `UUIDGenerator`).
- Le domaine ne dépend d'aucun framework.
- Un schéma Postgres par module, aucune jointure entre schémas. Un module ne lit les données d'un autre que par son API Java ou ses événements.
- Migrations Liquibase, un changelog par module inclus par le master.

## API
- Le contrat `contracts/api/` est la source de vérité : on modifie le contrat d'abord. `openapi.yaml` est le point d'entrée, `common.yaml` porte `Problem` et les réponses partagées, un fichier par module (`notes.yaml`) avec son tag. Le projet Gradle `:contracts:api` (convention `openapi-server`) en génère les interfaces et les DTO (suffixe `Dto`) que les controllers implémentent : la compilation casse si le code s'écarte du contrat. Le préfixe `/api/v1` du `servers` est appliqué aux `@RestController` par `ApiPathConfiguration`.
- Un deuxième module demandera une génération par tag (normalizer `FILTER`), pour que chaque module possède ses DTO.
- Le client du front (hey-api) et la future CLI sont générés depuis ce même contrat.
- Tout changement d'API se reflète dans la collection Bruno (`bruno/`, convention `bruno-collection`) : après une modification du contrat, `./gradlew :bruno:updateCollection` et commiter ; `check` échoue sinon (`verifyCollection`). Seuls les dossiers de requêtes (`notes/`, un par tag) sont générés ; `opencollection.yml` (auth OAuth2 `client_credentials`, test commun) et `environments/` (`Stack`, `BootRun`) sont écrits à la main. Donner des `example` aux corps de requête du contrat, sinon la requête générée part vide.
- Spring Security en resource server, jetons JWT d'un fournisseur OIDC, vérifiés sur l'émetteur et l'audience `api`. Aucune route anonyme. Les ressources appartiennent au `sub` du jeton, lu uniquement par `CurrentOwner` ; l'accès à celle d'un autre renvoie 404.
- Erreurs en `ProblemDetail` (RFC 9457), avec `errorCode` et `hint`. Une raison métier déclare sa `category()` (`NOT_FOUND`, `INVALID`, `CONFLICT`, `RULE_VIOLATION` par défaut), sans rien savoir de HTTP ; `ProblemDetailsExceptionHandler` la traduit en statut. Messages en anglais (locale fixe).
- Corps de requête limité à 64 Kio (`RequestSizeLimitFilter`, 413).
- Actuator sur le port de gestion 8081, seulement `health` (liveness, readiness), `info` et `prometheus`. Jamais routé par l'Ingress.
- CORS strict, validation des entrées, limites de taille des requêtes.

## Build et images
- API : Jib, base distroless `nonroot` (sinon Temurin JRE non-root) épinglée par digest. Front : Dockerfile minimal `nginx-unprivileged`, seule exception à Jib.
- Versions : bibliothèques dans `gradle/libs.versions.toml`, outils de build dans `gradle.properties`, bibliothèques gérées par le BOM Boot sans version.
- Dépôt public `github.com/ylepalud/skeletton-apps`, marqué *template* : point de départ des futurs projets (« Use this template »).
- CI GitHub Actions (`.github/workflows`), actions épinglées par SHA de commit, permissions minimales :
  - `ci.yml` (PR vers `develop`/`main`, push sur `main`) : jobs `check`, `e2e` (stack complète, Playwright, smoke Bruno) et `trivy-fs` (dépendances, dev comprises, secrets, configuration ; SARIF dans l'onglet Security). Ces trois checks sont exigés par les rulesets.
  - `publish.yml` (push sur `develop`) : `ci.yml`, puis pour chaque image dont le code a changé, push `ghcr.io/ylepalud/skeletton-apps/<api|api-app>:<sha>`, scan Trivy, SBOM SPDX attesté et signature cosign keyless (`.github/actions/secure-image`). Signée seulement si le scan passe. `workflow_dispatch` republie tout.
  - `nightly.yml` : PIT à 80 %. `release.yml` : release-please sur `main`, puis tag `vX.Y.Z` posé sur les images déjà publiées (pas de reconstruction).
- Authentification de la CI à GHCR avec le jeton GitHub intégré, pas de secret longue durée.
- Tags d'images immuables (SHA du commit). Pas de tag `latest` pour un déploiement.

## Branches et versions
- `develop` est la branche par défaut et la seule déployée. Les PR ciblent `develop`.
- `main` reçoit les releases : PR `develop` vers `main` fusionnée par *merge commit* (les PR vers `develop` sont en *squash*), puis release-please ouvre la PR de release qui pose le tag `vX.Y.Z`.
- La PR de release de release-please est créée avec le `GITHUB_TOKEN` : elle ne déclenche aucun workflow, donc aucun check, et le ruleset de `main` la bloque. Elle ne touche que `CHANGELOG.md` et le manifeste : la fusionner en contournement admin, `gh pr merge <n> --merge --admin`. Évolution possible : un jeton de GitHub App pour release-please, qui ferait passer la CI sur ses PR.
- Une PR qui ne touche que `.github/` ou la config ne publie aucune image (filtre par chemin) : `gh workflow run publish.yml --ref develop -f all=true` republie tout.

## Qualité
- Tests obligatoires avant fusion. Domaine testé sans Spring. Tests Spring et slices JPA sur Testcontainers Postgres, jamais H2.
- `check` inclut JaCoCo (80 %), Spotless, ErrorProne et NullAway, Modulith et jMolecules, lint et tests du front, check Bruno.
- Dépendances mises à jour par Renovate (`renovate.json`, application GitHub Mend Renovate à installer sur le dépôt) : 7 jours d'ancienneté minimum, fusion auto des patch, minor et digests si la CI passe, sauf Spring, Angular, PostgreSQL et les majeures. Les versions de `gradle.properties` portent un commentaire `# renovate:`.
- Pas de secret dans le code, les tests ni les fichiers de config commités.

## Développement local
- Un seul `compose.yaml` à la racine. Par défaut : postgres et mock-oauth2-server (8090). Profil `app` : l'API (image Jib `apps/api:local`, probes sur 8091) et le front (image nginx `apps/api-app:local`, http://localhost:8088, `/api` vers l'API).
- `./gradlew startLocalStack` construit les deux images, démarre tout et attend la readiness ; relancer après un changement ne recrée que les conteneurs dont l'image a changé. `stopLocalStack` garde la base, `resetDatabase` la supprime.
- `./gradlew :services:api:bootRun` démarre postgres et le mock à partir du même compose (sans le profil `app`). Ne pas lancer l'API en `bootRun` et en conteneur en même temps.
- Dans le conteneur, l'API vérifie l'émetteur `http://localhost:8090/default` (celui des jetons du navigateur) mais lit les clés à `http://mock-oauth2-server:8080/default/jwks` : `localhost` n'y désigne pas l'hôte.
- Bruno contre la stack : `cd bruno && npm run smoke`. E2E contre la stack : `E2E_BASE_URL=http://localhost:8088 ./gradlew :services:api-app:npmRunE2e -PrunE2e=true`.
- Jeton local : `curl -X POST localhost:8090/default/token -d grant_type=client_credentials -d client_id=<user> -d client_secret=x` (le `sub` vaut le `client_id`).
- Les tâches qui appellent docker le résolvent depuis le `PATH` de l'appelant (`dev.ylp.buildlogic.dockerExecutable`) : un daemon Gradle peut avoir un `PATH` minimal.

## CLI (plus tard)
- Si elle est en Java ou Kotlin, elle peut vivre ici. Autre langage : repo séparé.
