package antlr;

import java.io.Serializable;

/** A simple class to store some runtime parameters in visualage
 */
public class VAJANTLRParameters implements Serializable {
	private String grammarFile;
	private boolean debug;
	private boolean writeToFile;


	public VAJANTLRParameters(String file, boolean d, boolean w) {
		grammarFile = file;
		debug = d;
		writeToFile = w;
	}
	public boolean getDebug() {return debug;}
	public String getGrammarFile() {return grammarFile;}
	public static String getKey(String packageName) {
		return "antlr-vaj." + packageName;
	}
	public boolean getWriteToFile() {return writeToFile;}
}
