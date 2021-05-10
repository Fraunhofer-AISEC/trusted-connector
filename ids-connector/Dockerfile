ARG BASE_IMAGE=adoptopenjdk:11-jdk-hotspot-focal
FROM $BASE_IMAGE

LABEL AUTHOR="Michael Lux (michael.lux@aisec.fraunhofer.de)"

# Install socat for UNIX socket debugging and ps command for karaf scripts
RUN apt-get update -qq && apt-get install -qq socat procps

# Optional: Install oh-my-zsh for a better shell (~ 20 MiB overhead)
RUN apt-get update -qq && apt-get install -qq wget git zsh fonts-powerline \
  && wget https://github.com/robbyrussell/oh-my-zsh/raw/master/tools/install.sh -O - | zsh || true \
  # Set nice theme
  && sed -ie 's/^ZSH_THEME=".*"$/ZSH_THEME="agnoster"/' ~/.zshrc \
  # Disable automatic update
  && sed -ie 's/^# DISABLE_AUTO_UPDATE$/DISABLE_AUTO_UPDATE/' ~/.zshrc \
  # Remove git, Cleanup
  && apt-get remove --purge -qq git && apt-get autoremove --purge -qq \
  # Inject karaf console start command into zsh history
  && echo ": 0:0;bin/client" > ~/.zsh_history

# Add the actual core platform files to /root
ADD build/libs/lib /root/lib/
ADD build/libs/ids-connector.jar /root/

WORKDIR "/root"

# Ports to expose
EXPOSE 8080 29292

ENTRYPOINT ["java", "--class-path", "./ids-connector.jar:./lib/*", "de.fhg.aisec.ids.TrustedConnector"]
