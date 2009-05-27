package bluej.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

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
	
	private boolean gotExtends; // next type spec is the superclass/superinterfaces
	private boolean gotImplements; // next type spec(s) are interfaces
	
	private boolean hadError;
	
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
	
	@Override
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
	
	@Override
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
	
	@Override
	protected void gotTypeDefName(LocatableToken nameToken)
	{
		if (classLevel == 1) {
			if (info == null || isPublic && !info.foundPublicClass()) {
				info = new ClassInfo();
				info.setName(nameToken.getText(), isPublic);
				info.setExtendsInsertSelection(new Selection(nameToken.getLine(), nameToken.getEndColumn()));
				storeCurrentClassInfo = true;
			} else {
				storeCurrentClassInfo = false;
			}
		}
		super.gotTypeDefName(nameToken);
	}
	
	@Override
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
			}
		}
		catch (TokenStreamException tse) {
			tse.printStackTrace(); // TODO
		}
	}
	
	@Override
	protected void gotTypeDefImplements(LocatableToken implementsToken)
	{
		if (classLevel == 1 && storeCurrentClassInfo) {
			gotImplements = true;
		}
	}
	
	@Override
	public List<LocatableToken> parseModifiers()
	{
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
}
