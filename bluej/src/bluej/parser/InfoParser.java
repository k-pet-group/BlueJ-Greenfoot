package bluej.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import antlr.Token;
import antlr.TokenStreamException;
import bluej.parser.ast.LocatableToken;
import bluej.parser.ast.gen.JavaTokenTypes;
import bluej.parser.symtab.ClassInfo;
import bluej.parser.symtab.Selection;

public class InfoParser extends NewParser
{
	private ClassInfo info;
	private int classLevel = 0; // number of nested classes
	private boolean gotTypeDef; // whether we just reach a type def
	private boolean isPublic;
	private boolean storeCurrentClassInfo;
	private String lastComment; // last (javadoc) comment text we saw
	
	private boolean gotExtends; // next type spec is the superclass/superinterfaces
	private boolean gotImplements; // next type spec(s) are interfaces
	
	private boolean hadError;
	
	private LocatableToken pkgLiteralToken;
	private List<LocatableToken> packageTokens;
	private LocatableToken pkgSemiToken;
	
	public InfoParser(Reader r) {
		super(r);
	}
	
	public static ClassInfo parse(File f, List<String> l) throws FileNotFoundException
	{
		FileInputStream fis = new FileInputStream(f);
		return parse(new InputStreamReader(fis));
	}
	
	public static ClassInfo parse(Reader r, List<String> l)
	{
		return parse(r);
	}
	
	public static ClassInfo parse(File f) throws FileNotFoundException
	{
		return parse(f, null);
	}
	
	public static ClassInfo parse(Reader r)
	{
		InfoParser infoParser = null;
		infoParser = new InfoParser(r);
		infoParser.parseCU();
		
		if (infoParser.info != null) {
			return infoParser.info;
		}
		else {
			throw new RuntimeException("Couldn't get class info");
		}
	}
	
	protected void error(String msg)
	{
		if (! hadError) {
			//try {
				super.error(msg);
			//}
			//catch (RuntimeException re) {
			//	re.printStackTrace(System.out);
				// throw re;
			//}
			hadError = true;
		}
		// Just try and recover.
	}
	
	public void parseTypeDef()
	{
		if (classLevel == 0) {
			gotTypeDef = true;
		}
		classLevel++;
		super.parseTypeDef();
		classLevel--;
		gotTypeDef = false;
	}
	
	public boolean parseTypeSpec(boolean speculative)
	{
		boolean r;
		if (! speculative) {
			try {
				if (isPrimitiveType(tokenStream.LA(1))) {
					return super.parseTypeSpec(speculative);
				}
			}
			catch (TokenStreamException tse) {}
			List<LocatableToken> ttokens = new LinkedList<LocatableToken>();
			r = parseTypeSpec(speculative, ttokens);
			if (r && info != null) {
				info.addUsed(getClassName(ttokens));
			}
		}
		else {
			r = super.parseTypeSpec(speculative);
		}
		return r;
	}
	
	protected void gotMethodDeclaration(LocatableToken token)
	{
		// DAV !
		//System.out.println("Saw method: " + token.getText());
	}
	
	protected void gotTypeDefName(LocatableToken nameToken)
	{
		gotExtends = false; // haven't seen "extends ..." yet
		if (classLevel == 1) {
			if (info == null || isPublic && !info.foundPublicClass()) {
				info = new ClassInfo();
				info.setName(nameToken.getText(), isPublic);
				info.setExtendsInsertSelection(new Selection(nameToken.getLine(), nameToken.getEndColumn()));
				if (pkgSemiToken != null) {
					info.setPackageSelections(getSelection(pkgLiteralToken), getSelection(packageTokens),
							getClassName(packageTokens), getSelection(pkgSemiToken));
				}
				storeCurrentClassInfo = true;
			} else {
				storeCurrentClassInfo = false;
			}
		}
		super.gotTypeDefName(nameToken);
	}
	
	protected void gotTypeDefExtends(LocatableToken extendsToken)
	{
		try {
			if (classLevel == 1 && storeCurrentClassInfo) {
				// info.setExtendsReplaceSelection(s)
				gotExtends = true;
				SourceLocation extendsStart = info.getExtendsInsertSelection().getStartLocation();
				int extendsEndCol = tokenStream.LA(1).getColumn();
				int extendsEndLine = tokenStream.LA(1).getLine();
				if (extendsStart.getLine() == extendsEndLine) {
					info.setExtendsReplaceSelection(new Selection(extendsEndLine, extendsStart.getColumn(), extendsEndCol - extendsStart.getColumn()));
				}
				else {
					info.setExtendsReplaceSelection(new Selection(extendsEndLine, extendsStart.getColumn(), extendsToken.getEndColumn() - extendsStart.getColumn()));
				}
				info.setExtendsInsertSelection(null);
			}
		}
		catch (TokenStreamException tse) {
			tse.printStackTrace(); // TODO
		}
	}
	
	protected void gotPackageStatement(LocatableToken token)
	{
		pkgLiteralToken = token;
	}
	
	protected void gotPackage(List<LocatableToken> pkgTokens)
	{
		packageTokens = pkgTokens;
	}
	
	protected void gotPackageSemi(LocatableToken token)
	{
		pkgSemiToken = token;
	}
	
	
	public boolean parseTypeSpec(boolean speculative, List<LocatableToken> ttokens)
	{
		boolean result = super.parseTypeSpec(speculative, ttokens);
		if (gotExtends) {
			// The list of tokens gives us the name of the class that we extend
			info.setSuperclass(getClassName(ttokens));
			Selection superClassSelection = getSelection(ttokens);
			info.setSuperReplaceSelection(superClassSelection);
			info.setImplementsInsertSelection(new Selection(superClassSelection.getEndLine(),
					superClassSelection.getEndColumn()));
			gotExtends = false;
		}
		return result;
	}
	
	protected void gotTypeDefImplements(LocatableToken implementsToken)
	{
		if (classLevel == 1 && storeCurrentClassInfo) {
			gotImplements = true;
		}
	}
	
	public List<LocatableToken> parseModifiers()
	{
		try {
			Token hiddenToken = tokenStream.LA(1).getHiddenBefore();
			if (hiddenToken != null && hiddenToken.getType() == JavaTokenTypes.ML_COMMENT) {
				lastComment = hiddenToken.getText();
				// DAV !
				//System.out.println("Saw comment: " + lastComment);
			}
		}
		catch (TokenStreamException tse) {}
		
		List<LocatableToken> rval = super.parseModifiers();
		if (gotTypeDef) {
			for (LocatableToken lt: rval) {
				if (lt.getType() == JavaTokenTypes.LITERAL_public) {
					isPublic = true;
				}
			}
			gotTypeDef = false;
		}
		return rval;
	}
	
	private Selection getSelection(LocatableToken token)
	{
		return new Selection(token.getLine(), token.getColumn(), token.getLength());
	}
	
	private Selection getSelection(List<LocatableToken> tokens)
	{
		Iterator<LocatableToken> i = tokens.iterator();
		Selection s = getSelection(i.next());
		if (i.hasNext()) {
			LocatableToken last = i.next();
			while (i.hasNext()) {
				last = i.next();
			}
			s.combineWith(getSelection(last));
		}
		return s;
	}
	
	
	/**
	 * Convert a list of tokens specifying a type, which may include type parameters, into a class name
	 * without type parameters.
	 */
	private String getClassName(List<LocatableToken> tokens)
	{
		String name = "";
		for (Iterator<LocatableToken> i = tokens.iterator(); i.hasNext(); ) {
			name += i.next().getText();
			if (i.hasNext()) {
				// there may be type parameters, array
				LocatableToken tok = i.next();
				if (tok.getType() == JavaTokenTypes.LT) {
					skipTypePars(i);
					if (!i.hasNext()) {
						return name;
					}
					tok = i.next(); // DOT
				}
				else if (tok.getType() == JavaTokenTypes.LBRACK) {
					return name;
				}
				name += ".";
			}
		}
		return name;
	}
	
	private void skipTypePars(Iterator<LocatableToken> i)
	{
		int level = 1;
		while (level > 0 && i.hasNext()) {
			LocatableToken tok = i.next();
			if (tok.getType() == JavaTokenTypes.LT) {
				level++;
			}
			else if (tok.getType() == JavaTokenTypes.GT) {
				level--;
			}
			else if (tok.getType() == JavaTokenTypes.SR) {
				level -= 2;
			}
			else if (tok.getType() == JavaTokenTypes.BSR) {
				level -= 3;
			}
		}
	}
}
