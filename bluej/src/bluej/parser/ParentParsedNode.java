package bluej.parser;

import javax.swing.text.Document;

import org.syntax.jedit.tokenmarker.Token;

import bluej.parser.NodeTree.NodeAndPosition;

/**
 * An abstract ParsedNode which delegates to child nodes.
 * 
 * @author davmac
 */
public abstract class ParentParsedNode extends ParsedNode
{

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
                tok.next = new Token(nextTokLen, Token.NULL);
                tok = tok.next;
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
            tok.next = new Token(nextTokLen, Token.NULL);
            tok = tok.next;
        }

        tok.next = new Token(0, Token.END);
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
            doReparse(document, nodePos, 0);
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
    protected void reparseNode(Document document, int offset)
    {
        // Get own offset
        int noffset = offset;
        if (getContainingNodeTree() != null) {
            noffset += getContainingNodeTree().getPosition();
        }
        getParentNode().reparseNode(document, noffset);
    }
    
    protected abstract void doReparse(Document document, int nodePos, int position);
}
