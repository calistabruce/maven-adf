package gov.llnl;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JarLibrary {
	
	private String name;
	private String xmlns;
	private ArrayList<JarDef> jars = new ArrayList<JarDef>(10);
    
    /* Used to convert JDeveloper library names to something Maven and the shel environment can easily handle.
     * Only problems so far have been spaces and parentheses. 
     */
    private static final String INVALID_CHARS = "[ ()/]";
    private static final Pattern INVALID_CHARS_PATTERN = Pattern.compile(INVALID_CHARS);

	
	public ArrayList<JarDef> getJars() {
		return jars;
	}
	
	public void setJars(ArrayList<JarDef> jars) {
		this.jars = jars;
	}
	
	public void addFile(JarDef jarDef) {
		System.out.println("  "+getName()+": Adding: "+jarDef.toString());
		getJars().add(jarDef);
	}
	
	public void addJarFile(String path) {
		addFile(new JarDef(path, JarDef.JAR));
	}
	
	public void addSrcFile(String path) {
		addFile(new JarDef(path, JarDef.SRC));
	}
	
	public void addDocFile(String path) {
		addFile(new JarDef(path, JarDef.DOC));
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
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
		return getArtifactId()+".pom";
	}
}
