package dev.ylp.client

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test

/**
 * The hexagon of the shared module, as executable rules: the domain and the use cases know
 * neither the transport nor the adapters, and the public API stays within what Swift export
 * supports.
 */
class ArchitectureTest {
    private val production = Konsist.scopeFromSourceSet("commonMain", "clients/shared")

    @Test
    fun `the domain and the use cases depend on no adapter, transport or generated code`() {
        production.files
            .filter {
                it.packagee
                    ?.name
                    .orEmpty()
                    .let { name -> ".domain" in name || ".application" in name }
            }.assertFalse { file ->
                file.imports.any { import ->
                    import.name.startsWith("io.ktor") || ".adapter." in import.name || ".generated." in import.name
                }
            }
    }

    @Test
    fun `the domain depends on no use case`() {
        production.files
            .filter { ".domain" in it.packagee?.name.orEmpty() }
            .assertFalse { file -> file.imports.any { ".application." in it.name } }
    }

    @Test
    fun `the generated client is reached by the adapters and the composition root only`() {
        production.files
            .filter { file -> file.imports.any { ".generated." in it.name } }
            .assertTrue { it.packagee?.name == "dev.ylp.client" || ".adapter." in it.packagee?.name.orEmpty() }
    }

    @Test
    fun `the public API declares no generic type or function, which Swift export does not support`() {
        val classes = production.classes().filterNot { it.hasInternalModifier || it.hasPrivateModifier }
        val interfaces = production.interfaces().filterNot { it.hasInternalModifier || it.hasPrivateModifier }
        classes.assertFalse { it.hasTypeParameters() }
        interfaces.assertFalse { it.hasTypeParameters() }
        (classes.flatMap { it.functions() } + interfaces.flatMap { it.functions() })
            .filterNot { it.hasInternalModifier || it.hasPrivateModifier }
            .assertFalse { it.hasTypeParameters() }
    }

    @Test
    fun `the adapters are internal, except the token store the platforms may reuse`() {
        production
            .classes(includeNested = false)
            .filter { ".adapter." in it.packagee?.name.orEmpty() && it.name != "InMemoryTokenStore" }
            .assertTrue { it.hasInternalModifier }
    }
}
