import dev.ylp.buildlogic.linkSystemSqlite

// The logic of the native clients: API client, sign-in and use cases. The apps implement the UI
// and the platform adapters only.
plugins {
    id("openapi-kotlin-client")
    id("app.cash.sqldelight")
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
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
        }
        nativeMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
        // The common tests run on an in-memory database: JDBC SQLite on the JVM and the Android
        // host, the native driver on iOS, macOS and Linux.
        jvmMain.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
        }
        jvmTest.dependencies {
            implementation(libs.konsist)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.sqldelight.sqlite.driver)
        }
    }
}

// The local store of the offline mode: notes and the queue of changes to replay (ADR 0029).
sqldelight {
    // Its -lsqlite3 does not reach Linux's system folder: linkSystemSqlite below links instead.
    linkSqlite = false
    databases {
        create("ClientDatabase") {
            packageName = "dev.ylp.client.store.db"
        }
    }
}

kotlin {
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach { linkSystemSqlite() }
}

// SQLDelight writes public declarations, generic ones among them. The store is an adapter: its
// generated code is made internal, out of the API that Swift export publishes.
tasks.withType<app.cash.sqldelight.gradle.SqlDelightTask>().configureEach {
    val generated = outputDirectory
    doLast {
        val topLevelPublic = Regex("^public ", RegexOption.MULTILINE)
        generated.get().asFile.walk().filter { it.extension == "kt" }.forEach { file ->
            file.writeText(file.readText().replace(topLevelPublic, "internal "))
        }
    }
}

kover {
    reports {
        filters {
            excludes {
                packages("dev.ylp.client.store.db")
            }
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
