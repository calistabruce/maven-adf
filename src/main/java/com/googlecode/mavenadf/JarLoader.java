package com.googlecode.mavenadf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

public class JarLoader {
	
  public static final String JDEVHOME = "jdevhome";
  public static final String USEMANIFESTCLASSPATHS = "usemanifestclasspaths";
  
  private static final String HELP = "help";
  private static final String VERSION = "version";
  private static final String REPOURL = "repourl";
  private static final String REPOID = "repoid";
  private static final String GROUPBASE = "groupbase";
  private static final String SCRIPTPATH = "scriptpath";
  private static final String CONFIG = "config";
  private static final String VERBOSE = "verbose";
  
  public static final String DEFAULT_GROUPBASE = "com.oracle.jdeveloper";
  private static final String DEFAULT_SCRIPTPATH = "target/scripts";
  
  public static Map<String, String> props = new HashMap<String, String>();
  private static boolean verbose = false;
  
  private static File currentFile = null;
  
  private List<JarLibrary> libs = null;
  
  private String id = null;
  private String version = null;
	
	public static void main(String[] args) {
		JarLoader loader = new JarLoader();
    System.out.println("Started: " + new Date().toString());
		boolean valid = loader.parseCommandLine(args);
		
		if (valid) {
		  loader.processAll();
		}
	  System.out.println("Finished: " + new Date().toString());
	  System.out.println("Now run either run the 'deploy-mvnsh' maven shell script or the 'deploy-adf-jars' and 'deploy-adf-poms' scripts generated in: "
        + new File(props.get(SCRIPTPATH)).getAbsolutePath() + " to populate your maven repository.");
	}
	
	public boolean parseCommandLine(String[] args)  {
	  CommandLineParser parser = new PosixParser();
	  Options options = new Options();
	  options.addOption(HELP, false, "print this message");
	  options.addOption(VERBOSE, false, "Verbose Output");
	  options.addOption(VERSION, true, "JDeveloper Version");
	  options.addOption(JDEVHOME, true, "JDeveloper Home");
	  options.addOption(REPOURL, true, "Repository URL");
	  options.addOption(REPOID, true, "Repository ID");
	  options.addOption(GROUPBASE, true, "groupId base");
	  options.addOption(SCRIPTPATH, true, "Script Path for scripts to be written");
	  options.addOption(CONFIG, true, "Config File path");
	  options.addOption(USEMANIFESTCLASSPATHS, true, "Generate dependencies for manifest classpaths found");
	  
	  try {
	    CommandLine cmd = parser.parse(options, args);
	    if (cmd.hasOption(VERBOSE)) {
	      setVerbose(true);
	    }
	    if (cmd.hasOption(HELP)) {
	      showHelp(options);
	      return false;
	    } else if (cmd.hasOption(CONFIG)) {
	      String filePath = cmd.getOptionValue(CONFIG);
	      File f = new File(filePath);
	      if (f.exists()) {
	        InputStream is = new FileInputStream(f);
	        Properties p = new Properties();
	        p.load(is);
	        is.close();
	        for (Object key : p.keySet()) {
	          String keyStr = (String)key;
	          props.put(keyStr, (String)p.getProperty(keyStr));
	        }
	        return validateProperties(options);
	      } else {
	        System.err.println("Could not load properties file: " + filePath);
	        return false;
	      }
	    } else {
	      String[] propVals = new String[] {VERSION, JDEVHOME, REPOURL, REPOID};
	      for (String propVal : propVals) {
    	      if (cmd.hasOption(propVal)) {
    	        props.put(propVal, cmd.getOptionValue(propVal));
    	      } else {
    	        System.err.println(propVal + " required");
    	        showHelp(options);
    	        return false;
    	      }
	      }
	      props.put(VERSION, cmd.getOptionValue(VERSION, null));
	      props.put(JDEVHOME, cmd.getOptionValue(JDEVHOME, null));
	      props.put(REPOURL, cmd.getOptionValue(REPOURL, null));
	      props.put(REPOID, cmd.getOptionValue(REPOID, null));
	      props.put(GROUPBASE, cmd.getOptionValue(GROUPBASE, DEFAULT_GROUPBASE));
	      props.put(SCRIPTPATH, cmd.getOptionValue(SCRIPTPATH, DEFAULT_SCRIPTPATH));
	      props.put(USEMANIFESTCLASSPATHS, cmd.getOptionValue(USEMANIFESTCLASSPATHS, "false"));
	    }
	    return validateProperties(options);
	  } catch (ParseException e) {
	    e.printStackTrace();
        return false;
      } catch (IOException e) {
        e.printStackTrace();
        return false;
      }
	  
	}
	
	private String getPomPath() {
	  return props.get(SCRIPTPATH) + File.separator + "poms";
	}
	
	private boolean validateProperties(Options options) {
	  boolean isValid = true;
      String[] propKeys = new String[] {VERSION, JDEVHOME, REPOURL, REPOID, GROUPBASE, SCRIPTPATH};
      for (String propKey : propKeys) {
        String propVal = (String)props.get(propKey);
        if (propVal == null || "".equals(propVal.trim())) {
            System.err.println(propVal + " required");
            isValid = false;
          }
      }
      if (isValid) {
        return true;
      } else {
        showHelp(options);
        return false;
      }
	}
	
	private void showHelp(Options options) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("mavenadf", options);
	}

	private List<JarLibrary> getJarLibs() {
		if (libs == null) {
			libs = new ArrayList<JarLibrary>();
		}
		return libs;
	}
	
	public void addLibrary(JarLibrary lib) {
		getJarLibs().add(lib);
		lib.setLibraryFile(getCurrentFile());
		lib.setExtensionVersion(getVersion());
		lib.setExtensionId(getId());
	}
	
	public void processAll() {

		System.out.println("Generating loader scripts for JDeveloper version: " + props.get(VERSION));
		System.out.println("Looking in JDeveloper Home: " + props.get(JDEVHOME));
		
		File scriptsDir = new File(props.get(SCRIPTPATH));
		if (!scriptsDir.exists()) {
			scriptsDir.mkdirs();
		}
		getLibraries(props.get(JDEVHOME));
		
		// Generate a list of unique JARs
		HashMap<JarDef, Integer> jarList = buildJarList(getJarLibs());
		// spewJarList(jarList);
		writeMavenDeployJarsScript(jarList);
		System.out.println("Generated " + jarList.size() + " jar artifacts.");
		writeMavenLibraryPoms(getJarLibs());
		System.out.println("Generated " + getJarLibs().size() + " library poms.");
		writeMavenDeployLibraryPoms(getJarLibs());
		writeMavenShellScript();
	}

	private void writeMavenDeployLibraryPoms(List<JarLibrary> libs) {
      File deployScriptFile = new File(props.get(SCRIPTPATH) + File.separator + "deploy-adf-poms.sh");
      if (deployScriptFile.exists()) {
        deployScriptFile.delete();
      }		
      FileWriter deployScript = null;
		try {
			deployScript = new FileWriter(deployScriptFile);
			deployScript.append("#!/bin/bash\n");
			
			String pomPath = new File(getPomPath()).getAbsolutePath();
			
			TreeSet<JarLibrary> sortedLibraries = new TreeSet<JarLibrary>(new Comparator<JarLibrary>() {
				public int compare(JarLibrary o1, JarLibrary o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});

			sortedLibraries.addAll(libs);
			
			for (JarLibrary lib : sortedLibraries) {
				String filename = new File(pomPath + "/" + lib.getPomFileName()).getCanonicalPath();
				deployScript.append(String.format("mvn deploy:deploy-file -DpomFile=%1$s -Dfile=%1$s -Dpackaging=pom -DrepositoryId=%2$s -Durl=%3$s\n", filename,
				props.get(REPOID), props.get(REPOURL)));
			}
		} catch (IOException e) {
		    e.printStackTrace();
		} finally {
			try {
				deployScript.flush();
				deployScript.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void writeMavenLibraryPoms(List<JarLibrary> libs) {
		File pomDir = new File(getPomPath());
		if (!pomDir.exists()) {
			pomDir.mkdirs();
		}
		for (JarLibrary lib : libs) {
			FileWriter out = null;
			try {
				out = new FileWriter(getPomPath() + File.separator +lib.getPomFileName());
				if (isVerbose()) {
				  System.out.println("Creating pom for " + lib.getName());
				}
				writePomBegin(lib, out);
				for (JarDef jar : lib.getJars()) {
					if ((jar.getType() == JarDef.JAR || jar.getType() == JarDef.MANIFEST)) {
						writeJarDep(lib, jar, out);
					} else {
						if (isVerbose()) {
							System.out.println("Lib: " + lib.getName() + " Skipping: " + jar.getFilename());
						}
					}
				}
				writePomEnd(lib, out);
			} catch (IOException e) {
				System.err.println("Error creating: " + lib.getName());
				System.err.println(e.getMessage());
			} finally {
				try {
					out.flush();
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void writePomBegin(JarLibrary lib, FileWriter out) throws IOException {
		String libPath = lib.getLibraryFile().getCanonicalPath().substring(props.get(JDEVHOME).length()+1);
		out.append("<project>\n");
		out.append("  <modelVersion>4.0.0</modelVersion>\n");
		out.append("  <groupId>" + props.get(GROUPBASE) + ".library</groupId>\n");
		out.append("  <artifactId>" + lib.getArtifactId() + "</artifactId>\n");
		String deployedByDefault = lib.getDeployed();
		if (deployedByDefault == null) {
			deployedByDefault = "false";
		}
		out.append("  <!-- JDeveloper library name: '" + lib.getName() + "' -->\n"); 
		out.append("  <!-- Deployed by default: " + deployedByDefault + "-->\n");
		out.append("  <version>" + props.get(VERSION) + "</version>\n");
		out.append("  <!-- This library pom was generated from ${JDEVHOME}/" + libPath.replaceAll("\\\\", "/") + "!META-INF/extension.xml -->\n");
		out.append("  <!-- Extension ID: '" + lib.getExtensionId() + "' -->\n");
		out.append("  <!-- Extension Version: '" +  lib.getExtensionVersion() + "' -->\n");
		out.append("  <dependencies>\n");
	}

	private void writeJarDep(JarLibrary lib, JarDef jar, FileWriter out) throws IOException {
		boolean exists = false;
		File jarFile = new File(jar.getFilename()); 
		if (jarFile.exists()) {
			exists = true;
		}
		if (exists) {
			out.append("    <dependency>\n");
			if (jar.getType() == JarDef.MANIFEST) {
				out.append("      <!-- This dependency is from a MANIFEST classpath reference -->\n");
			}
			out.append("      <groupId>" + props.get(GROUPBASE) + ".jars." + jar.getGroupId() + "</groupId>\n");
			out.append("      <artifactId>" + jar.getArtifactId() + "</artifactId>\n");
			out.append("      <version>" + props.get(VERSION) + "</version>\n");
			
			
			try {
				String jdevHomePath = new File(props.get(JDEVHOME) + "/..").getCanonicalPath();
				
				String filePath = "${JDEVHOME}/../" + jarFile.getAbsolutePath().substring(jdevHomePath.length()+1);
				
				out.append("      <!-- File from: '" + filePath + "' -->\n");
			} catch (Throwable t) {
				System.err.println("Error generating file location for: " + jarFile.getAbsolutePath());
			}
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(jarFile);
				String md5 = DigestUtils.md5Hex(fis);
				out.append("      <!-- MD5='" + md5 + "' -->\n");
				fis.close();
				fis = new FileInputStream(jarFile);
				String sha1 = DigestUtils.sha1Hex(fis);
				out.append("      <!-- SHA1='" + sha1 + "' -->\n");
				
				
				
			} finally {
				if (fis != null) {
					try { fis.close(); } catch (IOException e) {}
				}
			}
			
			writeManifestAttributes(jar, out);
					
			out.append("    </dependency>\n");
		} else {
			if (isVerbose()) {
				System.err.println("Jar not found for library " + lib.getName() + ": " + jar.getFilename());
			}
			if (jar.getType() == JarDef.MANIFEST) {
				out.append("    <!-- No jar file found, but dependency was found for " + jar.getArtifactId() + " -->\n");
				out.append("    <!--   This dependency is from a MANIFEST classpath reference -->\n");
			} else {
				out.append("    <!-- No jar file found, but dependency was found for " + jar.getArtifactId() + " -->\n");
			}
			out.append("    <!--\n");
			out.append("    <dependency>\n");
			out.append("      <groupId>" + props.get(GROUPBASE) + ".jars." + jar.getGroupId() + "</groupId>\n");
			out.append("      <artifactId>" + jar.getArtifactId() + "</artifactId>\n");
			out.append("      <version>" + props.get(VERSION) + "</version>\n");
			out.append("    </dependency>\n");
			out.append("    -->\n");
		}
	}

  private void writeManifestAttributes(JarDef jar, FileWriter out) throws IOException {
		Attributes manifestAttributes = jar.getManifestAttributes();
		
		if (manifestAttributes != null) {

			TreeSet<Object> sortedAttributes = new TreeSet<Object>(new Comparator<Object>() {
				
				public int compare(Object o1, Object o2) {
					return o1.toString().compareTo(o2.toString());
				}
			});
			sortedAttributes.addAll(manifestAttributes.keySet());

			out.append("      <!-- Manifest Info: -->\n");
			for (Object key : sortedAttributes) {
				String value = manifestAttributes.get(key).toString();
				if (key.toString() != null && value != null && !"".equals(value.trim())) {
					out.append("      <!--   "+ key.toString() + "=" + value + " -->\n");
				}
			}
		}
	}

	private void writePomEnd(JarLibrary lib, FileWriter out) throws IOException {
		out.append("  </dependencies>\n");
		out.append("</project>\n");
	}

	private void writeMavenShellScript() {
	  File mavenShellScript = new File(props.get(SCRIPTPATH) + File.separator + "deploy-mvnsh.sh");
	  if (mavenShellScript.exists()) {
	    mavenShellScript.delete();
	  }
      FileWriter deployScript = null;
      try {
          deployScript = new FileWriter(mavenShellScript);
          deployScript.append("#!/bin/bash\n");
      deployScript
          .append("# If you have the \"maven shell\" http://shell.sonatype.org/index.html installed, just run this script, and it will do everything\n\n");
          deployScript.append("mvnsh source deploy-adf-jars.sh\n");
          deployScript.append("mvnsh source deploy-adf-poms.sh\n");
      } catch (IOException e) {
          System.err.println("Error writing deploy script: " + e.getMessage());
      } finally {
          try {
              deployScript.close();
          } catch (IOException e) {
              e.printStackTrace();
          }
      }
	}
	
	private void writeMavenDeployJarsScript(HashMap<JarDef, Integer> jarList) {
	    File deployScriptFile = new File(props.get(SCRIPTPATH) + File.separator + "deploy-adf-jars.sh");
	    if (deployScriptFile.exists()) {
	      deployScriptFile.delete();
	    }
		FileWriter deployScript = null;
		try {
			deployScript = new FileWriter(deployScriptFile);
			deployScript.append("#!/bin/bash\n");

			TreeSet<JarDef> sortedJars = new TreeSet<JarDef>(new Comparator<JarDef>() {
				
				public int compare(JarDef o1, JarDef o2) {
					return o1.getFilename().compareTo(o2.getFilename());
				}
			});
			sortedJars.addAll(jarList.keySet());
			
			for (JarDef jar : sortedJars) {
				if (new File(jar.getFilename()).exists()) {
					deployScript.append(String.format(
	              "mvn deploy:deploy-file -DgroupId=%1$s.jars.%2$s -DartifactId=%3$s -Dversion=%4$s -Dfile=%5$s -Dpackaging=jar -DrepositoryId=%6$s -Durl=%7$s\n",
	              props.get(GROUPBASE), jar.getGroupId(), jar.getArtifactId(), props.get(VERSION), new File(jar.getFilename()).getCanonicalPath(),
	              props.get(REPOID), props.get(REPOURL)));
				} else {
		            if (isVerbose()) {
		            	System.err.println("Jar not found: " + jar.getFilename());
		            }
				}
			}
		} catch (IOException e) {
			System.err.println("Error writing deploy script: " + e.getMessage());
		} finally {
			try {
				deployScript.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


	private HashMap<JarDef, Integer> buildJarList(List<JarLibrary> libs) {
		HashMap<JarDef, Integer> jars = new HashMap<JarDef, Integer>(50);
		for (JarLibrary lib : libs) {
			Integer count;
			for (JarDef jar : lib.getJars()) {
				if (jars.containsKey(jar)) {
					count = jars.get(jar) + 1;
				} else {
					count = new Integer(1);
				}
				if (jar.getType() == JarDef.JAR || jar.getType() == JarDef.MANIFEST) {
					jars.put(jar, count);
				}
			}
		}
		return jars;
	}

	private void spewJarList(HashMap<JarDef, Integer> jars) {
	  if (isVerbose()) {
		for (Object element : jars.entrySet()) {
			System.out.println(element);
		}
	  }
	}

	public void getLibraries(String path) {

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
        			  getLibraries(file.getAbsolutePath());
        			} else {
            			if (file.getName().endsWith("jar")) {
               			  if (isVerbose()) {
               				System.out.println("Processing: " + file.getAbsolutePath());
               			  }
          					getJDevExtensionXml(file);
            				
            			}
        			}
        		}
    		}
		}
	}

	public void getJDevExtensionXml(File file) {
	    JarFile jarfile = null;
	    setCurrentFile(file);
	    try {
	      jarfile = new JarFile(file);
	    } catch (IOException e) {
          // TODO Auto-generated catch block
          System.err.println("Not really a jar: " + file.getName());
          System.err.println(e.getMessage());
          return;
      }	      
		JarEntry jarEntry = jarfile.getJarEntry("META-INF/extension.xml");
		if (jarEntry == null) {
		  if (isVerbose()) {
			System.out.println("No extension.xml found for: " + file.getAbsolutePath());
		  }
		} else {
			InputStream is;
			try {
				setId(null);
				setVersion(null);
				is = jarfile.getInputStream(jarEntry);
                Digester digester = new Digester();
                addRules(digester);
                digester.push(this);
				digester.parse(is);
				is.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			}
		}
		return;
	}

  /*
   * <extension id="oracle.adf.share.dt" version="11.1.1.5.37.60.13"
   * esdk-version="1.0" rsbundle-class="oracle.adf.share.dt.res.Bundle"
   * xmlns="http://jcp.org/jsr/198/extension-manifest"> ... <library
   * name="JPS Designtime">
   * <classpath>../../../oracle_common/modules/oracle.jps_11
   * .1.1/jps-ee.jar</classpath> </library> ...
*/
	  
	
	private void addRules(Digester d) {
	  //d.addBeanPropertySetter("extension/hooks/libraries");
		
		
	  d.addSetProperties("*/extension", "id", "id");
	  d.addSetProperties("*/extension", "version", "version");
	  d.addSetProperties("*/ex:extension", "id", "id");
	  d.addSetProperties("*/ex:extension", "version", "version");
      d.addObjectCreate("*/libraries/library", JarLibrary.class);
      d.addSetProperties("*/libraries/library");
      d.addCallMethod("*/libraries/library/classpath", "addJarFile", 0);
      d.addCallMethod("*/libraries/library/srcpath", "addSrcFile", 0);
      d.addCallMethod("*/libraries/library/docpath", "addDocFile", 0);
      d.addSetNext("*/libraries/library", "addLibrary");

	}

  public void setId(String id) {
	  this.id = id;
  }
  
  public String getId() {
	  return this.id;
  }
  
  public void setVersion(String version) {
	  this.version = version;
  }
  
  public String getVersion() {
	  return this.version;
  }
  
  public static File getCurrentFile() {
	  return currentFile;
  }
  
  public static void setCurrentFile(File file) {
	  currentFile = file;
  }
	
  public static void setVerbose(boolean vb) {
    verbose = vb;
  }

  public static boolean isVerbose() {
    return verbose;
  }

}
