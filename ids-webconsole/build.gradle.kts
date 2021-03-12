import com.benjaminsproule.swagger.gradleplugin.model.*
import com.github.gradle.node.yarn.task.YarnTask

@Suppress("UNCHECKED_CAST")
val libraryVersions = rootProject.extra.get("libraryVersions") as Map<String, String>

plugins {
    id("com.github.node-gradle.node") version "3.0.1"
    id("com.benjaminsproule.swagger") version "1.0.14"
}

description = "IDS Core Platform Webconsole"

sourceSets {
    main {
        // only include the OSGI blueprint and the built angular app
        resources {
            include("OSGI-INF/blueprint/*")
        }
    }
}

swagger {
    apiSource(closureOf<ApiSourceExtension> {
        springmvc = false
        locations = listOf("de.fhg.aisec.ids.webconsole.api")
        schemes = listOf("http")
        host = "localhost:8181"
        basePath = "/"
        info(closureOf<InfoExtension> {
            title = "Trusted Connector API"
            version = project.version as String
            license(closureOf<LicenseExtension> {
                url = "http://www.apache.org/licenses/LICENSE-2.0.html"
                name = "Apache 2.0"
            })
            description ="""This is the administrative REST API of the Trusted Connector.

The API provides an administrative interface to manage the Trusted Connector at runtime
and is used by the default administration dashboard ("web console").
"""
        })
        swaggerDirectory = "${project.projectDir}/generated/swagger-ui"
        outputFormats = listOf("json","yaml")
        securityDefinition(closureOf<SecurityDefinitionExtension> {
            // `name` can be used refer to this security schemes from elsewhere
            name = "oauth2"
            type = "oauth2"
            // The flow used by the OAuth2 security scheme
            flow = "password"
            tokenUrl = "https://localhost:8181/user/login"
            scope(closureOf<ScopeExtension> {
                name = "write:api"
                description = "Read and write access to the API"
            })
        })
        /* the plugin could theoretically also generate the html files, however it currently only allows 
        for the generation of html OR swagger.json, not both. Therefore we still need to use spectacle using yarn */
        // templatePath = "${project.projectDir}/src/test/resources/strapdown.html.hbs"
        // outputPath = "${project.projectDir}/generated/document.html"   
    })
}

dependencies {
    providedByBundle(project(":ids-api")) { isTransitive = false }

    // Actual implementation must be provided by ids-infomodel-manager
    compileOnly("de.fraunhofer.iais.eis.ids.infomodel", "java", libraryVersions["infomodel"])

    providedByFeature("javax.servlet", "javax.servlet-api", libraryVersions["javaxServlet"])

    providedByFeature("org.apache.camel", "camel-core", libraryVersions["camel"])

    providedByFeature("org.apache.cxf", "cxf-rt-frontend-jaxrs", libraryVersions["cxf"])
    providedByFeature("org.apache.cxf", "cxf-rt-rs-extension-providers", libraryVersions["cxf"])

    // This is required for compilation only, and provided by karaf automatically, likely by some included feature
    compileOnly("javax.ws.rs", "javax.ws.rs-api", libraryVersions["wsRsApi"])

    compileOnly("org.checkerframework", "checker-qual", libraryVersions["checkerQual"])

    // use our specified version of jackson instead of the cxf-jackson feature
    providedByBundle("com.fasterxml.jackson.core", "jackson-core", libraryVersions["jackson"])
    providedByBundle("com.fasterxml.jackson.core", "jackson-databind", libraryVersions["jackson"])
    providedByBundle("com.fasterxml.jackson.core", "jackson-annotations", libraryVersions["jackson"])
    providedByBundle("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml", libraryVersions["jackson"])
    providedByBundle("com.fasterxml.jackson.jaxrs", "jackson-jaxrs-json-provider", libraryVersions["jackson"])

    providedByBundle("org.bitbucket.b_c", "jose4j", libraryVersions["jose4j"])
    implementation("com.auth0", "java-jwt", libraryVersions["auth0Jwt"])
    osgiCore("org.apache.felix", "org.apache.felix.framework", libraryVersions["felixFramework"])
    osgiCore("org.osgi", "osgi.cmpn", libraryVersions["osgiCompendium"])

    compileOnly("io.swagger", "swagger-jaxrs", libraryVersions["swagger"])

    testImplementation("junit", "junit", libraryVersions["junit4"])
    testImplementation("org.mockito", "mockito-core", libraryVersions["mockito"])

    testImplementation("org.apache.cxf", "cxf-rt-transports-local", libraryVersions["cxf"])
    testImplementation("org.apache.cxf", "cxf-rt-rs-client", libraryVersions["cxf"])
}

node {
    download.set(true)
}

val yarnInstall by tasks.registering(YarnTask::class) {
    inputs.file("src/main/resources/www/package.json").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file("src/main/resources/www/yarn.lock").withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir("src/main/resources/www/node_modules")
    outputs.cacheIf { true }

    workingDir.set(file("src/main/resources/www"))
    yarnCommand.set(listOf("install", "--ignore-optional"))
    onlyIf { !rootProject.hasProperty("skipAngular") }
}

val yarnLint by tasks.registering(YarnTask::class) {
    inputs.file("src/main/resources/www/package.json").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file("src/main/resources/www/yarn.lock").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file("src/main/resources/www/tslint.json").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir("src/main/resources/www/src").withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.upToDateWhen { true }
    outputs.cacheIf { true }

    workingDir.set(file("src/main/resources/www"))
    yarnCommand.set(listOf("lint"))
    onlyIf { !rootProject.hasProperty("skipAngular") }
}

val yarnBuild by tasks.registering(YarnTask::class) {
    inputs.file("src/main/resources/www/package.json").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file("src/main/resources/www/yarn.lock").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file("src/main/resources/www/angular.json").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir("src/main/resources/www/src").withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir("build/resources/main/www")
    outputs.cacheIf { true }

    workingDir.set(file("src/main/resources/www"))
    yarnCommand.set(listOf("bundle"))
    onlyIf { !rootProject.hasProperty("skipAngular") }

    dependsOn(yarnLint)
    // make sure yarn install is executed first
    dependsOn(yarnInstall)
}

tasks.named("processResources")<Task> {
    dependsOn(yarnBuild)
}
