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
	private static String JDEV_VERSION = "11.1.1.2.0";
	private static String JDEV_GROUP_BASE = "com.oracle.jdeveloper";
	private static String JDEV_HOME = "/home/nelson24/apps/jdev-11.1.1.2.0/jdeveloper";
	private static String POM_PATH = "target/scripts/poms";
	private static String SCRIPT_PATH = "target/scripts";

	// For Maven 2
	private static String REPO_URL = "https://nexus/content/repositories/thirdparty";
	private static String REPOSITORY_ID = "nexus-thirdparty";

	// For Maven 1 - default is for sqa maven repo
	private static String REPO_ROOT = "johndoe@maven1-host:/opt/local/maven-siteroot/maven/bc4j/jars";
	
	// Copy command for maven 1 - default is scp
	private static final String COPY_CMD="scp";
	
	public static void main(String[] args) {
		JarLoader me = new JarLoader();
		me.processAll();
	}

	public void processAll() {
		File scriptsDir = new File(SCRIPT_PATH);
		if (!scriptsDir.exists()) {
			scriptsDir.mkdirs();
		}
		Collection<JarLibrary> libs = getLibraries(JDEV_HOME + "/jdev/extensions");
		// Generate a list of unique JARs
		HashMap<JarDef, Integer> jarList = buildJarList(libs);
		spewJarList(jarList);
		writeMavenDeployJarsScript(jarList);
		writeMavenLibraryPoms(libs);
		writeMavenDeployLibraryPoms(libs);
		writeLegacyMavenCopyScript(jarList);
	}

	private void writeMavenDeployLibraryPoms(Collection<JarLibrary> libs) {
		FileWriter deployScript = null;
		try {
			deployScript = new FileWriter(SCRIPT_PATH + "/deploy-adf-poms.sh");
			deployScript.append("#!/bin/bash\n");
			deployScript.append("POM_PATH=" + new File(POM_PATH).getAbsolutePath() + "\n");
			deployScript.append("REPO_URL=" + REPO_URL + "\n");
			deployScript.append("REPOSITORY_ID=" + REPOSITORY_ID + "\n");

			for (JarLibrary lib : libs) {
				deployScript
						.append(String
								.format(
										"mvn deploy:deploy-file -DpomFile=$POM_PATH/%1$s -Dfile=$POM_PATH/%1$s -Dpackaging=pom -DrepositoryId=$REPOSITORY_ID -Durl=$REPO_URL\n",
										lib.getPomFileName()));
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

	private void writeMavenDeployJarsScript(HashMap<JarDef, Integer> jarList) {
		FileWriter deployScript = null;
		try {
			deployScript = new FileWriter(SCRIPT_PATH + "/deploy-adf-jars.sh");
			deployScript.append("#!/bin/bash\n");
			deployScript.append("GROUP_BASE=" + JDEV_GROUP_BASE + ".jars\n");
			deployScript.append("VERSION=" + JDEV_VERSION + "\n");
			deployScript.append("JDEV_HOME=" + JDEV_HOME + "\n");
			deployScript.append("REPO_URL=" + REPO_URL + "\n");
			deployScript.append("REPOSITORY_ID=" + REPOSITORY_ID + "\n");

			for (JarDef jar : jarList.keySet()) {
				// To get started, we're skipping the few odd ball jars.
				if (jar.getFilename().startsWith("../../")) {
					if (new File(JDEV_HOME + "/" + jar.getPathAndFilename()).exists()) {
						deployScript
								.append(String
										.format(
												"mvn deploy:deploy-file -DgroupId=$GROUP_BASE.%1$s -DartifactId=%2$s -Dversion=$VERSION -Dfile=$JDEV_HOME/%3$s -Dpackaging=jar -DrepositoryId=$REPOSITORY_ID -Durl=$REPO_URL\n",
												jar.getGroupId(), jar.getArtifactId(), jar.getPathAndFilename()));
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

	private void writeLegacyMavenCopyScript(HashMap<JarDef, Integer> jarList) {
		FileWriter deployScript = null;
		try {
			deployScript = new FileWriter(SCRIPT_PATH + "/deploy-jars-to-legacy-maven.sh");
			deployScript.append("#!/bin/bash\n");
			deployScript.append("VERSION=" + JDEV_VERSION + "\n");
			deployScript.append("JDEV_HOME=" + JDEV_HOME + "\n");
			deployScript.append("REPO_ROOT=" + REPO_ROOT + "\n");
			deployScript.append("COPY_CMD="+COPY_CMD +"\n");

			for (JarDef jar : jarList.keySet()) {
				// To get started, we're skipping the few odd ball jars.
				if (jar.getFilename().startsWith("../../")) {
					deployScript
							.append(String
									.format(
											"$COPY_CMD $JDEV_HOME/%1$s $REPO_ROOT/%2$s-$VERSION.jar\n",
											jar.getPathAndFilename(),jar.getArtifactId()));
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

	public Collection<JarLibrary> getLibraries(String path) {

		ArrayList<JarLibrary> libs = new ArrayList<JarLibrary>();

		File searchRoot = new File(path);
		File[] allFiles = searchRoot.listFiles();
		for (int i = 0; i < allFiles.length; i++) {
			File file = allFiles[i];
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
		return libs;
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
		d.addObjectCreate("extension/hooks/libraries/library", JarLibrary.class);
		d.addSetProperties("extension/hooks/libraries/library");
		d.addCallMethod("extension/hooks/libraries/library/classpath", "addJarFile", 0);
		d.addCallMethod("extension/hooks/libraries/library/srcpath", "addSrcFile", 0);
		d.addCallMethod("extension/hooks/libraries/library/docpath", "addDocFile", 0);
		d.addSetNext("extension/hooks/libraries/library", "add");
	}
}
