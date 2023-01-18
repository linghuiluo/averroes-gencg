#!/bin/bash

mvn install:install-file -Dfile=probe.jar -DgroupId=probe -DartifactId=probe -Dversion=0.0.1 -Dpackaging=jar -DgeneratePom=true
