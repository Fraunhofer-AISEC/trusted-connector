FROM openjdk:8
RUN mkdir /rat
COPY rat-repository-1.1.0-SNAPSHOT.jar /rat
WORKDIR /rat
EXPOSE 31337
CMD ["java", "-jar", "rat-repository-1.1.0-SNAPSHOT.jar","-p","31337","-h","127.0.0.1"]
