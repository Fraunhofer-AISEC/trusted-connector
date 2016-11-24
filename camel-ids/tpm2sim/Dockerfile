FROM debian:latest
MAINTAINER Georg Raess "georg.raess@aisec.fraunhofer.de"
# install used software
RUN apt-get -y update && apt-get -y upgrade && apt-get -y install sudo python3 python3-pip
RUN pip3 install --upgrade pip
RUN pip3 install -U setuptools
RUN pip3 install protobuf
RUN pip3 install simplejson
# adduser tpm2d
RUN adduser --disabled-password --gecos '' tpm2d
RUN adduser tpm2d sudo
RUN echo '%sudo ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers
# make some directories
RUN mkdir /tpm2d && mkdir /ttp && mkdir -p /data/cml/communication/tpm2d/
EXPOSE 29663
# copy static data
COPY script/ttp.py /ttp/
COPY bin/cml-tpm2d /tpm2d/
COPY bin/tpm2_simulator /tpm2d/
COPY script/start.sh /tpm2d/
