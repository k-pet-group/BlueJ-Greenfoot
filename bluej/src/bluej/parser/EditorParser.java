package bluej.parser;

import java.io.Reader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

import org.syntax.jedit.tokenmarker.Token;

import bluej.parser.ast.LocatableToken;
import bluej.parser.ast.gen.JavaTokenTypes;
import bluej.parser.nodes.ColourNode;
import bluej.parser.nodes.ContainerNode;
import bluej.parser.nodes.ParentParsedNode;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.nodes.ParsedNode;
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
    private List<LocatableToken> commentQueue = new LinkedList<LocatableToken>();
    
    
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
        completedNode(pcuNode, 0, pcuNode.getSize());
    }
                
    private void endTopNode(LocatableToken token, boolean included)
    {
        int topPos = getTopNodeOffset();
        ParsedNode top = scopeStack.pop();

        int endPos;
        if (included) {
            endPos = pcuNode.lineColToPosition(token.getEndLine(), token.getEndColumn());
        }
        else {
            endPos = pcuNode.lineColToPosition(token.getLine(), token.getColumn());
        }
        top.setNodeSize(endPos - topPos);
        
        completedNode(top, topPos, endPos - topPos);
    }

    /**
     * A node end has been reached. This method adds any appropriate comment nodes as
     * children of the new node.
     * 
     * @param node  The new node
     * @param position  The absolute position of the new node
     * @param size  The size of the new node
     */
    protected void completedNode(ParsedNode node, int position, int size)
    {
        ListIterator<LocatableToken> i = commentQueue.listIterator();
        while (i.hasNext()) {
            LocatableToken token = i.next();
            int startpos = pcuNode.lineColToPosition(token.getLine(), token.getColumn());
            if (startpos >= position && startpos < (position + size)) {
                Selection s = new Selection(token.getLine(), token.getColumn());
                s.extendEnd(token.getEndLine(), token.getEndColumn());
                int endpos = pcuNode.lineColToPosition(s.getEndLine(), s.getEndColumn());

                ColourNode cn = new ColourNode(node, Token.COMMENT1);
                node.insertNode(cn, startpos - position, endpos - startpos);
                
                i.remove();
            }
        }
    }

    /**
     * Get the start position of the top node in the scope stack.
     */
    private int getTopNodeOffset()
    {
        Iterator<ParsedNode> i = scopeStack.iterator();
        if (!i.hasNext()) {
            return 0;
        }
        
        int rval = 0;
        i.next();
        while (i.hasNext()) {
            rval += i.next().getOffsetFromParent();
        }
        return rval;
    }
    
    //  -------------- Callbacks from the superclass ----------------------

    @Override
    protected void beginPackageStatement(LocatableToken token)
    {
        pcuStmtBegin = token;
    }
    
    @Override
    protected void gotTypeDef(int tdType)
    {
        ParsedNode pnode = new ParsedTypeNode(scopeStack.peek());
        int curOffset = getTopNodeOffset();
        int insPos = pcuNode.lineColToPosition(pcuStmtBegin.getLine(), pcuStmtBegin.getColumn());
        scopeStack.peek().insertNode(pnode, insPos - curOffset, 0);
        scopeStack.push(pnode);
    }
    
    @Override
    protected void beginTypeBody(LocatableToken token)
    {
        ParentParsedNode bodyNode = new ParentParsedNode(scopeStack.peek());
        bodyNode.setInner(true);
        int curOffset = getTopNodeOffset();
        int insPos = pcuNode.lineColToPosition(token.getEndLine(), token.getEndColumn());
        scopeStack.peek().insertNode(bodyNode, insPos - curOffset, 0);
        scopeStack.push(bodyNode);
    }
    
    @Override
    protected void beginForLoop(LocatableToken token)
    {
        ParentParsedNode loopNode = new ContainerNode(scopeStack.peek(), ParsedNode.NODETYPE_ITERATION);
        int curOffset = getTopNodeOffset();
        int insPos = pcuNode.lineColToPosition(token.getLine(), token.getColumn());
        scopeStack.peek().insertNode(loopNode, insPos - curOffset, 0);
        scopeStack.push(loopNode);
    }
    
    @Override
    protected void beginForLoopBody(LocatableToken token)
    {
        if (token.getType() != JavaTokenTypes.LCURLY) {
            ParentParsedNode loopNode = new ParentParsedNode(scopeStack.peek());
            loopNode.setInner(true);
            int curOffset = getTopNodeOffset();
            int insPos = pcuNode.lineColToPosition(token.getLine(), token.getColumn());
            scopeStack.peek().insertNode(loopNode, insPos - curOffset, 0);
            scopeStack.push(loopNode);
        }
    }
    
    @Override
    protected void endForLoopBody(LocatableToken token, boolean included)
    {
        if (scopeStack.peek().getNodeType() != ParsedNode.NODETYPE_ITERATION) {
            endTopNode(token, false);
        }
    }
    
    @Override
    protected void beginStmtblockBody(LocatableToken token)
    {
        int curOffset = getTopNodeOffset();
        if (scopeStack.peek().getNodeType() == ParsedNode.NODETYPE_NONE) {
            // This is conditional, because the outer block may be a loop or selection
            // statement which already exists.
            ParentParsedNode blockNode = new ContainerNode(scopeStack.peek(), ParsedNode.NODETYPE_NONE);
            blockNode.setInner(true);
            int insPos = pcuNode.lineColToPosition(token.getLine(), token.getColumn());
            scopeStack.peek().insertNode(blockNode, insPos - curOffset, 0);
            scopeStack.push(blockNode);
        }
        ParentParsedNode blockInner = new ParentParsedNode(scopeStack.peek());
        blockInner.setInner(true);
        int insPos = pcuNode.lineColToPosition(token.getEndLine(), token.getEndColumn());
        scopeStack.peek().insertNode(blockInner, insPos - curOffset, 0);
        scopeStack.push(blockInner);
    }
   
    @Override
    protected void endStmtblockBody(LocatableToken token, boolean included)
    {
        endTopNode(token, false); // inner
        if (scopeStack.peek().getNodeType() == ParsedNode.NODETYPE_NONE) {
            endTopNode(token, included);
        }
    }
    
    protected void beginElement(LocatableToken token)
    {
        pcuStmtBegin = token;
    }
    
    protected void endTypeBody(LocatableToken token, boolean included)
    {
        endTopNode(token, false);
    }
    
    protected void gotTypeDefEnd(LocatableToken token, boolean included)
    {
        endTopNode(token, included);
    }
    
    @Override
    protected void endForLoop(LocatableToken token, boolean included)
    {
        endTopNode(token, included);
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
        pcuNode.insertNode(cn, startpos, endpos - startpos);
        completedNode(cn, startpos, endpos - startpos);
    }
    
    protected void gotImportStmtSemi(LocatableToken token)
    {
        Selection s = new Selection(pcuStmtBegin.getLine(), pcuStmtBegin.getColumn());
        s.extendEnd(token.getLine(), token.getColumn() + token.getLength());
        
        int startpos = pcuNode.lineColToPosition(s.getLine(), s.getColumn());
        int endpos = pcuNode.lineColToPosition(s.getEndLine(), s.getEndColumn());
        
        // PkgStmtNode psn = new PkgStmtNode();
        ColourNode cn = new ColourNode(pcuNode, Token.KEYWORD2);
        pcuNode.insertNode(cn, startpos, endpos - startpos);
    }
    
    public void gotComment(LocatableToken token)
    {
        commentQueue.add(token);
    }
    
    @Override
    protected void gotConstructorDecl(LocatableToken token,
            LocatableToken hiddenToken)
    {
        super.gotConstructorDecl(token, hiddenToken);
        LocatableToken start = pcuStmtBegin;
        if (hiddenToken != null) {
            start = hiddenToken;
        }
        
        ParsedNode pnode = new MethodNode(scopeStack.peek());
        int curOffset = getTopNodeOffset();
        int insPos = pcuNode.lineColToPosition(start.getLine(), start.getColumn());
        scopeStack.peek().insertNode(pnode, insPos - curOffset, 0);
        scopeStack.push(pnode);
    }
    
    @Override
    protected void gotMethodDeclaration(LocatableToken token,
            LocatableToken hiddenToken)
    {
        super.gotMethodDeclaration(token, hiddenToken);
        LocatableToken start = pcuStmtBegin;
        if (hiddenToken != null) {
            start = hiddenToken;
        }
        
        ParsedNode pnode = new MethodNode(scopeStack.peek());
        int curOffset = getTopNodeOffset();
        int insPos = pcuNode.lineColToPosition(start.getLine(), start.getColumn());
        scopeStack.peek().insertNode(pnode, insPos - curOffset, 0);
        scopeStack.push(pnode);
    }
    
    @Override
    protected void endMethodDecl(LocatableToken token, boolean included)
    {
        super.endMethodDecl(token, included);
        endTopNode(token, included);
    }
    
    @Override
    protected void beginMethodBody(LocatableToken token)
    {
        ParsedNode pnode = new ParentParsedNode(scopeStack.peek());
        pnode.setInner(true);
        int curOffset = getTopNodeOffset();
        int insPos = pcuNode.lineColToPosition(token.getEndLine(), token.getEndColumn());
        scopeStack.peek().insertNode(pnode, insPos - curOffset, 0);
        scopeStack.push(pnode);
    }
    
    @Override
    protected void endMethodBody(LocatableToken token, boolean included)
    {
        super.endMethodBody(token, included);
        endTopNode(token, false);
    }
}
