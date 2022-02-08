import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.yaml.snakeyaml.Yaml

buildscript {
    dependencies {
        classpath("org.yaml:snakeyaml:1.29")
    }
}

repositories {
    mavenCentral()
}

plugins {
    java

    // Spring Boot
    id("org.springframework.boot") version "2.5.8" apply false
    id("io.spring.dependency-management") version "1.0.11.RELEASE"

    // Other needed plugins
    id("com.moowork.node") version "1.3.1" apply false
    // Latest version compiled with Java 11
    id("com.benjaminsproule.swagger") version "1.0.8" apply false

    // Protobuf
    id("com.google.protobuf") version "0.8.17" apply false

    // Kotlin specific
    kotlin("jvm") version "1.6.10" apply false
    kotlin("plugin.spring") version "1.6.10" apply false

    id("com.diffplug.spotless") version "5.11.0"
    id("com.github.jk1.dependency-license-report") version "1.16"
}

@Suppress("UNCHECKED_CAST")
val libraryVersions: Map<String, String> =
    Yaml().loadAs(file("$rootDir/libraryVersions.yaml").inputStream(), Map::class.java) as Map<String, String>
extra.set("libraryVersions", libraryVersions)

licenseReport {
    configurations = arrayOf("compile")
}

allprojects {
    group = "de.fhg.aisec.ids"
    version = "6.1.0"
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
            mavenBom("org.springframework.boot:spring-boot-dependencies:${libraryVersions["springBoot"]}")
        }

        imports {
            mavenBom("org.apache.camel.springboot:camel-spring-boot-dependencies:${libraryVersions["camel"]}")
        }
    }

    dependencies {
        // Logging API
        implementation("org.slf4j", "slf4j-api", libraryVersions["slf4j"])

        // Some versions are downgraded for unknown reasons, fix this here
        val groupPins = mapOf(
            "org.jetbrains.kotlin" to mapOf(
                "*" to "kotlin"
            ),
            "com.squareup.okhttp3" to mapOf(
                "*" to "okhttp"
            ),
            "com.google.guava" to mapOf(
                "guava" to "guava"
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
                        useVersion(
                            libraryVersions[it]
                                ?: throw RuntimeException("Key \"$it\" not set in libraryVersions.yaml")
                        )
                    } ?: pins[requested.name]?.let { pin ->
                        // Pin only for specific names given in map
                        useVersion(
                            libraryVersions[pin]
                                ?: throw RuntimeException("Key \"$pin\" not set in libraryVersions.yaml")
                        )
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
            ktlint(libraryVersions["ktlint"])
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
 */"""
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
