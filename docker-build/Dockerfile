ARG BASE_IMAGE=debian:buster-slim
FROM $BASE_IMAGE

LABEL AUTHOR="Michael Lux (michael.lux@aisec.fraunhofer.de)"

# Fix for installing openjdk on debian-slim
RUN mkdir -p /usr/share/man/man1
# Install JDK
RUN apt-get update -qq && apt-get install -qq --no-install-recommends openjdk-11-jdk

# Install tools for nodejs/yarn setup
RUN apt-get update -qq && apt-get install -qq bash sudo wget gnupg
# Install nodejs 12.x
RUN wget -O - https://deb.nodesource.com/setup_12.x | bash - && apt-get install -qq nodejs
# Install yarn
RUN wget -O - https://dl.yarnpkg.com/debian/pubkey.gpg | apt-key add - \
    && echo "deb https://dl.yarnpkg.com/debian/ stable main" | tee /etc/apt/sources.list.d/yarn.list \
    && apt-get update && apt-get install yarn
# Install protobuf compiler (protoc)
RUN apt-get update -qq && apt-get install -qq protobuf-compiler

COPY run.sh .
RUN chmod +x run.sh

ENTRYPOINT ["/run.sh"]
CMD ["build", "check", "--no-daemon", "--parallel"]