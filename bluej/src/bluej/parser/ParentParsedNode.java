package bluej.parser;

import javax.swing.text.Document;

import org.syntax.jedit.tokenmarker.Token;

import antlr.TokenStream;
import antlr.TokenStreamException;
import bluej.parser.NodeTree.NodeAndPosition;
import bluej.parser.ast.LocatableToken;
import bluej.parser.ast.gen.JavaLexer;
import bluej.parser.ast.gen.JavaTokenTypes;

/**
 * An abstract ParsedNode which delegates to child nodes.
 * 
 * @author davmac
 */
public class ParentParsedNode extends ParsedNode
{
    protected ParentParsedNode()
    {
        super();
    }
    
    public ParentParsedNode(ParsedNode myParent)
    {
        super(myParent);
    }
    
    public Token getMarkTokensFor(int pos, int length, int nodePos,
            Document document)
    {
        Token tok = new Token(0, Token.END); // dummy
        if (length == 0) {
            return tok;
        }
        Token dummyTok = tok;
        
        NodeAndPosition np = getNodeTree().findNodeAtOrAfter(pos, nodePos);
        
        int cp = pos;
        while (np != null && np.getPosition() < (pos + length)) {
            if (cp < np.getPosition()) {
                int nextTokLen = np.getPosition() - cp;
                tok.next = tokenizeText(document, cp, nextTokLen);
                while (tok.next.id != Token.END) tok = tok.next;
                cp = np.getPosition();
            }
            
            int remaining = pos + length - cp;
            if (remaining > np.getSize() - cp + np.getPosition()) {
                remaining = np.getSize() - cp + np.getPosition();
            }
            if (remaining == 0) {
                break;
            }
            tok.next = np.getNode().getMarkTokensFor(cp, remaining, np.getPosition(), document);
            cp += remaining;
            while (tok.next.id != Token.END) {
                tok = tok.next;
            }
            np = getNodeTree().findNodeAtOrAfter(cp, nodePos);
        }
        
        // There may be a section left
        if (cp < pos + length) {
            int nextTokLen = pos + length - cp;
            tok.next = tokenizeText(document, cp, nextTokLen);
            while (tok.next.id != Token.END) tok = tok.next;
        }

        tok.next = new Token(0, Token.END);
        return dummyTok.next;
    }
    
    protected static Token tokenizeText(Document document, int pos, int length)
    {
        int line = document.getDefaultRootElement().getElementIndex(pos) + 1;
        DocumentReader dr = new DocumentReader(document, pos);
        
        EscapedUnicodeReader euReader = new EscapedUnicodeReader(dr);
        JavaLexer lexer = new JavaLexer(euReader);
        lexer.setTokenObjectClass("bluej.parser.ast.LocatableToken");
        lexer.setTabSize(4);
        euReader.setAttachedScanner(lexer);
        TokenStream tokenStream = new JavaTokenFilter(lexer, null);

        Token dummyTok = new Token(0, Token.END);
        Token token = dummyTok;
        
        try {
            int curcol = pos - document.getDefaultRootElement().getElement(line-1).getStartOffset() + 1;
            while (length > 0) {
                LocatableToken lt = (LocatableToken) tokenStream.nextToken();
                
                if (lt.getLine() > 1 || lt.getColumn() - curcol >= length) {
                    token.next = new Token(length, Token.NULL);
                    token = token.next;
                    break;
                }
                if (lt.getColumn() > curcol) {
                    // some space before the token
                    token.next = new Token(lt.getColumn() - curcol, Token.NULL);
                    token = token.next;
                    length -= token.length;
                    curcol += token.length;
                }
                
                byte tokType = Token.NULL;
                if (NewParser.isPrimitiveType(lt)) {
                    tokType = Token.PRIMITIVE;
                }
                else if (NewParser.isModifier(lt)) {
                    tokType = Token.KEYWORD1;
                }
                else if (lt.getType() == JavaTokenTypes.STRING_LITERAL) {
                    tokType = Token.LITERAL1;
                }
                else if (lt.getType() == JavaTokenTypes.CHAR_LITERAL) {
                    tokType = Token.LITERAL2;
                }
                else {
                    switch (lt.getType()) {
                    case JavaTokenTypes.LITERAL_assert:
                    case JavaTokenTypes.LITERAL_for:
                    case JavaTokenTypes.LITERAL_switch:
                    case JavaTokenTypes.LITERAL_while:
                    case JavaTokenTypes.LITERAL_do:
                    case JavaTokenTypes.LITERAL_try:
                    case JavaTokenTypes.LITERAL_catch:
                    case JavaTokenTypes.LITERAL_throw:
                    case JavaTokenTypes.LITERAL_finally:
                    case JavaTokenTypes.LITERAL_return:
                    case JavaTokenTypes.LITERAL_case:
                    case JavaTokenTypes.LITERAL_break:
                        tokType = Token.KEYWORD1;
                        break;
                    
                    case JavaTokenTypes.LITERAL_class:
                    case JavaTokenTypes.LITERAL_package:
                    case JavaTokenTypes.LITERAL_import:
                    case JavaTokenTypes.LITERAL_extends:
                    case JavaTokenTypes.LITERAL_interface:
                    case JavaTokenTypes.LITERAL_enum:
                        tokType = Token.KEYWORD2;
                        break;
                    
                    case JavaTokenTypes.LITERAL_this:
                    case JavaTokenTypes.LITERAL_null:
                    case JavaTokenTypes.LITERAL_super:
                    case JavaTokenTypes.LITERAL_true:
                    case JavaTokenTypes.LITERAL_false:
                        tokType = Token.KEYWORD3;
                        break;
                    
                    default:
                    }
                }
                token.next = new Token(lt.getLength(), tokType);
                token = token.next;
                length -= lt.getLength();
                curcol += lt.getLength();
            }
        } catch (TokenStreamException e) {
            // e.printStackTrace();
        }
        
        token.next = new Token(0, Token.END);
        return dummyTok.next;
    }

    public void textInserted(Document document, int nodePos, int insPos,
            int length)
    {
        NodeAndPosition child = getNodeTree().findNode(insPos, nodePos);
        if (child != null) {
            ParsedNode cnode = child.getNode();
            NodeTree cnodeTree = cnode.getContainingNodeTree();
            // grow the child node
            cnodeTree.setNodeSize(cnodeTree.getNodeSize() + length);
            // inform the child node of the change
            child.getNode().textInserted(document, child.getPosition(), insPos, length);
        }
        else {
            // We must handle the insertion ourself
            // TODO
            // for now just do a full reparse
            reparseNode(document, nodePos, 0);
        }
    }

    public void textRemoved(Document document, int nodePos, int delPos,
            int length)
    {
        int endPos = delPos + length;
        
        NodeAndPosition child = getNodeTree().findNodeAtOrAfter(delPos, nodePos);
        
        if (child != null && child.getPosition() < delPos) {
            // Remove the end portion (or middle) of the child node
            int childEndPos = child.getPosition() + child.getSize();
            if (childEndPos > endPos) {
                // Remove the middle of the child node
                child.getNode().textRemoved(document, child.getPosition() + nodePos, delPos, length);
                NodeTree childTree = child.getNode().getContainingNodeTree();
                childTree.setNodeSize(childTree.getNodeSize() - length);
                return;
            }
            else {
                // Remove the end portion of the child node
                int rlength = childEndPos - delPos; // how much is removed
                child.getNode().textRemoved(document, child.getPosition() + nodePos, delPos, rlength);
                NodeTree childTree = child.getNode().getContainingNodeTree();
                childTree.setNodeSize(childTree.getNodeSize() - length);
                length -= rlength;
                endPos -= rlength;
            }
            child = getNodeTree().findNodeAtOrAfter(delPos, nodePos);
        }
        
        if (child != null) {
            int childPos = child.getPosition();
            int childLen = child.getSize();
            
            while (childPos + childLen < endPos) {
                // The whole child should be removed
                child.getNode().getContainingNodeTree().remove();
                child = getNodeTree().findNodeAtOrAfter(delPos, nodePos);
                if (child == null) {
                    break;
                }
                childPos = child.getPosition();
                childLen = child.getSize();
            }
            
            if (child != null) {
                if (childPos < endPos) {
                    int slideLen = childPos - delPos;
                    child.getNode().getContainingNodeTree().slideNode(-slideLen);
                    length -= slideLen;
                    child.getNode().textRemoved(document, childPos, delPos, length - slideLen);
                    child.getNode().getContainingNodeTree().setNodeSize(child.getSize() - length);
                }
                else {
                    child.getNode().getContainingNodeTree().slideNode(-length);
                }
            }
            
        }
    }

    /**
     * Re-parse the node. The default implementation passes the request down to the parent.
     * The tree root must provide a different implementation.
     */
    protected void reparseNode(Document document, int nodePos, int offset)
    {
        // Get own offset
        int noffset = offset;
        if (getContainingNodeTree() != null) {
            noffset += getContainingNodeTree().getPosition();
        }
        getParentNode().reparseNode(document, nodePos - noffset, 0);
    }
    
    //protected abstract void doReparse(Document document, int nodePos, int position);
}
