package bluej.parser;

import java.io.Reader;

import bluej.parser.ast.LocatableToken;
import bluej.parser.symtab.Selection;

/**
 * Parser which builds parse node tree.
 * 
 * @author davmac
 */
public class EditorParser extends NewParser
{
    // private Stack<ParsedNode> scopeStack;
    
    private LocatableToken pkgStatementBegin;
    
    public EditorParser(Reader r)
    {
        super(r);
    }
    
    protected void error(String msg)
    {
        // ignore for now
    }
    
    public void parseCU(ParsedCUNode pcuNode)
    {
        parseCU();
    }
    
    /*
     * We have the beginning of a "package x.y.z;" statement. 
     */
    protected void beginPackageStatement(LocatableToken token)
    {
        pkgStatementBegin = token;
    }
    
    /*
     * We have the end of a package statement.
     */
    protected void gotPackageSemi(LocatableToken token)
    {
        Selection s = new Selection(pkgStatementBegin.getLine(), pkgStatementBegin.getColumn());
        s.extendEnd(token.getLine(), token.getColumn() + token.getLength());
        
        // PkgStmtNode psn = new PkgStmtNode();
        
    }
}
