# syntax = docker/dockerfile:experimental

ARG BUILDER_IMAGE=amd64/eclipse-temurin:21-jdk
ARG BASE_IMAGE=gcr.io/distroless/java21-debian12

FROM $BUILDER_IMAGE AS builder
LABEL AUTHOR="Michael Lux (michael.lux@aisec.fraunhofer.de)"
# Install tools for nodejs/yarn setup and protobuf compiler
RUN apt-get update -y && apt-get install -y bash sudo wget gnupg protobuf-compiler
WORKDIR /app
COPY . .
RUN --mount=type=cache,target=/root/.gradle \
    --mount=type=cache,target=/root/.m2 \
    --mount=type=cache,target=/app/ids-webconsole/src/main/angular/node_modules \
    ./gradlew yarnBuild check :ids-connector:build --parallel

FROM $BASE_IMAGE
LABEL AUTHOR="Michael Lux (michael.lux@aisec.fraunhofer.de)"
# Add the actual core platform JARs to /root/jars, as two layers
COPY --from=builder /app/ids-connector/build/libs/libraryJars/* /root/jars/
COPY --from=builder /app/ids-connector/build/libs/projectJars/* /root/jars/
WORKDIR "/root"
# Ports to expose
EXPOSE 8080 29292
ENTRYPOINT ["java"]
CMD ["--add-exports=java.base/sun.security.x509=ALL-UNNAMED", \
"--add-exports=java.base/sun.security.pkcs=ALL-UNNAMED", \
"--add-exports=java.base/sun.security.pkcs10=ALL-UNNAMED", \
"--class-path", "./jars/*", "de.fhg.aisec.ids.TrustedConnector", \
"--spring.config.location=classpath:application.yml,optional:/root/etc/application.yml"]
