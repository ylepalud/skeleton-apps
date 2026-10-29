// The logic of the native clients: API client, sign-in and use cases. The apps implement the UI
// and the platform adapters only.
plugins {
    id("openapi-kotlin-client")
}

openApiClient {
    contract = layout.projectDirectory.file("../../contracts/api/openapi.yaml")
    packageName = "dev.ylp.client.generated"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.auth)
            // PKCE: SHA-256 and a cryptographically secure random source on every platform.
            implementation(libs.kotlincrypto.sha2)
            implementation(libs.kotlincrypto.random)
        }
        jvmTest.dependencies {
            implementation(libs.konsist)
        }
    }
}
