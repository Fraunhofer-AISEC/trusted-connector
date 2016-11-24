FROM debian:latest
MAINTAINER Georg Raess "georg.raess@aisec.fraunhofer.de"
# install used software
RUN apt-get -y update && apt-get -y upgrade && apt-get -y install sudo
# adduser tpm2d
RUN adduser --disabled-password --gecos '' tpm2d
RUN adduser tpm2d sudo
RUN echo '%sudo ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers
# make some directories
RUN mkdir /tpm2d && mkdir -p /data/cml/communication/tpm2d/
# copy static data
COPY bin/cml-tpm2d /tpm2d/
COPY bin/tpm2_simulator /tpm2d/
COPY bin/start.sh /tpm2d/
