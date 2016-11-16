FROM ubuntu:16.04
RUN mkdir /tpm2d
RUN mkdir /protoc
RUN mkdir /socket
RUN apt-get -y update && apt-get install -y git autoconf automake libtool curl make g++ unzip python3 python3-pip
RUN pip3 install --upgrade pip
RUN pip3 install -U setuptools
RUN pip3 install protobuf
RUN git clone https://github.com/google/protobuf /protoc
COPY data/tpm2d.py /tpm2d
COPY data/attestation.proto /tpm2d
COPY data/ttp.py /tpm2d
COPY data/sigpubkey.bin /tpm2d
RUN cd /protoc && ./autogen.sh
RUN cd /protoc && ./configure && make && make install && ldconfig
RUN protoc --proto_path=/tpm2d --python_out=/tpm2d /tpm2d/attestation.proto 
#do not run on start
#RUN nohup /tpm2d/tpm2d.py /socket/socket1 &
#RUN nohup /tpm2d/tpm2d.py /socket/socket2 &
#RUN cd /tpm2d && nohup ./ttp.py & 
