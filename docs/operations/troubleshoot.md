# Troubleshoot

## The build fails in strange ways

Symptoms: a convention plugin class "not found", `node` or `jib` unresolved in `build-logic`, files missing from `build-logic/build`, while the code has not changed.

Cause: another Gradle daemon builds the same project at the same time, typically an IDE's (the VS Code Java extension, IntelliJ). Concurrent builds corrupt the outputs, and corrupted outputs can poison the local build cache.

Fix: close the IDE's Gradle sync or the IDE while building from the command line, then:

```bash
./gradlew --stop
rm -rf build-logic/build .gradle/configuration-cache
rm -rf ~/.gradle/caches/build-cache-1   # only if the problem comes back
```

## docker is not found

Tasks that run docker resolve it from the `PATH` of the command that started the build. If it still fails, check that `docker` is on your `PATH` (`which docker`) and that the daemon runs (`docker info`).

## A port is already in use

The stack publishes 8088, 8090 and 8091; `bootRun` uses 8080 and 8081. Another project bound to one of them wins. Find it with `docker ps --format '{{.Names}} {{.Ports}}'` or `lsof -i :<port>`. From inside a container, `host.docker.internal:<port>` reaches whatever the host publishes on that port, which may not be what you expect.

## Gradle cannot download (behind a proxy)

A global `~/.gradle/gradle.properties` may set an HTTP proxy that is unreachable from where you are. Bypass it for one build:

```bash
./gradlew -Dhttp.nonProxyHosts='*' check
```

## bootRun started without the identity provider

Spring Boot does not start compose services when some of them already run. Start them all, then run again:

```bash
docker compose up -d --wait
```

## The web app signs in but the API answers 401

The token's issuer or audience does not match what the API expects. Decode the token (its middle part is base64 JSON) and compare `iss` with the configured issuer and `aud` with `api`. In the local stack, the issuer must be `http://localhost:8090/default`.
