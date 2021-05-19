import com.benjaminsproule.swagger.gradleplugin.model.ApiSourceExtension
import com.benjaminsproule.swagger.gradleplugin.model.InfoExtension
import com.benjaminsproule.swagger.gradleplugin.model.LicenseExtension
import com.benjaminsproule.swagger.gradleplugin.model.ScopeExtension
import com.benjaminsproule.swagger.gradleplugin.model.SecurityDefinitionExtension
import com.github.gradle.node.yarn.task.YarnTask

@Suppress("UNCHECKED_CAST")
val libraryVersions = rootProject.extra.get("libraryVersions") as Map<String, String>

plugins {
    id("com.github.node-gradle.node") version "3.0.1"
    id("com.benjaminsproule.swagger")
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
    implementation("org.springframework.boot:spring-boot-starter-jersey")
    implementation("org.springframework.security", "spring-security-crypto")
    implementation("org.bouncycastle", "bcprov-jdk15on", libraryVersions["bouncyCastle"])
    implementation("de.fraunhofer.iais.eis.ids.infomodel", "java", libraryVersions["infomodel"])
    implementation("org.apache.camel", "camel-core", libraryVersions["camel"])
    implementation("org.apache.cxf", "cxf-rt-rs-extension-providers", libraryVersions["cxf"])
    implementation("org.bitbucket.b_c", "jose4j", libraryVersions["jose4j"])
    implementation("com.auth0", "java-jwt", libraryVersions["auth0Jwt"])

    compileOnly("io.swagger", "swagger-jaxrs", libraryVersions["swagger"])

    testImplementation("junit", "junit", libraryVersions["junit4"])
    testImplementation("org.mockito", "mockito-core", libraryVersions["mockito"])
    testImplementation("org.apache.cxf", "cxf-rt-transports-local", libraryVersions["cxf"])
    testImplementation("org.apache.cxf", "cxf-rt-rs-client", libraryVersions["cxf"])
    testImplementation("com.fasterxml.jackson.core", "jackson-core", libraryVersions["jackson"])
    testImplementation("com.fasterxml.jackson.jaxrs", "jackson-jaxrs-json-provider", libraryVersions["jackson"])
}

node {
    // This is important for a hassle-free build without pre-installed yarn!
    // To disable, pass -PnodeDownload=false to gradle!
    download.set(findProperty("nodeDownload")?.toString()?.toBoolean() ?: true)
}

val yarnInstall by tasks.registering(YarnTask::class) {
    inputs.file("src/main/angular/package.json").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file("src/main/angular/yarn.lock").withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir("src/main/angular/node_modules")
    outputs.cacheIf { true }

    workingDir.set(file("src/main/angular"))
    yarnCommand.set(listOf("install", "--ignore-optional"))
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
    // make sure yarn install is executed first
    dependsOn(yarnInstall)
}

tasks.processResources {
    dependsOn(yarnBuild)
}
