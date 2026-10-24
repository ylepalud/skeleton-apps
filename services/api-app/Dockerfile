# The web app, served by an unprivileged nginx (port 8080) that also proxies /api to the API.
# Built from the bundle that `./gradlew :services:api-app:assemble` produces.
FROM nginxinc/nginx-unprivileged:1.31.6-alpine@sha256:884e0b7f42f303641b0f4e78755aea647521dca0b89ca3be7992ad78b5c07434

COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY dist/api-app/browser /usr/share/nginx/html

# The base image already runs as nginx (uid 101); stated here so it never silently changes.
USER 101
EXPOSE 8080
