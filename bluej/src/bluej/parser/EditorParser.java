package bluej.parser;

import java.io.Reader;

import org.syntax.jedit.tokenmarker.Token;

import com.sun.org.apache.bcel.internal.generic.GETSTATIC;

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
    private ParsedCUNode pcuNode;
    
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
        this.pcuNode = pcuNode;
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
        
        int startpos = pcuNode.lineColToPosition(s.getLine(), s.getColumn());
        int endpos = pcuNode.lineColToPosition(s.getEndLine(), s.getEndColumn());
        
        // PkgStmtNode psn = new PkgStmtNode();
        ColourNode cn = new ColourNode(pcuNode, Token.KEYWORD1);
        pcuNode.getNodeTree().insertNode(cn, startpos, endpos - startpos);
    }
    
    public void gotComment(LocatableToken token)
    {
        Selection s = new Selection(token.getLine(), token.getColumn());
        s.extendEnd(token.getEndLine(), token.getEndColumn());

        int startpos = pcuNode.lineColToPosition(s.getLine(), s.getColumn());
        int endpos = pcuNode.lineColToPosition(s.getEndLine(), s.getEndColumn());

        ColourNode cn = new ColourNode(pcuNode, Token.COMMENT1);
        pcuNode.getNodeTree().insertNode(cn, startpos, endpos - startpos);
    }
}
