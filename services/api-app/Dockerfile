# The web app, served by an unprivileged nginx (port 8080) that also proxies /api to the API.
# Built from the bundle that `./gradlew :services:api-app:assemble` produces.
FROM nginxinc/nginx-unprivileged:1.31.7-alpine@sha256:b5047b9b76845eac124358373c6852377c52e4b05834a47647e955dd91cb8f1e

# The base image is rebuilt upstream only for nginx releases: pull the Alpine security
# fixes published since, so the image scan does not fail on an already fixed package.
USER root
RUN apk upgrade --no-cache

COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY dist/api-app/browser /usr/share/nginx/html

# The base image already runs as nginx (uid 101); stated here so it never silently changes.
USER 101
EXPOSE 8080
