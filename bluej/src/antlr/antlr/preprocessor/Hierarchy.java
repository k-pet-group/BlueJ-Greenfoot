package antlr.preprocessor;

import antlr.collections.impl.IndexedVector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.io.*;
import antlr.*;
import antlr.preprocessor.Grammar;

public class Hierarchy {
	protected static Grammar LexerRoot = new Grammar("Lexer", null, null);
	protected static Grammar ParserRoot = new Grammar("Parser", null, null);
	protected static Grammar TreeParserRoot = new Grammar("TreeParser", null, null);
	protected Hashtable symbols;	// table of grammars
	protected Hashtable files;	// table of grammar files read in

	public Hierarchy() {
		symbols = new Hashtable(10);
		files = new Hashtable(10);
		
		LexerRoot.setPredefined(true);
		ParserRoot.setPredefined(true);
		TreeParserRoot.setPredefined(true);
		
		symbols.put(LexerRoot.getName(), LexerRoot);
		symbols.put(ParserRoot.getName(), ParserRoot);
		symbols.put(TreeParserRoot.getName(), TreeParserRoot);
	}
	public void addGrammar(Grammar gr) {
		gr.setHierarchy(this);
		// add grammar to hierarchy
		symbols.put(gr.getName(), gr);
		// add grammar to file.
		GrammarFile f = getFile(gr.getFileName());
		f.addGrammar(gr);
	}
	public void addGrammarFile(GrammarFile gf) {
		files.put(gf.getName(), gf);
	}
	public void expandGrammarsInFile(String fileName) {
		GrammarFile f = getFile(fileName);
		for (Enumeration e=f.getGrammars().elements(); e.hasMoreElements(); ) {
			Grammar g = (Grammar)e.nextElement();
			g.expandInPlace();
		}	
	}
	public Grammar findRoot(Grammar g) {
		if ( g.getSuperGrammarName()==null ) {		// at root
			return g;
		}
		// return root of super.
		Grammar sg = g.getSuperGrammar();
		if ( sg==null ) return g;		// return this grammar if super missing
		return findRoot(sg);
	}
	public GrammarFile getFile(String fileName) {
		return (GrammarFile)files.get(fileName);
	}
	public Grammar getGrammar(String gr) {
		return (Grammar)symbols.get(gr);
	}
	public static String optionsToString(IndexedVector options) {
		String s = "options {"+System.getProperty("line.separator");
		for (Enumeration e = options.elements() ; e.hasMoreElements() ;) {
			s += (Option)e.nextElement()+System.getProperty("line.separator");
		}
		s += "}"+
			System.getProperty("line.separator")+
			System.getProperty("line.separator");
		return s;
	}
	public void readGrammarFile(String file) throws FileNotFoundException {
		FileReader grStream = new FileReader(file);
		addGrammarFile(new GrammarFile(file));
		
		// Create the simplified grammar lexer/parser
		PreprocessorLexer ppLexer = new PreprocessorLexer(grStream);
		Preprocessor pp = new Preprocessor(ppLexer);

		// populate the hierarchy with class(es) read in
		try {
			pp.grammarFile(this, file);
		}
		catch (ANTLRException se) {
			antlr.Tool.toolError("error reading grammar(s):"+se);
		}
		catch (IOException io) {
			antlr.Tool.toolError("IO error reading grammar(s):"+io);
		}
	}
	/** Return true if hierarchy is complete, false if not */
	public boolean verifyThatHierarchyIsComplete() {
		boolean complete = true;
		// Make a pass to ensure all grammars are defined
		for (Enumeration e = symbols.elements() ; e.hasMoreElements() ;) {
			Grammar c = (Grammar)e.nextElement();
			if ( c.getSuperGrammarName()==null ) {
				continue;		// at root: ignore predefined roots
			}
			Grammar superG = c.getSuperGrammar();
			if ( superG == null ) {
				antlr.Tool.toolError("grammar "+c.getSuperGrammarName()+" not defined");
				complete = false;
				symbols.remove(c.getName()); // super not defined, kill sub
			}
		}
	
		if ( !complete ) return false;
		
		// Make another pass to set the 'type' field of each grammar
		// This makes it easy later to ask a grammar what its type
		// is w/o having to search hierarchy.
		for (Enumeration e = symbols.elements() ; e.hasMoreElements() ;) {
			Grammar c = (Grammar)e.nextElement();
			if ( c.getSuperGrammarName()==null ) {
				continue;		// ignore predefined roots
			}
			c.setType(findRoot(c).getName());
		}
		
		return true;
	}
}
