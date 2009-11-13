#!/bin/bash
#
#  For some reason, these jars were not found by JarLoader.  They must be in libraries defined outside of extensions.

GROUP_BASE=com.oracle.jdeveloper.jars
VERSION=10.1.3.4.0
JDEV_HOME=/opt/jdev/jdev10134
REPO_URL=scpexe://johnson30@sqa.llnl.gov/www/maven2
mvn deploy:deploy-file -DgroupId=$GROUP_BASE.jlib -DartifactId=adf-faces-impl -Dversion=$VERSION -Dfile=$JDEV_HOME/jlib/adf-faces-impl.jar -Dpackaging=jar -Durl=$REPO_URL
mvn deploy:deploy-file -DgroupId=$GROUP_BASE.jlib -DartifactId=adf-faces-api -Dversion=$VERSION -Dfile=$JDEV_HOME/jlib/adf-faces-api.jar -Dpackaging=jar -Durl=$REPO_URL
mvn deploy:deploy-file -DgroupId=$GROUP_BASE.jlib -DartifactId=adftags -Dversion=$VERSION -Dfile=$JDEV_HOME/BC4J/jlib/adftags.jar -Dpackaging=jar -Durl=$REPO_URL
