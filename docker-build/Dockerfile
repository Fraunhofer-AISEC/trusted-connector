ARG JDK_BASE_IMAGE=fraunhoferaisec/jdk-base:develop
FROM $JDK_BASE_IMAGE

LABEL AUTHOR="Michael Lux (michael.lux@aisec.fraunhofer.de)"

# Install tools for nodejs/yarn setup and protobuf compiler
RUN apt-get update -qq && apt-get install -qq bash sudo wget gnupg protobuf-compiler

COPY run.sh .
RUN chmod +x run.sh

ENTRYPOINT ["/run.sh"]
CMD ["yarnBuild", "check", ":build", "--parallel"]