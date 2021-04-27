@Suppress("UNCHECKED_CAST")
val libraryVersions = rootProject.extra.get("libraryVersions") as Map<String, String>

dependencies {
    implementation("com.github.jnr", "jnr-unixsocket", libraryVersions["jnrunix"])
    // Only jnr-ffi is provided by bundle, because it contains native libraries
    // Versions above 2.1.5 fail with a wiring exception for package com.github.jnr.jffi.native
    unixSocketBundle("com.github.jnr", "jnr-ffi", libraryVersions["jnrffi"])
}