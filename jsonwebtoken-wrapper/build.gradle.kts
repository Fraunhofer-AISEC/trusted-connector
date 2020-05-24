@Suppress("UNCHECKED_CAST") val libraryVersions =
        rootProject.ext.get("libraryVersions") as Map<String, String>

dependencies {
    providedByBundle("io.jsonwebtoken", "jjwt-impl", libraryVersions["jsonwebtoken"])
    providedByBundle("io.jsonwebtoken", "jjwt-jackson", libraryVersions["jsonwebtoken"])
    providedByBundle("io.jsonwebtoken", "jjwt-api", libraryVersions["jsonwebtoken"])
}
