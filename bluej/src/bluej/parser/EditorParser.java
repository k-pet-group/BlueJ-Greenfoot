package bluej.parser;

import java.io.Reader;
import java.util.Stack;

import org.syntax.jedit.tokenmarker.Token;

import bluej.parser.ast.LocatableToken;
import bluej.parser.symtab.Selection;

/**
 * Parser which builds parse node tree.
 * 
 * @author davmac
 */
public class EditorParser extends NewParser
{
    private Stack<ParsedNode> scopeStack = new Stack<ParsedNode>();
    
    private LocatableToken pcuStmtBegin;
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
        scopeStack.push(pcuNode);
        parseCU();
        scopeStack.pop();
    }
    
    /*
     * We have the beginning of a "package x.y.z;" statement. 
     */
    protected void beginPackageStatement(LocatableToken token)
    {
        pcuStmtBegin = token;
    }
    
    /*
     * We have the end of a package statement.
     */
    protected void gotPackageSemi(LocatableToken token)
    {
        Selection s = new Selection(pcuStmtBegin.getLine(), pcuStmtBegin.getColumn());
        s.extendEnd(token.getLine(), token.getColumn() + token.getLength());
        
        int startpos = pcuNode.lineColToPosition(s.getLine(), s.getColumn());
        int endpos = pcuNode.lineColToPosition(s.getEndLine(), s.getEndColumn());
        
        // PkgStmtNode psn = new PkgStmtNode();
        ColourNode cn = new ColourNode(pcuNode, Token.KEYWORD1);
        pcuNode.getNodeTree().insertNode(cn, startpos, endpos - startpos);
    }
    
    protected void beginImportStatement(LocatableToken token)
    {
        pcuStmtBegin = token;
    }
    
    protected void gotImportStmtSemi(LocatableToken token)
    {
        Selection s = new Selection(pcuStmtBegin.getLine(), pcuStmtBegin.getColumn());
        s.extendEnd(token.getLine(), token.getColumn() + token.getLength());
        
        int startpos = pcuNode.lineColToPosition(s.getLine(), s.getColumn());
        int endpos = pcuNode.lineColToPosition(s.getEndLine(), s.getEndColumn());
        
        // PkgStmtNode psn = new PkgStmtNode();
        ColourNode cn = new ColourNode(pcuNode, Token.KEYWORD2);
        pcuNode.getNodeTree().insertNode(cn, startpos, endpos - startpos);
    }
    
    public void gotComment(LocatableToken token)
    {
        Selection s = new Selection(token.getLine(), token.getColumn());
        s.extendEnd(token.getEndLine(), token.getEndColumn());

        int startpos = pcuNode.lineColToPosition(s.getLine(), s.getColumn());
        int endpos = pcuNode.lineColToPosition(s.getEndLine(), s.getEndColumn());

        ColourNode cn = new ColourNode(pcuNode, Token.COMMENT1);
        scopeStack.peek().getNodeTree().insertNode(cn, startpos, endpos - startpos);
    }
}
