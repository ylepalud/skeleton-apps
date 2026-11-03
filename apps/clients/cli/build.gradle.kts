import dev.ylp.buildlogic.linkSystemSqlite

// The command-line client: Clikt commands on the shared module, one native binary per platform.
plugins {
    id("kotlin-native-cli-convention")
}

kotlin {
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.withType<org.jetbrains.kotlin.gradle.plugin.mpp.Executable>().configureEach {
            baseName = "notes"
            entryPoint = "dev.ylp.notes.cli.main"
        }
        // The local store of the shared module runs on the system's SQLite: the CLI and its tests link it.
        linkSystemSqlite()
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":apps:clients:shared"))
            implementation(libs.clikt)
            // The one-shot server that receives the sign-in redirect.
            implementation(libs.ktor.server.cio)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
