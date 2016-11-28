FROM debian:latest
MAINTAINER Georg Raess "georg.raess@aisec.fraunhofer.de"
# install used software
RUN apt-get -y update && apt-get -y upgrade && apt-get -y install sudo python3 python3-pip
RUN pip3 install --upgrade pip
RUN pip3 install -U setuptools
RUN pip3 install simplejson
# make some directories
RUN mkdir /tpm2d/ && mkdir -p /data/cml/communication/tpm2d/
EXPOSE 29663
# copy data
COPY script/ttp.py /tpm2d/
WORKDIR /tpm2d
