// Server interfaces and models generated from the API contract, implemented by services/api.
plugins {
    id("openapi-server")
}

openApiServer {
    apiPackage = "dev.ylp.notes.adapter.in.rest.api"
    modelPackage = "dev.ylp.notes.adapter.in.rest.model"
}
