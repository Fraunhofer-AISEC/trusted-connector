FROM ubuntu:16.04
MAINTAINER Georg Raess "georg.raess@aisec.fraunhofer.de"
# install used software
RUN apt-get -y update && apt-get install -y git autoconf automake libtool curl make g++ unzip python3 python3-pip sudo
RUN pip3 install --upgrade pip
RUN pip3 install -U setuptools
RUN pip3 install protobuf
# adduser tpm2d
RUN adduser --disabled-password --gecos '' tpm2d
RUN adduser tpm2d sudo
RUN echo '%sudo ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers
# make some directories
RUN mkdir /tpm2d && mkdir /protoc && mkdir /socket
# copy static data
COPY data/tpm2ds.py /tpm2d
COPY data/tpm2dc.py /tpm2d
COPY data/attestation.proto /tpm2d
COPY data/ttp.py /tpm2d
COPY data/sigpubkey.bin /tpm2d
# compile protoc
RUN git clone https://github.com/google/protobuf /protoc
WORKDIR /protoc
RUN ./autogen.sh && ./configure && make && make install && ldconfig
RUN protoc --proto_path=/tpm2d --python_out=/tpm2d /tpm2d/attestation.proto 
RUN chown -R tpm2d /tpm2d
RUN chown -R tpm2d /socket
RUN chown -R tpm2d /protoc
EXPOSE 29663
#do not run on startup
#RUN nohup /tpm2d/tpm2d.py /socket/socket1 &
#RUN nohup /tpm2d/tpm2d.py /socket/socket2 &
#RUN cd /tpm2d && nohup ./ttp.py & 
