import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

repositories {
    mavenCentral()
}

plugins {
    java
    alias(libs.plugins.springboot) apply false
    alias(libs.plugins.spring.dependencyManagement)
    alias(libs.plugins.swagger) apply false
    alias(libs.plugins.protobuf) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.plugin.spring) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.licenseReport)
    alias(libs.plugins.versions)
}

licenseReport {
    configurations = arrayOf("compile")
}

allprojects {
    group = "de.fhg.aisec.ids"
    version = "6.3.1"

    tasks.withType<DependencyUpdatesTask> {
        rejectVersionIf {
            ".*(rc-?[0-9]*|Beta)$".toRegex().matches(candidate.version)
        }
    }
}

subprojects {
    repositories {
        mavenCentral()
        mavenLocal()

        // References IAIS repository that contains the infomodel artifacts
        maven("https://maven.iais.fraunhofer.de/artifactory/eis-ids-public/")
    }

    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "com.diffplug.spotless")

    configure<DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:${rootProject.libs.versions.springBoot.get()}")
        }

        imports {
            mavenBom("org.apache.camel.springboot:camel-spring-boot-dependencies:${rootProject.libs.versions.camel.get()}")
        }
    }

    dependencies {
        // Logging API
        implementation(rootProject.libs.slf4j.api)

        // Some versions are downgraded for unknown reasons, fix this here
        val groupPins = mapOf(
            "org.jetbrains.kotlin" to mapOf(
                "*" to rootProject.libs.versions.kotlin.get()
            ),
            "com.google.guava" to mapOf(
                "guava" to rootProject.libs.versions.guava.get()
            )
        )
        // We need to explicitly specify the kotlin version for all kotlin dependencies,
        // because otherwise something (maybe a plugin) downgrades the kotlin version,
        // which produces errors in the kotlin compiler. This is really nasty.
        configurations.all {
            resolutionStrategy.eachDependency {
                groupPins[requested.group]?.let { pins ->
                    pins["*"]?.let {
                        // Pin all names when asterisk is set
                        useVersion(it)
                    } ?: pins[requested.name]?.let { pin ->
                        // Pin only for specific names given in map
                        useVersion(pin)
                    }
                }
            }
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "11"
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    // Disable time-wasting tasks
    tasks.withType<Zip> {
        if (name in setOf("distZip", "bootDistZip")) {
            enabled = false
        }
    }
    tasks.withType<Tar> {
        if (name in setOf("distTar", "bootDistTar")) {
            enabled = false
        }
    }
}

configure(subprojects.filter { it.name != "examples" }) {
    apply(plugin = "com.diffplug.spotless")

    spotless {
        kotlin {
            target("src/*/kotlin/**/*.kt")
            ktlint(libs.versions.ktlint.get())
            licenseHeader(
                """/*-
 * ========================LICENSE_START=================================
 * ${project.name}
 * %%
 * Copyright (C) ${"$"}YEAR Fraunhofer AISEC
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
 */
                """.trim()
            ).yearSeparator(" - ")
        }
    }
}

// Always write project version to version.txt after build/install
tasks.build {
    doLast {
        file(project.projectDir).resolve("version.txt").bufferedWriter().use {
            it.write(project.version.toString())
        }
    }
}
