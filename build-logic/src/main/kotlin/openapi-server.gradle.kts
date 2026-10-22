/*
 * A library generated from an OpenAPI contract: the Spring MVC interfaces and
 * models that the application's controllers implement. The contract is the
 * source of truth, so the application stops compiling as soon as a controller
 * drifts from it.
 *
 * Living in its own project keeps generated code out of the application's
 * coverage and mutation testing, and packages it as a plain jar.
 */
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    id("java-convention")
    `java-library`
    id("org.openapi.generator")
}

interface OpenApiServerExtension {
    /** Entry file of the contract; the files it references sit next to it. */
    val contract: RegularFileProperty

    /** Package of the generated interfaces. */
    val apiPackage: Property<String>

    /** Package of the generated models. */
    val modelPackage: Property<String>
}

val openApiServer = extensions.create<OpenApiServerExtension>("openApiServer")
openApiServer.contract.convention(layout.projectDirectory.file("openapi.yaml"))

val libs = the<VersionCatalogsExtension>().named("libs")

// What the generated code compiles against, exposed to the application.
dependencies {
    api(platform(libs.findLibrary("spring-boot-dependencies").get()))
    api("org.springframework:spring-web")
    api("org.springframework:spring-context")
    api("jakarta.validation:jakarta.validation-api")
    api("jakarta.annotation:jakarta.annotation-api")
    api("com.fasterxml.jackson.core:jackson-annotations")
    // Provided by the servlet container at runtime.
    compileOnly("jakarta.servlet:jakarta.servlet-api")
}

val generatedDir = layout.buildDirectory.dir("generated/openapi")

val openApiGenerate =
    tasks.named<GenerateTask>("openApiGenerate") {
        generatorName = "spring"
        inputSpec = openApiServer.contract
        outputDir = generatedDir
        apiPackage.set(openApiServer.apiPackage)
        modelPackage.set(openApiServer.modelPackage)
        // Only the interfaces and models: no application, tests or documentation.
        generateApiTests = false
        generateModelTests = false
        generateApiDocumentation = false
        generateModelDocumentation = false
        configOptions =
            mapOf(
                "interfaceOnly" to "true",
                "useTags" to "true",
                "skipDefaultInterface" to "true",
                "useSpringBoot4" to "true",
                "useJspecify" to "true",
                "useResponseEntity" to "true",
                "useBeanValidation" to "true",
                "openApiNullable" to "false",
                "documentationProvider" to "none",
                "annotationLibrary" to "none",
            )
        // The entry file references sibling files: any change in the folder regenerates.
        inputs.dir(openApiServer.contract.map { it.asFile.parentFile })
    }

sourceSets.named("main") {
    java.srcDir(files(generatedDir.map { it.dir("src/main/java") }).builtBy(openApiGenerate))
}
