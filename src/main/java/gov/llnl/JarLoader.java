package gov.llnl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

public class JarLoader {
	
	// These need to become command line arguments or properties.  For now, hard coded.
  private static String JDEV_VERSION = "11.1.1.5.0";
  private static String JDEV_HOME = "/opt/apps/jdeveloper/jdeveloper-11.1.1.5.0/jdeveloper";

	// For Maven 2
	private static String REPO_URL = "https://nexus/content/repositories/thirdparty";
	private static String REPOSITORY_ID = "nexus-thirdparty";

    // Customize these as you like.  The following values work just fine
    private static String JDEV_GROUP_BASE = "com.oracle.jdeveloper";
    private static String POM_PATH = "target/scripts/poms";
    private static String SCRIPT_PATH = "target/scripts";

	
	public static void main(String[] args) {
		JarLoader me = new JarLoader();
		me.processAll();
	}

	public void processAll() {
		File scriptsDir = new File(SCRIPT_PATH);
		if (!scriptsDir.exists()) {
			scriptsDir.mkdirs();
		}
		Collection<JarLibrary> libs = new ArrayList<JarLibrary>();
		getLibraries(libs, JDEV_HOME);
		
		// Generate a list of unique JARs
		HashMap<JarDef, Integer> jarList = buildJarList(libs);
		spewJarList(jarList);
		writeMavenDeployJarsScript(jarList);
		writeMavenLibraryPoms(libs);
		writeMavenDeployLibraryPoms(libs);
		writeMavenShellScript();
	}

	private void writeMavenDeployLibraryPoms(Collection<JarLibrary> libs) {
      File deployScriptFile = new File(SCRIPT_PATH + "/deploy-adf-poms.sh");
      if (deployScriptFile.exists()) {
        deployScriptFile.delete();
      }		
      FileWriter deployScript = null;
		try {
			deployScript = new FileWriter(deployScriptFile);
			deployScript.append("#!/bin/bash\n");
			
			String pomPath = new File(POM_PATH).getAbsolutePath();
			for (JarLibrary lib : libs) {
				deployScript.append(String.format(
				"mvn deploy:deploy-file -DpomFile=%1$s/%2$s -Dfile=%1$s/%2$s -Dpackaging=pom -DrepositoryId=%3$s -Durl=%4$s\n",
				pomPath, lib.getPomFileName(), REPOSITORY_ID, REPO_URL));
			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} finally {
			try {
				deployScript.flush();
				deployScript.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void writeMavenLibraryPoms(Collection<JarLibrary> libs) {
		File pomDir = new File(POM_PATH);
		if (!pomDir.exists()) {
			pomDir.mkdirs();
		}
		for (JarLibrary lib : libs) {
			FileWriter out = null;
			try {
				out = new FileWriter(POM_PATH + "/"+lib.getPomFileName());
				System.out.println("Creating pom for " + lib.getName());
				writePomBegin(lib, out);
				for (JarDef jar : lib.getJars()) {
					if (jar.getType() == JarDef.JAR && jar.getFilename().startsWith("../../")) {
						writeJarDep(jar, out);
					}
				}
				writePomEnd(lib, out);
			} catch (IOException e) {
				System.out.println("Error creating: " + lib.getName());
				System.out.println(e.getMessage());
			} finally {
				try {
					out.flush();
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private void writePomBegin(JarLibrary lib, FileWriter out) throws IOException {
		out.append("<project>\n");
		out.append("  <modelVersion>4.0.0</modelVersion>\n");
		out.append("  <groupId>" + JDEV_GROUP_BASE + ".library</groupId>\n");
		out.append("  <artifactId>" + lib.getArtifactId() + "</artifactId>\n");
		out.append("  <version>" + JDEV_VERSION + "</version>\n");
		out.append("  <dependencies>\n");
	}

	private void writeJarDep(JarDef jar, FileWriter out) throws IOException {
		boolean exists = false;
		if (new File(JDEV_HOME + "/" + jar.getPathAndFilename()).exists()) {
			exists = true;
		}
		if (!exists) {
		    System.err.println("Jar not found: " + new File(JDEV_HOME + "/" + jar.getPathAndFilename()).getAbsolutePath());
			out.append("<!-- No jar file found, but dependency was found -->\n");
			out.append("<!--\n");
		}
		out.append("    <dependency>\n");
		out.append("      <groupId>" + JDEV_GROUP_BASE + ".jars." + jar.getGroupId() + "</groupId>\n");
		out.append("      <artifactId>" + jar.getArtifactId() + "</artifactId>\n");
		out.append("      <version>" + JDEV_VERSION + "</version>\n");
		out.append("    </dependency>\n");
		if (!exists) {
			out.append("-->\n");
		}
	}

	private void writePomEnd(JarLibrary lib, FileWriter out) throws IOException {
		out.append("  </dependencies>\n");
		out.append("</project>\n");
	}

	private void writeMavenShellScript() {
	  File mavenShellScript = new File(SCRIPT_PATH + "/deploy-mvnsh.sh");
	  if (mavenShellScript.exists()) {
	    mavenShellScript.delete();
	  }
      FileWriter deployScript = null;
      try {
          deployScript = new FileWriter(mavenShellScript);
          deployScript.append("#!/bin/bash\n");
          deployScript.append("# If you have the \"maven shell\" http://shell.sonatype.org/index.html installed, just run this script, and it will do everything\n\n");
          deployScript.append("mvnsh source deploy-adf-jars.sh\n");
          deployScript.append("mvnsh source deploy-adf-poms.sh\n");
      } catch (IOException e) {
          System.out.println("Error writing deploy script: " + e.getMessage());
      } finally {
          try {
              deployScript.close();
          } catch (IOException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
          }
      }
	}
	
	private void writeMavenDeployJarsScript(HashMap<JarDef, Integer> jarList) {
	    File deployScriptFile = new File(SCRIPT_PATH + "/deploy-adf-jars.sh");
	    if (deployScriptFile.exists()) {
	      deployScriptFile.delete();
	    }
		FileWriter deployScript = null;
		try {
			deployScript = new FileWriter(deployScriptFile);
			deployScript.append("#!/bin/bash\n");

			for (JarDef jar : jarList.keySet()) {
				// To get started, we're skipping the few odd ball jars.
				if (jar.getFilename().startsWith("../../")) {
					if (new File(JDEV_HOME + "/" + jar.getPathAndFilename()).exists()) {
						deployScript.append(String.format(
						"mvn deploy:deploy-file -DgroupId=%1$s.jars.%2$s -DartifactId=%3$s -Dversion=%4$s -Dfile=%5$s/%6$s -Dpackaging=jar -DrepositoryId=%7$s -Durl=%8$s\n",
						JDEV_GROUP_BASE, jar.getGroupId(), jar.getArtifactId(), JDEV_VERSION, JDEV_HOME, jar.getPathAndFilename(), REPOSITORY_ID, REPO_URL));
					} else {
						System.out.println("File: " + jar.getPathAndFilename() + " does not exist.");
					}
				}
			}
		} catch (IOException e) {
			System.out.println("Error writing deploy script: " + e.getMessage());
		} finally {
			try {
				deployScript.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


	private HashMap<JarDef, Integer> buildJarList(Collection<JarLibrary> libs) {
		HashMap<JarDef, Integer> jars = new HashMap<JarDef, Integer>(50);
		for (JarLibrary lib : libs) {
			Integer count;
			for (JarDef jar : lib.getJars()) {
				if (jars.containsKey(jar)) {
					count = jars.get(jar) + 1;
				} else {
					count = new Integer(1);
				}
				if (jar.getType() == JarDef.JAR) {
					jars.put(jar, count);
				}
			}
		}
		return jars;
	}

	private void spewJarList(HashMap<JarDef, Integer> jars) {
		for (Object element : jars.entrySet()) {
			System.out.println(element);
		}
	}

	public void getLibraries(Collection<JarLibrary> libs, String path) {

		File searchRoot = new File(path);
		if (!searchRoot.exists()) {
		  System.err.println("Directory does not exist: " + path);
		} else {
    		File[] allFiles = searchRoot.listFiles();
    		if (allFiles == null) {
    		  throw new NullPointerException("Permissions problem accessing: " + searchRoot.getAbsolutePath());
    		} else {
        		for (int i = 0; i < allFiles.length; i++) {
        			File file = allFiles[i];
        			if (file.isDirectory()) {
        			  getLibraries(libs, file.getAbsolutePath());
        			} else {
            			if (file.getName().endsWith("jar")) {
            				System.out.println("Processing: " + file.getName());
            				JarFile jarfile;
            				try {
            					jarfile = new JarFile(file);
            					Digester digester = new Digester();
            					addRules(digester);
            					digester.push(libs);
            					getJDevExtensionXml(jarfile, digester);
            				} catch (IOException e) {
            					// TODO Auto-generated catch block
            					System.out.println("Not really a jar: " + file.getName());
            					System.out.println(e.getMessage());
            				}
            			}
        			}
        		}
    		}
		}
	}

	public JarLibrary getJDevExtensionXml(JarFile jarfile, Digester digester) {
		JarEntry jarEntry = jarfile.getJarEntry("META-INF/extension.xml");
		if (jarEntry == null) {
			System.out.println("No extension.xml found");
		} else {
			InputStream is;
			try {
				is = jarfile.getInputStream(jarEntry);
				digester.parse(is);
				is = jarfile.getInputStream(jarEntry);
				// debugThing(is);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

	private void addRules(Digester d) {
		// d.addBeanPropertySetter("extension/hooks/libraries");

      d.addObjectCreate("*/libraries/library", JarLibrary.class);
      d.addSetProperties("*/libraries/library");
      d.addCallMethod("*/libraries/library/classpath", "addJarFile", 0);
      d.addCallMethod("*/libraries/library/srcpath", "addSrcFile", 0);
      d.addCallMethod("*/libraries/library/docpath", "addDocFile", 0);
      d.addSetNext("*/libraries/library", "add");

	}
	
	
}
