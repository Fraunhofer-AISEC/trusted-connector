#!/bin/sh
mvn install -DskipITs -DskipTests -DskipBugs -DskipDocker -DskipAngular -DskipLicense "$@"
