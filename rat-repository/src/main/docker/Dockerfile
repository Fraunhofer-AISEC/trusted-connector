FROM openjdk:8-alpine
RUN mkdir /rat
COPY rat-repository.jar /rat/rat-repository.jar
WORKDIR /rat
EXPOSE 31337
CMD ["java", "-jar", "rat-repository.jar","-p","31337","-h","127.0.0.1"]
