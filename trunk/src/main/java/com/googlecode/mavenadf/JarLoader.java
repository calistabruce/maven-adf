package com.googlecode.mavenadf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

public class JarLoader {
	
  private static final String HELP = "help";
  private static final String VERSION = "version";
  private static final String JDEVHOME = "jdevhome";
  private static final String REPOURL = "repourl";
  private static final String REPOID = "repoid";
  private static final String GROUPBASE = "groupbase";
  private static final String SCRIPTPATH = "scriptpath";
  private static final String CONFIG = "config";
  private static final String VERBOSE = "verbose";
  
  public static final String DEFAULT_GROUPBASE = "com.oracle.jdeveloper";
  private static final String DEFAULT_SCRIPTPATH = "target/scripts";
  
  private Map<String, String> props = new HashMap<String, String>();
  private static boolean verbose = false;
	
	public static void main(String[] args) {
		JarLoader loader = new JarLoader();
    System.out.println("Started: " + new Date().toString());
		boolean valid = loader.parseCommandLine(args);
		
		if (valid) {
		  loader.processAll();
		}
	  System.out.println("Finished: " + new Date().toString());
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

	public void processAll() {
	  
		File scriptsDir = new File(props.get(SCRIPTPATH));
		if (!scriptsDir.exists()) {
			scriptsDir.mkdirs();
		}
		Collection<JarLibrary> libs = new ArrayList<JarLibrary>();
		getLibraries(libs, props.get(JDEVHOME));
		
		// Generate a list of unique JARs
		HashMap<JarDef, Integer> jarList = buildJarList(libs);
		spewJarList(jarList);
		writeMavenDeployJarsScript(jarList);
		writeMavenLibraryPoms(libs);
		writeMavenDeployLibraryPoms(libs);
		writeMavenShellScript();
	}

	private void writeMavenDeployLibraryPoms(Collection<JarLibrary> libs) {
      File deployScriptFile = new File(props.get(SCRIPTPATH) + File.separator + "deploy-adf-poms.sh");
      if (deployScriptFile.exists()) {
        deployScriptFile.delete();
      }		
      FileWriter deployScript = null;
		try {
			deployScript = new FileWriter(deployScriptFile);
			deployScript.append("#!/bin/bash\n");
			
			String pomPath = new File(getPomPath()).getAbsolutePath();
			for (JarLibrary lib : libs) {
				deployScript.append(String.format(
				"mvn deploy:deploy-file -DpomFile=%1$s/%2$s -Dfile=%1$s/%2$s -Dpackaging=pom -DrepositoryId=%3$s -Durl=%4$s\n",
				pomPath, lib.getPomFileName(), props.get(REPOID), props.get(REPOURL)));
			}
		} catch (IOException e) {
		    e.printStackTrace();
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
					if (jar.getType() == JarDef.JAR && jar.getFilename().startsWith("../../")) {
						writeJarDep(lib, jar, out);
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
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private void writePomBegin(JarLibrary lib, FileWriter out) throws IOException {
		out.append("<project>\n");
		out.append("  <modelVersion>4.0.0</modelVersion>\n");
		out.append("  <groupId>" + props.get(GROUPBASE) + ".library</groupId>\n");
		out.append("  <artifactId>" + lib.getArtifactId() + "</artifactId>\n");
		out.append("  <version>" + props.get(VERSION) + "</version>\n");
		out.append("  <dependencies>\n");
	}

	private void writeJarDep(JarLibrary lib, JarDef jar, FileWriter out) throws IOException {
		boolean exists = false;
		if (new File(props.get(JDEVHOME) + File.separator + jar.getPathAndFilename()).exists()) {
			exists = true;
		}
		if (!exists) {
		    System.err.println("Jar not found for library " + lib.getName() + ": " + new File(props.get(JDEVHOME) + File.separator + jar.getPathAndFilename()).getAbsolutePath());
			out.append("<!-- No jar file found, but dependency was found -->\n");
			out.append("<!--\n");
		}
		out.append("    <dependency>\n");
		out.append("      <groupId>" + props.get(GROUPBASE) + ".jars." + jar.getGroupId() + "</groupId>\n");
		out.append("      <artifactId>" + jar.getArtifactId() + "</artifactId>\n");
		out.append("      <version>" + props.get(VERSION) + "</version>\n");
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
	  File mavenShellScript = new File(props.get(SCRIPTPATH) + File.separator + "deploy-mvnsh.sh");
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
          System.err.println("Error writing deploy script: " + e.getMessage());
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
	    File deployScriptFile = new File(props.get(SCRIPTPATH) + File.separator + "deploy-adf-jars.sh");
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
					if (new File(props.get(JDEVHOME) + "/" + jar.getPathAndFilename()).exists()) {
						deployScript.append(String.format(
						"mvn deploy:deploy-file -DgroupId=%1$s.jars.%2$s -DartifactId=%3$s -Dversion=%4$s -Dfile=%5$s/%6$s -Dpackaging=jar -DrepositoryId=%7$s -Durl=%8$s\n",
						props.get(GROUPBASE), jar.getGroupId(), jar.getArtifactId(), props.get(VERSION), props.get(JDEVHOME), jar.getPathAndFilename(), props.get(REPOID), props.get(REPOURL)));
					} else {
            if (isVerbose()) {
  						System.err.println("Jar not found: " + props.get(JDEVHOME) + File.separator + jar.getPathAndFilename());
            }
					}
        } else {
          if (isVerbose()) {
            System.out.println("Skipping jar: " + props.get(JDEVHOME) + "/" + jar.getPathAndFilename());
          }
        }
			}
		} catch (IOException e) {
			System.err.println("Error writing deploy script: " + e.getMessage());
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
	  if (isVerbose()) {
		for (Object element : jars.entrySet()) {
			System.out.println(element);
		}
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
               			  if (isVerbose()) {
               				System.out.println("Processing: " + file.getName());
               			  }
          					Digester digester = new Digester();
          					addRules(digester);
          					digester.push(libs);
          					getJDevExtensionXml(file, digester);
            				
            			}
        			}
        		}
    		}
		}
	}

	public void getJDevExtensionXml(File file, Digester digester) {
	    JarFile jarfile = null;
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
		return;
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

  public static void setVerbose(boolean vb) {
    verbose = vb;
  }

  public static boolean isVerbose() {
    return verbose;
  }

}
