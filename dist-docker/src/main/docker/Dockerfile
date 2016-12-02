FROM frolvlad/alpine-oraclejdk8:cleaned
MAINTAINER Julian Schütte (julian.schuette@aisec.fraunhofer.de)

# Some Java native libs require glibc, which must be added manually to Alpine linux
#RUN apk --update add wget curl ca-certificates tar && \
#    curl -Ls https://github.com/sgerrand/alpine-pkg-glibc/releases/download/2.21-r2/glibc-2.21-r2.apk > glibc.apk && \
#    apk add --allow-untrusted glibc.apk

RUN apk --update add socat libstdc++ bash \
    && rm -rf /var/cache/apk/* \
    && rm /bin/sh \
    && ln -s /bin/bash /bin/sh \
    && ldconfig /lib /usr/glibc/usr/lib \
    && echo 'hosts: files mdns4_minimal [NOTFOUND=return] dns mdns4' >> /etc/nsswitch.conf 
# The latter is required for proper hostname resolution in Java on Alpine linux

ADD docker-files/ /root/

CMD "/bin/bash"
RUN chmod 0755 /root/bin/karaf
WORKDIR "/root"
EXPOSE 8181
EXPOSE 29998
EXPOSE 5005
EXPOSE 1099
EXPOSE 1098
ENTRYPOINT ["/root/bin/karaf", "debug"]
