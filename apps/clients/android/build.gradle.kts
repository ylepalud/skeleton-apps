// The Android app: Compose screens and the two platform adapters of the shared module.
plugins {
    id("android-app-convention")
}

/** A setting of the app, from a Gradle property; the default targets the local stack. */
fun setting(
    name: String,
    localDefault: String,
): String = "\"${providers.gradleProperty(name).getOrElse(localDefault)}\""

android {
    namespace = "dev.ylp.notes"

    defaultConfig {
        applicationId = "dev.ylp.notes"
        // On an emulator, `adb reverse` maps these localhost ports to the machine's.
        buildConfigField("String", "API_BASE_URL", setting("mobileApiBaseUrl", "http://localhost:8088/api/v1"))
        buildConfigField("String", "OIDC_ISSUER", setting("mobileOidcIssuer", "http://localhost:8090/default"))
        buildConfigField("String", "OIDC_CLIENT_ID", setting("mobileOidcClientId", "api-mobile"))
        // A private-use scheme (RFC 8252): the reverse of a domain the project owns.
        buildConfigField("String", "OIDC_REDIRECT_URI", "\"dev.ylp.notes://callback\"")
    }
}

dependencies {
    implementation(project(":apps:clients:shared"))
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.androidx.browser)

    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.espresso.core)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.compose.ui.test.manifest)
}

kover {
    reports {
        filters {
            excludes {
                // AES-GCM in the AndroidKeyStore: only a device has one, Robolectric does not.
                classes("dev.ylp.notes.auth.KeystoreCipher")
            }
        }
    }
}
