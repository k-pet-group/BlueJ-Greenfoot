package antlr.preprocessor;

/** Stores header action, grammar preamble, file options, and
 *  list of grammars in the file
 */

import antlr.collections.impl.IndexedVector;
import java.util.Enumeration;
import java.io.*;

public class GrammarFile {
	protected String fileName;
	protected String headerAction;
	protected IndexedVector options;
	protected IndexedVector grammars;
	protected boolean expanded = false;	// any grammars expanded within?

	public GrammarFile(String f) {
		fileName = f;
		grammars = new IndexedVector();
	}
	public void addGrammar(Grammar g) {
		grammars.appendElement(g.getName(), g);
	}
	public void generateExpandedFile() throws IOException {
		if ( !expanded ) {
			return;	// don't generate if nothing got expanded
		}	
		String expandedFileName = nameForExpandedGrammarFile(this.getName());
		
		// create the new grammar file with expanded grammars
		PrintWriter expF = antlr.Tool.openOutputFile(expandedFileName);
		expF.println(toString());
		expF.close();
	}
	public IndexedVector getGrammars() {
		return grammars;
	}
	public String getName() {return fileName;}
	public String nameForExpandedGrammarFile(String f) {
		if ( expanded ) {
			// strip path to original input, make expanded file in current dir
			return "expanded"+antlr.Tool.fileMinusPath(f);	
		}
		else {
			return f;
		}		
	}
	public void setExpanded(boolean exp) {
		expanded = exp;
	}
	public void setHeaderAction(String a) {headerAction=a;}
	public void setOptions(IndexedVector o) {options=o;}
	public String toString() {
		String h = headerAction==null ? "" : headerAction;
		String o = options==null ? "" : Hierarchy.optionsToString(options);
		
		String s=h+o;
		for (Enumeration e=grammars.elements(); e.hasMoreElements(); ) {
			Grammar g = (Grammar)e.nextElement();
			s += g;
		}	
		return s;
	}
}
