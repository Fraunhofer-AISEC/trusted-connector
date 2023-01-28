import com.diffplug.gradle.spotless.SpotlessApply
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
    version = "7.0.0"

    val versionRegex = ".*(rc-?[0-9]*|beta)$".toRegex(RegexOption.IGNORE_CASE)

    tasks.withType<DependencyUpdatesTask> {
        rejectVersionIf {
            // Reject release candidates and betas and pin Apache Camel to 3.18 LTS version
            versionRegex.matches(candidate.version)
                || (candidate.group in setOf("org.apache.camel", "org.apache.camel.springboot")
                && !candidate.version.startsWith("3.18"))
                || (candidate.group.startsWith("de.fraunhofer.iais.eis.ids") && !candidate.version.startsWith("4.1."))
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

    configure<DependencyManagementExtension> {
        // This order is important! If camel versions are imported after spring, breaking downgrades will occur!
        imports {
            mavenBom("org.apache.camel.springboot:camel-spring-boot-dependencies:${rootProject.libs.versions.camel.get()}")
        }
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:${rootProject.libs.versions.springBoot.get()}")
        }
    }

    dependencies {
        // Some versions are downgraded for unknown reasons, fix this here
        val groupPins = mapOf(
            "org.jetbrains.kotlin" to mapOf(
                "*" to rootProject.libs.versions.kotlin.get()
            ),
            "com.google.guava" to mapOf(
                "guava" to rootProject.libs.versions.guava.get()
            ),
            "com.sun.xml.bind" to mapOf(
                "jaxb-core" to rootProject.libs.versions.jaxbCore.get(),
                "jaxb-impl" to rootProject.libs.versions.jaxbImpl.get()
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
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "17"
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
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

val spotlessApplyAll: Task by tasks.creating

configure(subprojects.filter { it.name != "examples" }) {
    apply(plugin = "com.diffplug.spotless")

    tasks.withType<SpotlessApply> {
        spotlessApplyAll.dependsOn(this.path)
    }

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
