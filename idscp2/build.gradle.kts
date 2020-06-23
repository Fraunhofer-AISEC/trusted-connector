import com.google.protobuf.gradle.protobuf
import org.gradle.plugins.ide.idea.model.IdeaModel

@Suppress("UNCHECKED_CAST") val libraryVersions =
        rootProject.ext.get("libraryVersions") as Map<String, String>

version = libraryVersions["idscp2"] ?: error("IDSCP2 version not specified")

apply(plugin = "com.google.protobuf")
apply(plugin = "idea")

val generatedProtoBaseDir = "${projectDir}/generated"

protobuf {
    generatedFilesBaseDir = generatedProtoBaseDir
}

tasks.named("clean") {
    doLast {
        delete(generatedProtoBaseDir)
    }
}

configure<IdeaModel> {
    module {
        // mark as generated sources for IDEA
        generatedSourceDirs.add(File("${generatedProtoBaseDir}/main/java"))
    }
}

dependencies {
    providedByBundle(project(":ids-api")) { isTransitive = false }

    providedByBundle("com.github.microsoft", "TSS.Java", libraryVersions["tssJava"])

    providedByBundle("com.google.protobuf", "protobuf-java", libraryVersions["protobuf"])
    
    publishCompile("org.checkerframework", "checker-qual", libraryVersions["checkerQual"])

    implementation("io.jsonwebtoken", "jjwt-impl", libraryVersions["jsonwebtoken"])
    implementation("io.jsonwebtoken", "jjwt-jackson", libraryVersions["jsonwebtoken"])
    implementation("io.jsonwebtoken", "jjwt-api", libraryVersions["jsonwebtoken"])
    providedByBundle("org.json", "json", libraryVersions["orgJson"])
    providedByBundle("org.bitbucket.b_c", "jose4j", libraryVersions["jose4j"])
    providedByBundle("com.squareup.okhttp3", "okhttp", libraryVersions["okhttp"])

    testImplementation("junit", "junit", libraryVersions["junit4"])
    testImplementation("org.mockito", "mockito-core", libraryVersions["mockito"])
}
