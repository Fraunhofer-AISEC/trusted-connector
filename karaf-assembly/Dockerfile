FROM openjdk:8-alpine

LABEL AUTHOR="Julian Schütte (julian.schuette@aisec.fraunhofer.de)"

# Add some additional packages:
# socat is not important, just for debugging and playing around with UNIX sockets
# libstdc++ is required for some (core) Java functionality
# bash is just a nicer shell than sh
RUN apk --update --no-cache add socat libstdc++ bash docker \
    && ldconfig /lib /usr/glibc/usr/lib \
    && echo 'hosts: files mdns4_minimal [NOTFOUND=return] dns mdns4' >> /etc/nsswitch.conf
# The latter is required for proper hostname resolution in Java on Alpine linux

# Adding the actual core platform files to /root
ADD build/assembly /root/

# Making karaf start script and docker CLI executable
RUN chmod 0755 /root/bin/karaf

CMD "/bin/bash"
WORKDIR "/root"

# Ports to expose
EXPOSE 8181
EXPOSE 8443
EXPOSE 29998
EXPOSE 5005
EXPOSE 1099
EXPOSE 1098
EXPOSE 9292
ENTRYPOINT ["/root/bin/karaf"]
