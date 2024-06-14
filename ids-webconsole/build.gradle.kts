import com.benjaminsproule.swagger.gradleplugin.model.ApiSourceExtension
import com.benjaminsproule.swagger.gradleplugin.model.InfoExtension
import com.benjaminsproule.swagger.gradleplugin.model.LicenseExtension
import com.benjaminsproule.swagger.gradleplugin.model.ScopeExtension
import com.benjaminsproule.swagger.gradleplugin.model.SecurityDefinitionExtension
import com.github.gradle.node.yarn.task.YarnTask

plugins {
    alias(libs.plugins.node)
    alias(libs.plugins.swagger)
}

description = "IDS Core Platform Webconsole"

swagger {
    apiSource(
        closureOf<ApiSourceExtension> {
            springmvc = false
            locations = listOf("de.fhg.aisec.ids.webconsole.api")
            schemes = listOf("http")
            host = "localhost:8181"
            basePath = "/"
            info(
                closureOf<InfoExtension> {
                    title = "Trusted Connector API"
                    version = project.version as String
                    license(
                        closureOf<LicenseExtension> {
                            url = "http://www.apache.org/licenses/LICENSE-2.0.html"
                            name = "Apache 2.0"
                        }
                    )
                    description = """This is the administrative REST API of the Trusted Connector.

The API provides an administrative interface to manage the Trusted Connector at runtime
and is used by the default administration dashboard ("web console").
"""
                }
            )
            swaggerDirectory = "${project.projectDir}/generated/swagger-ui"
            outputFormats = listOf("json", "yaml")
            securityDefinition(
                closureOf<SecurityDefinitionExtension> {
                    // `name` can be used refer to this security schemes from elsewhere
                    name = "oauth2"
                    type = "oauth2"
                    // The flow used by the OAuth2 security scheme
                    flow = "password"
                    tokenUrl = "https://localhost:8181/user/login"
                    scope(
                        closureOf<ScopeExtension> {
                            name = "write:api"
                            description = "Read and write access to the API"
                        }
                    )
                }
            )
        /* the plugin could theoretically also generate the html files, however it currently only allows
        for the generation of html OR swagger.json, not both. Therefore we still need to use spectacle using yarn */
            // templatePath = "${project.projectDir}/src/test/resources/strapdown.html.hbs"
            // outputPath = "${project.projectDir}/generated/document.html"
        }
    )
}

dependencies {
    implementation(project(":ids-api"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation(libs.spring.security.crypto)
    implementation(libs.infomodel)
    implementation(libs.camel.core)
    implementation(libs.jose4j)
    implementation(libs.auth0Jwt)
    implementation(libs.bundles.ktor.richClient)
    implementation(libs.ktor.client.auth)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.reactive)
    implementation(libs.kotlinx.reactor)

    compileOnly(libs.swagger.jaxrs)

    testImplementation(libs.bundles.test5)
    testImplementation(libs.jackson.core)
    testImplementation(libs.jackson.jaxrsJsonProvider)
}

node {
    // This is important for a hassle-free build without pre-installed yarn!
    // To disable, pass -PnodeDownload=false to gradle!
    download.set(findProperty("nodeDownload")?.toString()?.toBoolean() ?: true)
    version.set("20.14.0")
}

val yarnInstall by tasks.registering(YarnTask::class) {
    inputs.file("src/main/angular/package.json").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file("src/main/angular/yarn.lock").withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir("src/main/angular/node_modules")
    outputs.cacheIf { true }

    workingDir.set(file("src/main/angular"))
    yarnCommand.set(listOf("install"))
    onlyIf { !rootProject.hasProperty("skipAngular") }
}

val yarnLint by tasks.registering(YarnTask::class) {
    inputs.file("src/main/angular/package.json").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file("src/main/angular/yarn.lock").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file("src/main/angular/.eslintrc.json").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir("src/main/angular/src").withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.upToDateWhen { true }
    outputs.cacheIf { true }

    workingDir.set(file("src/main/angular"))
    yarnCommand.set(listOf("ng", "lint"))
    onlyIf { !rootProject.hasProperty("skipAngular") }

    dependsOn(yarnInstall)
}

val yarnBuild by tasks.registering(YarnTask::class) {
    inputs.file("src/main/angular/package.json").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file("src/main/angular/yarn.lock").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file("src/main/angular/angular.json").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir("src/main/angular/src").withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir("build/resources/main/www")
    outputs.cacheIf { true }

    workingDir.set(file("src/main/angular"))
    yarnCommand.set(listOf("bundle"))
    onlyIf { !rootProject.hasProperty("skipAngular") }

    dependsOn(yarnLint)
}

tasks.processResources {
    dependsOn(yarnBuild)
}
