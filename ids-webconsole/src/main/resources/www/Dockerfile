FROM ubuntu:16.04

MAINTAINER Christian Banse <christian.banse@aisec.fraunhofer.de>

RUN apt-get update
RUN apt-get install -y git curl unzip
RUN apt-get install -y python-dev
RUN apt-get install -y python-pip
RUN apt-get install -y nodejs nodejs-legacy npm nginx

EXPOSE 80

WORKDIR /tmp

RUN npm install -g jspm gulp
RUN jspm config registries.github.auth 9766c9c82013e1eba50c2797872521da99ee9a4e

# this should hopefully trigger Docker to only update npm/jspm if dependencies have changed
ADD package.json .
ADD typings.json .
ADD config.js .
RUN npm install
RUN npm run postinstall

# add the rest of the files
ADD . .

# set environment to production
ENV NODE_ENV production

# build everything for production
RUN gulp bundle

# copy to nginx
RUN cp -r dist/* /var/www/html/

CMD ["nginx", "-g", "daemon off;"]
