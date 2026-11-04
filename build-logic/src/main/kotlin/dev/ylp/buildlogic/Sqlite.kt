package dev.ylp.buildlogic

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family

/**
 * Links every binary of this target against the system's SQLite, which the shared module's local
 * store runs on. Apple systems ship it. On Linux, Kotlin/Native links in its own sysroot: the
 * runtime library, present on usual distributions, is named from the system's folder, so that no
 * development package is needed; the library's own dependencies resolve at run time.
 */
fun KotlinNativeTarget.linkSystemSqlite() {
    val options =
        when (konanTarget.family) {
            Family.LINUX -> listOf("-L/usr/lib/x86_64-linux-gnu", "-L/usr/lib64", "-l:libsqlite3.so.0", "--allow-shlib-undefined")
            else -> listOf("-lsqlite3")
        }
    binaries.configureEach { linkerOpts(options) }
}
