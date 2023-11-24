ARG BASE_IMAGE=eclipse-temurin:21-jdk
FROM $BASE_IMAGE

LABEL AUTHOR="Michael Lux (michael.lux@aisec.fraunhofer.de)"

# Install tools for nodejs/yarn setup and protobuf compiler
RUN apt-get update -qq && apt-get install -qq bash sudo rsync protobuf-compiler

COPY run.sh .
RUN chmod +x run.sh
VOLUME /build

ENTRYPOINT ["/run.sh"]
CMD ["yarnBuild", "check", ":ids-connector:build", "--parallel"]