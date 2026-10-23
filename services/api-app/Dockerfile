# The web app, served by an unprivileged nginx (port 8080) that also proxies /api to the API.
# Built from the bundle that `./gradlew :services:api-app:assemble` produces.
FROM nginxinc/nginx-unprivileged:1.30.6-alpine@sha256:f5116ed4606f3eb015dd4a8e28cb9b351476dc0ad4bc6e9f93b19cec130cdf03

COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY dist/api-app/browser /usr/share/nginx/html

# The base image already runs as nginx (uid 101); stated here so it never silently changes.
USER 101
EXPOSE 8080
