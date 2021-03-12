@file:Suppress("PropertyName")

import com.github.jlouns.gradle.cpe.tasks.CrossPlatformExec
import java.util.*

@Suppress("UNCHECKED_CAST")
val libraryVersions = rootProject.extra.get("libraryVersions") as Map<String, String>

dependencies {
    testImplementation(project(":ids-api")) { isTransitive = false }

    osgiCore("org.apache.felix", "org.apache.felix.framework", libraryVersions["felixFramework"])
    osgiCore("org.osgi", "osgi.cmpn", libraryVersions["osgiCompendium"])

    testImplementation("org.ops4j.pax.exam", "pax-exam", libraryVersions["paxExam"])
    testImplementation("org.ops4j.pax.exam", "pax-exam-junit4", libraryVersions["paxExam"])
    testImplementation("org.ops4j.pax.exam", "pax-exam-container-karaf", libraryVersions["paxExam"])
    testImplementation("org.apache.karaf.itests", "common", libraryVersions["karaf"])

    testImplementation("org.apache.karaf", "apache-karaf", libraryVersions["karaf"], ext = "pom")
    testImplementation("org.awaitility", "awaitility", libraryVersions["awaitility"])
    testImplementation("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.hamcrest",
        libraryVersions["servicemixHamcrest"])
    testImplementation("javax.annotation", "javax.annotation-api", libraryVersions["javaxAnnotation"])
}

val BRANDING_WIDTH = 84

fun getBrandingAligned(branding: String, space: String = "\\u0020"): String {
    val sb = StringBuilder()
    val spaces = (BRANDING_WIDTH - branding.length) / 2
    for (i in 0 until spaces) {
        sb.append(space)
    }
    sb.append(branding)
    return sb.toString()
}

val parsePom = tasks.register<Copy>("parsePom") {
    from(project.projectDir) {
        include("pom.template.xml")
    }
    expand(
        mapOf(
            "projectVersion" to project.version as String,
            "karafVersion" to libraryVersions["karaf"],
            "paxVersion" to libraryVersions["pax"],
            "brandingFirst" to getBrandingAligned(
                "Trusted Connector Console (${project.version}), Apache Karaf (${libraryVersions["karaf"]})"
            ),
            "brandingSecond" to getBrandingAligned(
                "Fraunhofer AISEC ${Calendar.getInstance().get(Calendar.YEAR)}"
            )
        )
    )
    rename("pom.template.xml", "pom.xml")
    into(project.projectDir)

    inputs.property("projectVersion", project.version)
    inputs.property("karafVersion", libraryVersions["karaf"])
}

/*
Now this is tricky. We need to build a custom distribution of karaf with a few features:
- included ids feature
- a bunch of configuration files in etc

Since gradle still has no karaf-assembly plugin we need to do this using maven (meh!)
*/
val assembleKaraf by tasks.registering(CrossPlatformExec::class) {
    commandLine(listOf("./mvnw", "--no-transfer-progress", "clean", "package"))
    doLast {
        mkdir("${project.buildDir}/classes/kotlin/test")
    }
    dependsOn(parsePom)
    // Wait for all relevant sub projects before executing assembly process
    rootProject.subprojects.forEach {
        if (it.name.startsWith("ids") || it.name.startsWith("camel-")
            || it.name.endsWith("-patch") || it.name.endsWith("-wrapper")
            || it.name == "karaf-features-ids"
        ) {
            dependsOn(it.tasks.named("install"))
        }
    }
}
// Sometimes required to fix an error caused by a non-existing folder (maybe caused by mvn clean)
mkdir("${project.buildDir}/classes/kotlin/test")

tasks.named("jar") {
    dependsOn(assembleKaraf)
}

// The PaxExam config of KarafTestSupport requires the maven dependency meta information generated here
val makeMavenDependencies by tasks.registering {
    val outputFileDir = project.file("build/classes/java/test/META-INF/maven/")
    val outputFile = file(outputFileDir).resolve("dependencies.properties")
    outputs.file(outputFile)

    doFirst {
        val properties = Properties()

        // information of the project itself
        properties.setProperty("groupId", "${project.group}")
        properties.setProperty("artifactId", project.name)
        properties.setProperty("version", "${project.version}")
        properties.setProperty("${project.group}/${project.name}/version", "${project.version}")

        // information of all test runtime dependencies
        project.configurations.testCompileClasspath.get().resolvedConfiguration.resolvedArtifacts.forEach {
            val keyBase = it.moduleVersion.id.group + "/" + it.moduleVersion.id.name
            properties.setProperty("${keyBase}/scope", "compile")
            properties.setProperty("${keyBase}/type", it.extension)
            properties.setProperty("${keyBase}/version", it.moduleVersion.id.version)
        }

        outputFileDir.mkdirs()
        outputFile.outputStream().use {
            properties.store(it, "Generated from Gradle for PaxExam integration tests")
        }
    }
}

tasks.named("integrationTest") {
    dependsOn(makeMavenDependencies)
    dependsOn(assembleKaraf)
    outputs.upToDateWhen { false }
}
