package bluej.parser;

import java.io.Reader;
import java.util.Iterator;
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
    
    protected void beginElement(LocatableToken token)
    {
        pcuStmtBegin = token;
    }
    
    protected void gotTypeDef(int tdType)
    {
        ParsedNode pnode = new ParsedTypeNode(scopeStack.peek());
        int curOffset = getCurrentOffset();
        int insPos = pcuNode.lineColToPosition(pcuStmtBegin.getLine(), pcuStmtBegin.getColumn());
        scopeStack.peek().getNodeTree().insertNode(pnode, insPos - curOffset, 0);
        scopeStack.push(pnode);
    }
    
    private int getCurrentOffset()
    {
        Iterator<ParsedNode> i = scopeStack.iterator();
        if (!i.hasNext()) {
            return 0;
        }
        
        int rval = 0;
        i.next();
        while (i.hasNext()) {
            rval += i.next().getContainingNodeTree().getPosition();
        }
        return rval;
    }
    
    protected void gotTypeDefEnd(LocatableToken token, boolean included)
    {
        int topPos = getCurrentOffset();
        ParsedNode top = scopeStack.pop();
        
        int endPos = pcuNode.lineColToPosition(token.getEndLine(), token.getEndColumn());
        top.getContainingNodeTree().setNodeSize(endPos - topPos);
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
        int curOffset = getCurrentOffset();
        scopeStack.peek().getNodeTree().insertNode(cn, startpos - curOffset, endpos - startpos);
    }
}
