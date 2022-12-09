ARG BASE_IMAGE=gcr.io/distroless/java17-debian11
FROM $BASE_IMAGE

LABEL AUTHOR="Michael Lux (michael.lux@aisec.fraunhofer.de)"

# Add the actual core platform JARs to /root/jars, as two layers
ADD build/libs/libraryJars/* /root/jars/
ADD build/libs/projectJars/* /root/jars/

WORKDIR "/root"

# Ports to expose
EXPOSE 8080 29292

ENTRYPOINT ["java"]
CMD ["--add-exports=java.base/sun.security.x509=ALL-UNNAMED", \
"--add-exports=java.base/sun.security.pkcs=ALL-UNNAMED", \
"--add-exports=java.base/sun.security.pkcs10=ALL-UNNAMED", \
"--class-path", "./jars/*", "de.fhg.aisec.ids.TrustedConnector"]
