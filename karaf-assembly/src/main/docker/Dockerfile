FROM frolvlad/alpine-oraclejdk8:cleaned
MAINTAINER Julian Schütte (julian.schuette@aisec.fraunhofer.de)

# Add some additional packages:
# socat is not important, just for debugging and playing around with UNIX sockets
# libstdc++ is required for some (core) Java functionality
# bash is just a nicer shell than sh
# libtdl is required for the docker CLI client
RUN apk --update add socat libstdc++ bash libltdl \
    && rm -rf /var/cache/apk/* \
    && rm /bin/sh \
    && ln -s /bin/bash /bin/sh \
    && ldconfig /lib /usr/glibc/usr/lib \
    && echo 'hosts: files mdns4_minimal [NOTFOUND=return] dns mdns4' >> /etc/nsswitch.conf
# The latter is required for proper hostname resolution in Java on Alpine linux

# Adding the actual core platform files to /root
ADD * /root/

# Making karaf start script and docker CLI executable
RUN chmod 0755 /root/bin/karaf \
	&& chmod 0755 /root/bin/docker \
	&& ln -s /root/bin/docker /bin/docker

CMD "/bin/bash"
WORKDIR "/root"

# Ports to expose
EXPOSE 8181
EXPOSE 29998
EXPOSE 5005
EXPOSE 1099
EXPOSE 1098
EXPOSE 9292
ENTRYPOINT ["/root/bin/karaf"]
