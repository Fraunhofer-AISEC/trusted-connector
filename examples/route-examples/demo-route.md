The UC demo requires a running connector that matches the UC config given in the XML route. The `jmalloc/echo-server` may be exchanged with something more appropriate if desired.

The following steps are required:

- First, start UC demo container via `docker run -d -p 8080:8080 --name test jmalloc/echo-server`.
- The IP address in `<to>`-statement must be adapted for the new container. Display current IP address via `docker inspect --format='{{.NetworkSettings.IPAddress}}' test`.
- If the UC demo fails, check whether the repo digest needs to be adapted in XML (last part of DockerHub URI). Show recent digest with `docker inspect --format='{{.RepoDigests}}' jmalloc/echo-server`