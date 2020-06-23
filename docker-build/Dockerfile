ARG JDK_BASE_IMAGE=fraunhoferaisec/jdk-base:develop
FROM $JDK_BASE_IMAGE

LABEL AUTHOR="Michael Lux (michael.lux@aisec.fraunhofer.de)"

# Install tools for nodejs/yarn setup
RUN apt-get update -qq && apt-get install -qq bash sudo wget gnupg
# Install nodejs 12.x
RUN wget -O - https://deb.nodesource.com/setup_12.x | bash - && apt-get install -qq nodejs
# Install yarn
RUN wget -O - https://dl.yarnpkg.com/debian/pubkey.gpg | apt-key add - \
    && echo "deb https://dl.yarnpkg.com/debian/ stable main" | tee /etc/apt/sources.list.d/yarn.list \
    && apt-get update -qq && apt-get -qq install yarn
# Install newer protobuf compiler from focal (3.0.x doesn't generate required has...() methods)
# This is an ugly hack, but maintains compatiblity to non-x86 platforms
RUN sed -e 's/bionic/focal/' /etc/apt/sources.list > /etc/apt/sources.list.d/focal.list \
    && apt-get update -qq && apt-get install -qq protobuf-compiler \
    # Perform cleanup
    && rm /etc/apt/sources.list.d/focal.list && apt-get clean && rm -r /var/lib/apt/lists/*

COPY run.sh .
RUN chmod +x run.sh

ENTRYPOINT ["/run.sh"]
CMD ["build", "check", "--parallel"]