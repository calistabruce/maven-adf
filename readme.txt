To facilitate using Maven with ADF projects, the first requirement is having JARs available in a Maven repository.

This utility is a quick and dirty implementation for extracting JARs and their dependencies as defined by JDeveloper Libraries as Maven poms.

The strategy is to generate pom.xml files and scripts that deploy pom's and JARs to a Maven repository.

(There is some code in here to generate library poms for maven 1, but we don't use it anymore.  It's left in for reference in case someone needs it.)

This has been in use successfully since JDeveloper 10.1.3.

HOWTO:

1) Edit the JarLoader.java file to change the constants to match your environment:
  private static String JDEV_VERSION = "11.1.1.2.0";
  private static String JDEV_HOME = "/opt/jdev/jdev-11.1.1.2.0/jdeveloper";
  private static String REPO_URL = "https://your.nexus.repo/content/repositories/thirdparty";
  private static String REPOSITORY_ID = "nexus-thirdparty";
  
2) Run this command to generate the scripts:
$ mvn clean compile exec:java
  
3) Make sure your ~/.m2/settings.xml is configured with a repository id matching the REPOSITORY_ID in step 1.
(we use maven encrypted passwords to store the nexus repository password: http://maven.apache.org/guides/mini/guide-encryption.html )
 <servers>
    <server>
      <id>nexus-thirdparty</id>
      <username>userid</username>
      <password>{23zlsdfasdf;alsdkfjasdfa/i/JTU=}</password>
    </server>
  </servers>

4) Run the generated shell scripts to populate your maven repository
$ cd target/scripts
$ ./deploy-adf-jars.sh
$ ./deploy-adf-poms.sh

5) Now you can add jdev library references to your projects.  We use the trinidad jdev plugin to generate .jpr files.  
So, here's an example pom fragment showing the use of one of the generated library poms.  Notice that we have converted 
weird characters (like space, /, $, etc) to _ characters in the library poms generated.

    .....
    <properties>
      <jdev.release>11.1.1.2.0</jdev.release>
    </properties>
    .....
    <plugins>
      <plugin>
        <groupId>org.apache.myfaces.trinidadbuild</groupId>
        <artifactId>maven-jdev-plugin</artifactId>
        <inherited>true</inherited>
        <configuration>
          <libraries>
            <library>JSF 1.2</library>
            <library>ADF Faces Runtime 11</library>
          </libraries>
        </configuration>
      </plugin>
    </plugins>
     ......
    <dependencies>
     <dependency>
      <groupId>com.oracle.jdeveloper.library</groupId>
      <artifactId>ADF_Faces_Runtime_11</artifactId>
      <version>${jdev.release}</version>
      <type>pom</type>
      <scope>provided</scope>
     </dependency>
     <dependency>
      <groupId>com.oracle.jdeveloper.library</groupId>
      <artifactId>JSF_1.2</artifactId>
      <version>${jdev.release}</version>
      <type>pom</type>
      <scope>provided</scope>
     </dependency>
    </dependencies>