#!/bin/bash
VERSION=10.1.3.4.0
JDEV_HOME=/opt/jdev/jdev10134
REPO_ROOT=johnson30@sqa.llnl.gov:/opt/local/maven-siteroot/maven/bc4j/jars
COPY_CMD=scp
$COPY_CMD $JDEV_HOME/jlib/adf-faces-impl.jar $REPO_ROOT/adf-faces-impl-$VERSION.jar
$COPY_CMD $JDEV_HOME/jlib/adf-faces-api.jar $REPO_ROOT/adf-faces-api-$VERSION.jar
$COPY_CMD $JDEV_HOME/BC4J/jlib/adftags.jar $REPO_ROOT/adftags-$VERSION.jar
