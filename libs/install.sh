#!/bin/bash

mvn install:install-file -Dfile=probe.jar -DgroupId=probe -DartifactId=probe -Dversion=0.0.1 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=soot-4.3.0-SNAPSHOT.jar -DgroupId=org.soot-oss -DartifactId=soot -Dversion=4.3.0-SNAPSHOT -Dpackaging=jar -DgeneratePom=true
