import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.yaml.snakeyaml.Yaml

buildscript {
    dependencies {
        classpath("org.yaml:snakeyaml:1.26")
    }
}

repositories {
    mavenCentral()
}

plugins {
    java
    maven
    id("com.google.protobuf") version "0.8.15"
    // WARNING: Versions 5.2.x onwards export java.* packages, which is not allowed in Felix OSGi Resolver!
    // See http://karaf.922171.n3.nabble.com/Manifest-import-problems-td4059042.html
    id("biz.aQute.bnd") version "5.1.2" apply false
    id("org.jetbrains.kotlin.jvm") version "1.4.31"
    id("com.github.jlouns.cpe") version "0.5.0"
    id("com.diffplug.spotless") version "5.11.0"
    id("com.github.jk1.dependency-license-report") version "1.16"
}

@Suppress("UNCHECKED_CAST")
val libraryVersions: Map<String, String> =
    Yaml().loadAs(file("${rootDir}/libraryVersions.yaml").inputStream(), Map::class.java) as Map<String, String>
extra.set("libraryVersions", libraryVersions)

licenseReport {
    configurations = arrayOf("compile", "providedByFeature", "providedByBundle")
}

allprojects {
    group = "de.fhg.aisec.ids"
    version = "4.0.0"
}

tasks.clean {
    subprojects.forEach {
        dependsOn(it.tasks.clean)
    }
}

tasks.build {
    subprojects.filter { it.name == "karaf-assembly" }.forEach {
        dependsOn(it.tasks.build)
    }
}

subprojects {
    repositories {
        mavenCentral()
//        mavenLocal()

        // References IAIS repository that contains the infomodel artifacts
        maven("https://maven.iais.fraunhofer.de/artifactory/eis-ids-public/")
    }

    apply(plugin = "biz.aQute.bnd.builder")
    apply(plugin = "java")
    apply(plugin = "maven")
    apply(plugin = "kotlin")

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    tasks.test {
        exclude("**/*IT.*")
    }

    val integrationTest = tasks.register<Test>("integrationTest") {
        include("**/*IT.*")
        systemProperty("project.version", "$project.version")
    }

    tasks.withType<Test> {
        testLogging {
            events("failed")
            exceptionFormat = TestExceptionFormat.FULL
        }
    }

    tasks.check {
        dependsOn(integrationTest)
    }

    // Configuration for dependencies that will be provided through features in the OSGi environment
    val providedByFeature by configurations.creating

    // Configurations for dependencies that will be provided through bundles in the OSGi environment
    // Separate configurations are required when two bundles depend on different versions of the same bundle!
    val providedByBundle by configurations.creating
    val unixSocketBundle by configurations.creating
    val infomodelBundle by configurations.creating

    // Configurations for bundles grouped to dedicated features apart from the main ids feature
    @Suppress("UNUSED_VARIABLE")
    val influxFeature by configurations.creating
    @Suppress("UNUSED_VARIABLE")
    val zmqFeature by configurations.creating

    // OSGi core dependencies which will just be there during runtime
    val osgiCore by configurations.creating

    // For artifacts that should be included as "compile" dependencies into published maven artifacts
    val publishCompile by configurations.creating

    configurations["compile"].extendsFrom(providedByFeature, providedByBundle, unixSocketBundle, infomodelBundle,
        osgiCore, publishCompile)

    dependencies {
        // Logging API
        providedByBundle("org.slf4j", "slf4j-simple", libraryVersions["slf4j"])

        // Needed for kotlin modules, provided at runtime via kotlin-osgi-bundle in karaf-features-ids
        compileOnly("org.jetbrains.kotlin", "kotlin-stdlib-jdk8", libraryVersions["kotlin"])
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "11"
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-Xlint:unchecked")
//        options.isDeprecation = true
    }

    tasks.jar {
        manifest {
            attributes(
                    "Bundle-Vendor" to "Fraunhofer AISEC",
                    "-noee" to true
            )
        }
    }
}

configure(subprojects.filter { it.name != "examples" }) {
    apply(plugin = "com.diffplug.spotless")

    spotless {
        isEnforceCheck = false

        kotlin {
            licenseHeader("""/*-
 * ========================LICENSE_START=================================
 * ${project.name}
 * %%
 * Copyright (C) \${"$"}YEAR Fraunhofer AISEC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */""").yearSeparator(" - ")
        }
    }
}

// Always write project version to version.txt after build/install
tasks.register<Task>("dumpVersion") {
    file(project.projectDir).resolve("version.txt").bufferedWriter().use {
        it.write(project.version.toString())
    }
}