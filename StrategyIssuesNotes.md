# Introduction #

This is a collection of related notes to the goal of using Maven and ADF.

# 5/3/2013 Notes #
  * Added recording of MD5, SHA1 and Filename information in the generated comments in the library pom.xml files.  Makes it easier to find the "same" artifact in the maven central repo for comparison purposes.
  * Verified on 11.1.1.7.0, 11.1.2.3.0, 11.1.2.4.0

# 4/7/2012 Notes #
  * Added support to follow MANIFEST.MF Class-Path jar references.  For jars that have a manifest classpath, maven-adf will try to discover the jars and add them as dependencies to libraries.  This behavior is configurable via the usemanifestclasspaths configuration property.
  * Added comments to the generated pom files to help with explaining where the pom and associated dependencies came from in the jdeveloper installation
  * Fixed up the groupId string values to be more comprehensible.  The groupId should match the location in the jdeveloper install where the jdeveloper extension was discovered.
  * Verified on 10.1.3.5.0, 11.1.1.3.0, 11.1.1.4.0, 11.1.1.5.0, 11.1.1.6.0, 11.1.2.0.0, 11.1.2.1.0

# 5/11/2011 Notes #
  * Committed fixes to better detect "Oracle JDBC" library.
  * Added support for Maven Shell http://shell.sonatype.org/ for deployment using a single command execution.
  * Verified on 10.1.3.4.0, 10.1.3.5.0, 11.1.1.3.0, 11.1.1.4.0, 11.1.1.5.0

# Usage #


# Deploy ADF jars to maven #
> Use [JarLoader](http://code.google.com/p/maven-adf/source/browse/trunk/src/main/java/gov/llnl/JarLoader.java)
> > Fix ability to use SCP.  Find a FAST way to deploy these! Specifically, handle different user names on each end.
> > List missing ones
> > > adftags.jar


> Could Oracle enhance the JDeveloper ADF installer to install to Maven as a target?

# JAR and POM Naming #
> File name munging (removes spaces and parens) is required as Maven has tighter restrictions.
> Managing duplicate jar usage:  what happens when JDev is using commons-xxx and another component needs commons-yyy?
> > How do we know EXACTLY which versions are bundled with JDeveloper?
> > Current strategy is to copy all JARs from JDeveloper into repo with a version number matching JDeveloper itself.
> > This could now be resolved by using Nexus to search by MD5 and link directly to third party JARs/POMs.


# Runtime Issues #

> Re-address deploying ADF Runtime in the app instead of the container
> > Note: this affects what is defined as optional in the Library definitions.
> > We do this (deploy in the app) because we have many apps with different lifecycles.

# Building New Projects #

> Describe multi-module layout and how to generate using an arch-type

> IDE Integration
    * Netbeans has native integration to read the pom.xml files directly and use them
    * Use m2eclipse so that Eclipse has native integration to read the pom.xml files directly
    * JDeveloper use 'mvn jdev:jdev' http://myfaces.apache.org/trinidad/plugins/maven-jdev-plugin/project-info.html (The brand new maven2 integration for JDeveloper doesn't quite understand multi-module maven projects yet, so still using the trinidad plugin)