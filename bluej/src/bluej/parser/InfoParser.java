package bluej.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import bluej.parser.ast.LocatableToken;
import bluej.parser.ast.gen.JavaTokenTypes;
import bluej.parser.symtab.ClassInfo;

public class InfoParser extends NewParser
{
	private ClassInfo info;
	private int classLevel = 0; // number of nested classes
	private boolean gotTypeDef; // whether we just reach a type def
	private boolean isPublic;
	
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
	
	public static ClassInfo parse(Reader r)
	{
		InfoParser infoParser = new InfoParser(r);
		infoParser.parseCU();
		if (infoParser != null) {
			return infoParser.info;
		}
		else {
			throw new RuntimeException("Couldn't get class info");
		}
	}
	
//	@Override
//	protected void error(String msg)
//	{
//		// Just try and recover.
//	}
	
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
			}
		}
		super.gotTypeDefName(nameToken);
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
