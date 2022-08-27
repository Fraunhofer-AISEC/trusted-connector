import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

apply(plugin = "com.google.protobuf")

protobuf {
    if (findProperty("protocDownload")?.toString()?.toBoolean() != false) {
        protoc {
            artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
        }
    }
}

tasks.clean {
    // Sometimes required to fix an error caused by non-existence of this folder.
    doLast {
        mkdir("${project.buildDir}/classes/kotlin/main")
    }
}

dependencies {
    implementation(project(":ids-api"))
    implementation("org.springframework.boot:spring-boot-starter")
    // Provided dependency of docker-java-api
    implementation(libs.javax.json)
    implementation(libs.dockerJavaApi)
    implementation(libs.jnrunix)
    implementation(libs.protobuf)

    testImplementation(libs.bundles.test4)
}
