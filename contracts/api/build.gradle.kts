// Server interfaces and models generated from the API contract, implemented by services/api.
plugins {
    id("openapi-server")
}

// Own group: services/api is also named "api", and identical coordinates would make Gradle
// resolve one project as the other.
group = "dev.ylp.contracts"

openApiServer {
    apiPackage = "dev.ylp.notes.adapter.in.rest.api"
    modelPackage = "dev.ylp.notes.adapter.in.rest.model"
}
