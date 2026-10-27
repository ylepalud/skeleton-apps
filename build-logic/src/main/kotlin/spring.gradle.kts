/*
 * A deployable Spring Boot application: a Spring Modulith of modules exposing a
 * secured REST API, JPA persistence with Liquibase migrations, and the shared
 * quality gates. Tests run against a real PostgreSQL through Testcontainers.
 *
 * The BOMs are imported as native Gradle platforms, so only libraries they
 * manage are declared here without a version.
 */
plugins {
    id("quality-gates")
    id("service-isolation")
    id("org.springframework.boot")
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    val platforms =
        listOf("spring-boot-dependencies", "spring-modulith-bom", "jmolecules-bom")
            .map { platform(libs.findLibrary(it).get()) }
    platforms.forEach {
        implementation(it)
        developmentOnly(it)
        testAndDevelopmentOnly(it)
    }

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    implementation("org.jmolecules:jmolecules-ddd")
    implementation("org.jmolecules:jmolecules-hexagonal-architecture")

    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testImplementation("org.jmolecules.integrations:jmolecules-archunit")

    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
}

/*
 * `bootRun` targets local development, where the `local` profile starts the
 * application's Compose services. An explicitly requested profile always wins.
 */
tasks.named<JavaExec>("bootRun") {
    val requestedProfiles =
        providers
            .systemProperty("spring.profiles.active")
            .orElse(providers.environmentVariable("SPRING_PROFILES_ACTIVE"))
            .orElse("local")
    systemProperty("spring.profiles.active", requestedProfiles.get())
}
