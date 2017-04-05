FROM ubuntu:16.04

MAINTAINER Christian Banse <christian.banse@aisec.fraunhofer.de>

RUN apt-get update
RUN apt-get install -y git curl unzip
RUN apt-get install -y python-dev
RUN apt-get install -y python-pip
RUN apt-get install -y nodejs nodejs-legacy npm nginx

EXPOSE 80

WORKDIR /tmp

# this should hopefully trigger Docker to only update npm/jspm if dependencies have changed
ADD package.json .
RUN npm install --no-optional
RUN npm run postinstall

# add the rest of the files
ADD . .

# set environment to production
ENV NODE_ENV production

# build everything for production
RUN npm run bundle

# copy to nginx
RUN cp -r dist/* /var/www/html/

CMD ["nginx", "-g", "daemon off;"]
