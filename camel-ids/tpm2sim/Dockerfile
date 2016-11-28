FROM debian:latest
MAINTAINER Georg Raess "georg.raess@aisec.fraunhofer.de"
# install used software
RUN apt-get -y update && apt-get -y upgrade && apt-get -y install sudo python3 python3-pip
RUN pip3 install --upgrade pip
RUN pip3 install -U setuptools
RUN pip3 install protobuf
RUN pip3 install simplejson
# run gosu
ENV GOSU_VERSION 1.9
RUN set -x \
    && apt-get update && apt-get install -y --no-install-recommends ca-certificates wget && rm -rf /var/lib/apt/lists/* \
    && dpkgArch="$(dpkg --print-architecture | awk -F- '{ print $NF }')" \
    && wget -O /usr/local/bin/gosu "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-$dpkgArch" \
    && wget -O /usr/local/bin/gosu.asc "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-$dpkgArch.asc" \
    && export GNUPGHOME="$(mktemp -d)" \
    && gpg --keyserver ha.pool.sks-keyservers.net --recv-keys B42F6819007F00F88E364FD4036A9C25BF357DD4 \
    && gpg --batch --verify /usr/local/bin/gosu.asc /usr/local/bin/gosu \
    && rm -r "$GNUPGHOME" /usr/local/bin/gosu.asc \
    && chmod +x /usr/local/bin/gosu \
    && gosu nobody true \
    && apt-get purge -y --auto-remove ca-certificates wget
# make some directories
RUN mkdir -p /data/cml/communication/tpm2d/
EXPOSE 29663
# copy static data
COPY script/ttp.py /tpm2d/
COPY bin/cml-tpm2d /tpm2d/
COPY bin/tpm2_simulator /tpm2d/
COPY script/start.sh /tpm2d/
COPY script/entry.sh /tpm2d/
WORKDIR /tpm2d
ENTRYPOINT ["/tpm2d/entry.sh"]
