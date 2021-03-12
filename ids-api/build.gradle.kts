import com.google.protobuf.gradle.protobuf
import org.gradle.plugins.ide.idea.model.IdeaModel

@Suppress("UNCHECKED_CAST")
val libraryVersions = rootProject.extra.get("libraryVersions") as Map<String, String>

apply(plugin = "com.google.protobuf")
apply(plugin = "idea")

val protobufGeneratedDir = "$projectDir/generated"

protobuf {
    generatedFilesBaseDir = protobufGeneratedDir
}

tasks.named("clean") {
    doFirst {
        delete(protobufGeneratedDir)
    }
    // Sometimes required to fix an error caused by a non-existing folder
    doLast {
        mkdir("${project.buildDir}/classes/kotlin/main")
    }
}

configure<IdeaModel> {
    module {
        // mark as generated sources for IDEA
        generatedSourceDirs.add(File("${protobufGeneratedDir}/main/java"))
    }
}

dependencies {
    providedByBundle("com.google.protobuf", "protobuf-java", libraryVersions["protobuf"])

    providedByBundle("com.fasterxml.jackson.core", "jackson-annotations", libraryVersions["jackson"])

    publishCompile("org.checkerframework", "checker-qual", libraryVersions["checkerQual"])

    // Actual implementation must be provided by ids-infomodel-manager
    publishCompile("de.fraunhofer.iais.eis.ids.infomodel", "java", libraryVersions["infomodel"])

    publishCompile("org.apache.camel", "camel-core", libraryVersions["camel"])

    testImplementation("junit", "junit", libraryVersions["junit4"])
    testImplementation("org.mockito", "mockito-core", libraryVersions["mockito"])
}
