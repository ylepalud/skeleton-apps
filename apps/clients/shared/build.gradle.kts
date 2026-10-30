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

// The iOS app reads the module as Swift: one `Shared` module, built and embedded by Xcode through
// `embedSwiftExportForXcode`. Swift export is Alpha; ADR 0027 says when to fall back to SKIE.
kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl::class)
    swiftExport {
        moduleName = "Shared"
        flattenPackage = "dev.ylp.client"
    }
}

// The bridge Kotlin generates for coroutines warns on a kotlinx.coroutines opt-in it does not
// declare. That code is not ours: only its compilation tolerates warnings.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile>().configureEach {
    if (name.startsWith("compileSwiftExport")) {
        compilerOptions.allWarningsAsErrors = false
    }
}
