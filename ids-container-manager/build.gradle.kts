import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

@Suppress("UNCHECKED_CAST")
val libraryVersions = rootProject.extra.get("libraryVersions") as Map<String, String>

apply(plugin = "com.google.protobuf")

val protobufGeneratedDir = "$projectDir/generated"

protobuf {
    generatedFilesBaseDir = protobufGeneratedDir
    if (findProperty("protocDownload")?.toString()?.toBoolean() != false) {
        protoc {
            artifact = "com.google.protobuf:protoc:${libraryVersions["protobuf"]}"
        }
    }
}

// Since there are no Java sources in this project except the protobuf-generated ones,
// we use this to silence output (protobuf deprecation warnings) during standard build.
tasks.withType<JavaCompile> {
    logging.captureStandardError(LogLevel.INFO)
}

tasks.clean {
    doFirst {
        delete(protobufGeneratedDir)
    }
    // Sometimes required to fix an error caused by non-existence of this folder.
    doLast {
        mkdir("${project.buildDir}/classes/kotlin/main")
    }
}

dependencies {
    implementation(project(":ids-api"))
    implementation("org.springframework.boot:spring-boot-starter")
    // Provided dependency of docker-java-api
    implementation("org.glassfish", "javax.json", libraryVersions["javaxJson"])
    // Required until our library PR has been accepted
    implementation("com.amihaiemil.web", "docker-java-api", libraryVersions["dockerJavaApi"])
    implementation("com.github.jnr", "jnr-unixsocket", libraryVersions["jnrunix"])
    implementation("com.google.protobuf", "protobuf-java", libraryVersions["protobuf"])

    testImplementation("junit", "junit", libraryVersions["junit4"])
    testImplementation("org.mockito", "mockito-core", libraryVersions["mockito"])
}
