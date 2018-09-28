FROM alpine:3.7
MAINTAINER Georg Raess "georg.raess@aisec.fraunhofer.de"
LABEL de.fhg.aisec.ids.container.label="tpm2d"
LABEL de.fhg.aisec.ids.version="1.0"
# install used software
RUN apk update \
    && apk add su-exec shadow
# make some directories
RUN mkdir /tpm2d/ && mkdir -p /data/cml/tpm2d/communication/ && mkdir -p /data/tpm2_simulator/
# copy data
COPY bin/cml-tpm2d /tpm2d/
COPY bin/tpm2_simulator /tpm2d/
COPY script/entry.sh /tpm2d/
WORKDIR /tpm2d
ENTRYPOINT ["/tpm2d/entry.sh"]
