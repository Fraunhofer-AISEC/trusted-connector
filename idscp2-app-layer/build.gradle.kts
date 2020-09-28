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
    providedByBundle(project(":idscp2")) { isTransitive = false }

    providedByBundle("com.google.protobuf", "protobuf-java", libraryVersions["protobuf"])
}
