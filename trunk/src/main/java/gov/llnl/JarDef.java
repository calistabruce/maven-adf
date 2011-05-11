package gov.llnl;

/*
 * JarDef represents a jar definition as extracted from Jdeveloper Library definitions.
 * 
 * Filename, as extracted, is the following format:
 * ../../{path}/{filename}.{ext}
 * 
 * The mapping to Maven repository becomes:
 * GroupId     com.oracle.jdeveloper.{path}
 * ArtifactId  {filename}
 * Version     All jars are given the current IDE version number.
 * 
 * Example:
 * ../../BC4J/lib/adfshare.jar
 * GroupId     com.oracle.jdeveloper.BC4J.lib
 * ArtifactId  adfshare
 * Version     10.1.3.0.4
 */
public class JarDef {
	
	private String  filename;
	private int type;
	
	public static int JAR=0;
	public static int SRC=1;
	public static int DOC=2;
	
	public JarDef(String path, int type) {
		this.setFilename(path);
		this.setType(type);
	}
	
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
	  if ("${jdbc.library}".equals(filename)) {
	    this.filename = "../../../wlserver_10.3/server/lib/ojdbc6.jar";
	  } else if ("${orai18n.library}".equals(filename)) {
	    this.filename = "../../../oracle_common/modules/oracle.nlsrtl_11.1.0/orai18n.jar";
	  } else {
		this.filename = filename;
	  }
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	
	public String toString() {
		return getFilename();
	}
	
	public String getPathAndFilename() {
		return getFilename().substring(6);
	}
	
	public String getGroupId() {
		String[] paths = getFilename().split("/");
		// TODO Make this more generic.
		String groupId = paths[2];
		for (int i = 3; i < paths.length -1; i++) {
			groupId += "."+paths[i];
		}
		// Convert any dots to underscores
		groupId=groupId.replace('.', '_');
		return groupId;
	}
	
	public String getArtifactId() {
		int lastSlash = getFilename().lastIndexOf('/')+1;
		int lastDot = getFilename().lastIndexOf(".");
		return getFilename().substring(lastSlash,lastDot);
	}
	
	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((filename == null) ? 0 : filename.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final JarDef other = (JarDef) obj;
		if (filename == null) {
			if (other.filename != null)
				return false;
		} else if (!filename.equals(other.filename))
			return false;
		return true;
	}

}
