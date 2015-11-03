To facilitate using Maven with ADF projects, the first requirement is having JARs available in a Maven repository.

This utility is a quick and dirty implementation for extracting JARs and their dependencies as defined by JDeveloper Libraries as Maven poms.

The strategy is to generate pom.xml files and scripts that deploy pom's and JARs to a Maven repository.

This has been in use successfully since JDeveloper 10.1.3.