To facilitate using Maven with ADF projects, the first requirement is having JARs available in a Maven repository.

This utility is a quick and dirty implementation for extracting JARs and their dependencies as defined by JDeveloper Libraries as Maven poms.

The strategy is to generate pom.xml files and scripts that deploy pom's and JARs to a Maven repository.

This has been in use successfully since JDeveloper 10.1.3.

HOWTO:

1) Edit sample.properties to match your environment
  
2) Run this command to generate the scripts (assuming you have maven shell installed):
$ mvn clean package exec:java -Dexec.args="-config sample.properties"
OR..  for verbose output:
$ mvn clean package exec:java -Dexec.args="-config sample.properties -verbose"

  
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
$ ./deploy-mvnsh.sh

5) Now you can add jdev library references to your projects.  We use the trinidad jdev plugin to generate .jpr files.  
So, here's an example pom fragment showing the use of one of the generated library poms.  Notice that we have converted 
weird characters (like space, /, $, etc) to _ characters in the library poms generated.

    .....
    <properties>
      <jdev.release>11.1.1.5.0</jdev.release>
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