FROM openjdk:8
MAINTAINER Georg Raess "georg.raess@aisec.fraunhofer.de"
# install used software
RUN apt-get -y update && apt-get -y upgrade
# make some directories
RUN mkdir /rat/
EXPOSE 31337
# copy data
COPY rat-repository-1.0.0.jar /rat
WORKDIR /rat
