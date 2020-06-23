import com.google.protobuf.gradle.protobuf
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.yaml.snakeyaml.Yaml

@Suppress("UNCHECKED_CAST") val libraryVersions =
        Yaml().load(File("${rootDir}/libraryVersions.yaml").inputStream()) as Map<String, String>

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
    providedByBundle("com.github.microsoft", "TSS.Java", libraryVersions["tssJava"])

    providedByBundle("org.apache.logging.log4j", "log4j-core", libraryVersions["log4j"])
    providedByBundle("org.apache.logging.log4j", "log4j-slf4j-impl", libraryVersions["log4j"])

    providedByBundle("com.google.protobuf", "protobuf-java", libraryVersions["protobuf"])
    
    publishCompile("org.checkerframework", "checker-qual", libraryVersions["checkerQual"])

    providedByBundle("io.jsonwebtoken", "jjwt-impl", libraryVersions["jsonwebtoken"])
    providedByBundle("io.jsonwebtoken", "jjwt-jackson", libraryVersions["jsonwebtoken"])
    providedByBundle("io.jsonwebtoken", "jjwt-api", libraryVersions["jsonwebtoken"])
    providedByBundle("org.json", "json", libraryVersions["orgJson"])
    providedByBundle("org.bitbucket.b_c", "jose4j", libraryVersions["jose4j"])
    providedByBundle("com.squareup.okhttp3", "okhttp", libraryVersions["okhttp"])

    testImplementation("junit", "junit", libraryVersions["junit4"])
    testImplementation("org.mockito", "mockito-core", libraryVersions["mockito"])
}
