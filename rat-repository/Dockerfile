FROM openjdk:11-jdk-slim-stretch

ARG VERSION
ENV FILENAME="rat-repository-${VERSION}.jar"

LABEL AUTHOR="Michael Lux (michael.lux@aisec.fraunhofer.de)"

# Optional: Install oh-my-zsh for a better shell (~ 20 MiB overhead)
#RUN apt-get update && apt-get install -y wget git zsh fonts-powerline \
#  && wget https://github.com/robbyrussell/oh-my-zsh/raw/master/tools/install.sh -O - | zsh || true \
#  # Set nice theme
#  && sed -ie 's/^ZSH_THEME=".*"$/ZSH_THEME="agnoster"/' ~/.zshrc \
#  # Disable automatic update
#  && sed -ie 's/^# DISABLE_AUTO_UPDATE$/DISABLE_AUTO_UPDATE/' ~/.zshrc \
#  # Remove wget and git, Cleanup
#  && apt-get remove --purge -y wget git && apt-get autoremove --purge -y

ADD build/libs/rat-repository-${VERSION}.jar /root/

WORKDIR "/root"

EXPOSE 31337

CMD java -jar ${FILENAME} -p 31337 -h 0.0.0.0
