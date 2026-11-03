package dev.ylp.notes.cli

// mode_t is 16 bits on macOS and 32 on Linux: each platform calls these with its own.

/** Creates the directory [path], usable by the owner only; 0 on success, as mkdir. */
internal expect fun makePrivateDirectory(path: String): Int

/** Makes the open file [descriptor] readable and writable by the owner only; 0 on success, as fchmod. */
internal expect fun makePrivateFile(descriptor: Int): Int
