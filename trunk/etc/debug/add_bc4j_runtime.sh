#!/bin/bash

# Possible groupId's: BC4J_Runtime com.oracle.jdeveloper
GROUP="com.oracle.jdeveloper"
VERSION=10.1.3.0.4
JDEV_HOME=/Applications/JDeveloper.app/Contents/Resources/jdev
REPO_URL=file://Users/admin/maven2-repo

mvn deploy:deploy-file -DgroupId=$GROUP.BC4J.lib -DartifactId=adfshare -Dversion=$VERSION -Dfile=$JDEV_HOME/BC4J/lib/adfshare.jar -Dpackaging=jar -Durl=$REPO_URL
mvn deploy:deploy-file -DgroupId=$GROUP.BC4J.lib -DartifactId=bc4jmt -Dversion=$VERSION -Dfile=$JDEV_HOME/BC4J/lib/bc4jmt.jar -Dpackaging=jar -Durl=$REPO_URL
mvn deploy:deploy-file -DgroupId=$GROUP.BC4J.lib -DartifactId=collections -Dversion=$VERSION -Dfile=$JDEV_HOME/BC4J/lib/collections.jar -Dpackaging=jar -Durl=$REPO_URL
mvn deploy:deploy-file -DgroupId=$GROUP.BC4J.lib -DartifactId=bc4jct -Dversion=$VERSION -Dfile=$JDEV_HOME/BC4J/lib/bc4jct.jar -Dpackaging=jar -Durl=$REPO_URL

mvn deploy:deploy-file -DgroupId=$GROUP.lib -DartifactId=xmlparserv2 -Dversion=$VERSION -Dfile=$JDEV_HOME/lib/xmlparserv2.jar -Dpackaging=jar -Durl=$REPO_URL

mvn deploy:deploy-file -DgroupId=$GROUP.jlib -DartifactId=jdev-cm -Dversion=$VERSION -Dfile=$JDEV_HOME/jlib/jdev-cm.jar -Dpackaging=jar -Durl=$REPO_URL
mvn deploy:deploy-file -DgroupId=$GROUP.jlib -DartifactId=ojmisc -Dversion=$VERSION -Dfile=$JDEV_HOME/jlib/ojmisc.jar -Dpackaging=jar -Durl=$REPO_URL
mvn deploy:deploy-file -DgroupId=$GROUP.jlib -DartifactId=commons-el -Dversion=$VERSION -Dfile=$JDEV_HOME/jlib/commons-el.jar -Dpackaging=jar -Durl=$REPO_URL
mvn deploy:deploy-file -DgroupId=$GROUP.jlib -DartifactId=jsp-el-api -Dversion=$VERSION -Dfile=$JDEV_HOME/jlib/jsp-el-api.jar -Dpackaging=jar -Durl=$REPO_URL
mvn deploy:deploy-file -DgroupId=$GROUP.jlib -DartifactId=oracle-el -Dversion=$VERSION -Dfile=$JDEV_HOME/jlib/oracle-el.jar -Dpackaging=jar -Durl=$REPO_URL

#  BC4J Runtime: Adding: ../doc/ohj/bc4jjavadoc.jar
#  BC4J Runtime: Adding: ../doc/ohj/adfsharejavadoc.jar
#  BC4J Runtime: Adding: ../../BC4J/src/adfmsrc.zip
#  BC4J Runtime: Adding: ../../BC4J/src/bc4jsrc.zip

# To add the library pom:
# mvn deploy:deploy-file -DpomFile=bc4j_runtime.pom -Dfile=bc4j_runtime.pom -Dpackaging=pom-Durl=file://Users/admin/maven2-repo
