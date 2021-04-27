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

tasks.clean {
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
    providedByBundle(project(":ids-api")) { isTransitive = false }

    implementation("org.springframework.boot:spring-boot-starter")

    // Provided dependency of docker-java-api
    providedByBundle("org.glassfish", "javax.json", libraryVersions["javaxJson"])
    // Required until our library PR has been accepted
    providedByBundle("com.amihaiemil.web", "docker-java-api", libraryVersions["dockerJavaApi"]) {
        exclude("com.github.jnr", "jnr-unixsocket")
    }

    //implementation(project(":jnr-unixsocket-wrapper"))
    implementation("com.github.jnr", "jnr-unixsocket", libraryVersions["jnrunix"])
    implementation("com.github.jnr", "jnr-ffi", libraryVersions["jnrffi"])

    providedByBundle("com.google.protobuf", "protobuf-java", libraryVersions["protobuf"])

    osgiCore("org.osgi", "osgi.cmpn", libraryVersions["osgiCompendium"])

    testImplementation("junit", "junit", libraryVersions["junit4"])
    testImplementation("org.mockito", "mockito-core", libraryVersions["mockito"])
}
