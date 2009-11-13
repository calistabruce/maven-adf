#!/bin/bash
#
#  These libraries were not found and built by JarLoader.

POM_PATH=./
VERSION=10.1.3.4.0
JDEV_HOME=/opt/jdev/jdev10134
REPO_URL=scpexe://sqa.llnl.gov/www/maven2
mvn deploy:deploy-file -DpomFile=$POM_PATH/ADF_Faces_Runtime.pom -Dversion=$VERSION -Dfile=$POM_PATH/ADF_Faces_Runtime.pom -Dpackaging=pom -Durl=$REPO_URL
