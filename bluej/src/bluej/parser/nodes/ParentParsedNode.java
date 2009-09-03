package bluej.parser.nodes;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Segment;

import antlr.TokenStream;
import antlr.TokenStreamException;
import bluej.editor.moe.MoeSyntaxDocument;
import bluej.editor.moe.Token;
import bluej.parser.DocumentReader;
import bluej.parser.JavaTokenFilter;
import bluej.parser.NewParser;
import bluej.parser.ast.LocatableToken;
import bluej.parser.ast.gen.JavaLexer;
import bluej.parser.ast.gen.JavaTokenTypes;
import bluej.parser.nodes.NodeTree.NodeAndPosition;

/**
 * An abstract ParsedNode which delegates to child nodes.
 * 
 * @author davmac
 */
public class ParentParsedNode extends ParsedNode
{
    private int cachedLeftIndex = -1;
    
    protected ParentParsedNode()
    {
        super();
    }
    
    public ParentParsedNode(ParsedNode myParent)
    {
        super(myParent);
    }
    
    public int getLeftmostIndent(Document document, int nodePos, int tabSize)
    {
        if (cachedLeftIndex == -1) {
            recalcLeftIndent(document, nodePos, tabSize);
        }
        return cachedLeftIndex;
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
        DocumentReader dr = new DocumentReader(document, pos);
        TokenStream lexer = NewParser.getLexer(dr);
        TokenStream tokenStream = new JavaTokenFilter(lexer, null);

        Token dummyTok = new Token(0, Token.END);
        Token token = dummyTok;
        
        try {
            int curcol = 1;
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
                    case JavaTokenTypes.LITERAL_if:
                    case JavaTokenTypes.LITERAL_else:
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

    private void recalcLeftIndent(Document document, int nodePos, int tabSize)
    {
        cachedLeftIndex = -1;
        int size = getSize();
        int endpos = nodePos + size;
        int curpos = nodePos;
        
        while (curpos < endpos) {
            NodeAndPosition nap = getNodeTree().findNodeAtOrAfter(curpos, nodePos);
            int napos = endpos;
            if (nap != null) {
                napos = Math.min(nap.getPosition(), napos);
            }
            
            // A segment of text which is in this node.
            int textlen = napos - curpos;
            while (textlen != 0) {
                int lbegin = document.getDefaultRootElement().getElementIndex(curpos);
                int lcol = curpos - document.getDefaultRootElement().getElement(lbegin).getStartOffset();
                int endlPos = document.getDefaultRootElement().getElement(lbegin).getEndOffset();
                
                int lineAmount = Math.min(endlPos - curpos, textlen);
                Segment segment = new Segment();
                try {
                    document.getText(curpos, lineAmount, segment);
                } catch (BadLocationException e) {
                    // e.printStackTrace();
                }
                
                int indent = getIndentOf(segment, lcol, tabSize);
                if (indent != -1 && (indent < cachedLeftIndex || cachedLeftIndex == -1)) {
                    cachedLeftIndex = indent;
                }
                
                curpos += lineAmount;
                textlen -= lineAmount;
            }
            
            if (nap != null) {
                curpos += nap.getSize();
            }
        }
        
        if (cachedLeftIndex == -1) {
            ParsedNode parent = getParentNode();
            if (parent != null) {
                cachedLeftIndex = Math.max(parent.getLeftmostIndent(document, nodePos, tabSize), 0);
                cachedLeftIndex += tabSize;
            }
        }
    }
    
    /**
     * Get the indent of a string, if it starts at the given column.
     * Returns -1 if the indent couldn't be identified (empty line).
     */
    private int getIndentOf(Segment string, int startcol, int tabSize)
    {
        int indent = startcol;
        for (int i = string.getBeginIndex(); i < string.getEndIndex(); i++) {
            char c = string.setIndex(i);
            if (c == '\n') {
                return -1;
            }
            if (c == ' ') {
                indent++;
            }
            else if (c == '\t') {
                indent += tabSize;
                indent -= indent % tabSize;
            }
            else {
                return indent;
            }
        }
        
        return -1;
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

                cachedLeftIndex = -1; // DAV not always needed
                reparseNode(document, nodePos, 0);
                ((MoeSyntaxDocument) document).documentChanged();
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
        
        cachedLeftIndex = -1; // DAV not always needed
        reparseNode(document, nodePos, 0);
        ((MoeSyntaxDocument) document).documentChanged();
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
        cachedLeftIndex = -1;
    }
    
    //protected abstract void doReparse(Document document, int nodePos, int position);
}
