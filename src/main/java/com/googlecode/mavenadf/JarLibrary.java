package com.googlecode.mavenadf;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JarLibrary {

  private File libraryFile;
  private String name;
  private String xmlns;
  private String extensionId;
  private String extensionVersion;
  private String deployed;

  private ArrayList<JarDef> jars = new ArrayList<JarDef>(10);

  /*
   * Used to convert JDeveloper library names to something Maven and the shell
   * environment can easily handle. Only problems so far have been spaces and
   * parentheses.
   */
  private static final String INVALID_CHARS = "[ ()/]";
  private static final Pattern INVALID_CHARS_PATTERN = Pattern.compile(INVALID_CHARS);

  public ArrayList<JarDef> getJars() {
    return jars;
  }

  public void setJars(ArrayList<JarDef> jars) {
    this.jars = jars;
  }

  public boolean addFile(JarDef jar) {
    for (JarDef jarDef : getJars()) {
      if (jarDef.getFilename().equals(jar.getFilename())) {
        // already have this jar in our collection
        return false;
      }
    }

    if (JarLoader.isVerbose()) {
      if (jar.exists()) {
        System.out.println("  " + getName() + ": Adding " + (jar.getType() == JarDef.MANIFEST ? "(manifest) " : "") + jar.toString());
      } else {
        System.out.println("  " + getName() + ": Adding: " + jar.toString() + " (does not exist on filesystem)");
      }
    }
    getJars().add(jar);
    return true;
  }

  public void addJarFile(String path) {
    privAddJarFile(path, JarDef.JAR);
  }

  private void privAddJarFile(String filepath, int type) {
    JarDef jar = new JarDef(this, filepath, type);
    if (addFile(jar)) {
      if (JarLoader.props.get(JarLoader.USEMANIFESTCLASSPATHS).equalsIgnoreCase("true")) {
        File file = new File(jar.getFilename());
        if (file.exists() && file.isFile()) {
          try {
            JarFile jarFile = new JarFile(jar.getFilename());
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
              Attributes attributes = manifest.getMainAttributes();
              jar.setManifestAttributes(attributes);
              String classpath = attributes.getValue("Class-Path");
              if (classpath != null && !"".equals(classpath.trim())) {
                if (JarLoader.isVerbose()) {
                  System.out.println("    Manifest classpath for Library: " + jar.getLibrary().getName() + " \n     jarfile: " + jar.getFilename()
                      + " \n     manifest: " + classpath);
                }
                StringTokenizer st = new StringTokenizer(classpath, " ");
                String basePath = ".";
                if (jar.getFilename().lastIndexOf("/") >= 0) {
                  basePath = jar.getFilename().substring(0, jar.getFilename().lastIndexOf("/"));
                }
                while (st.hasMoreTokens()) {
                  String manifestjarfile = st.nextToken();
                  if (manifestjarfile.endsWith(".jar")) {
                    privAddJarFile(basePath + "/" + manifestjarfile, JarDef.MANIFEST);
                  }
                }
              }
            }
          } catch (Throwable t) {
            t.printStackTrace();
          }

        }
      }
    }
  }

  public void addSrcFile(String path) {
    addFile(new JarDef(this, path, JarDef.SRC));
  }

  public void addDocFile(String path) {
    addFile(new JarDef(this, path, JarDef.DOC));
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    if (this.name != null) {
      System.err.println("Renaming: " + this.name + " to: " + name);
    }
    if ("${COHERENCE_RUNTIME_LIB_NAME}".equals(name)) {
      this.name = "Coherence Runtime";
    } else {
      this.name = name;
    }
  }

  public String getXmlns() {
    return xmlns;
  }

  public void setXmlns(String xmlns) {
    this.xmlns = xmlns;
  }

  // Convert name into something valid for Maven.
  public String getArtifactId() {
    Matcher libNameMatcher = INVALID_CHARS_PATTERN.matcher(getName());
    return libNameMatcher.replaceAll("_");
  }

  public String getPomFileName() {
    return getArtifactId() + ".pom";
  }

  public File getLibraryFile() {
    return libraryFile;
  }

  public void setLibraryFile(File libraryFile) {
    this.libraryFile = libraryFile;
  }

  public Map<String, String> getProps() {
    return JarLoader.props;
  }

  public String getExtensionId() {
    return extensionId;
  }

  public void setExtensionId(String extensionId) {
    this.extensionId = extensionId;
  }

  public String getExtensionVersion() {
    return extensionVersion;
  }

  public void setExtensionVersion(String extensionVersion) {
    this.extensionVersion = extensionVersion;
  }

  public String getDeployed() {
    return deployed;
  }

  public void setDeployed(String deployed) {
    this.deployed = deployed;
  }

}
