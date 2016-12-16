FROM openjdk:8
RUN mkdir /rat
COPY jar/rat-repository-1.0.0.jar /rat
WORKDIR /rat
EXPOSE 31337
CMD ["java", "-jar", "rat-repository-1.0.0.jar","-p","31337","-h","127.0.0.1"]
