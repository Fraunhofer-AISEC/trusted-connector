FROM node:12-slim

#https://github.com/puckel/docker-airflow/issues/182#issuecomment-444683455
RUN mkdir -p /usr/share/man/man1
RUN apt-get update -qq && apt-get install -qq bash sudo openjdk-8-jdk

COPY run.sh .
RUN chmod +x run.sh

ENV GRADLE_OPTS: "-Dorg.gradle.daemon=false"

ENTRYPOINT ["/run.sh"]
CMD ["build", "check", "dockerize", "--parallel", "-PdockerTag=develop"]