package bluej.parser;

import java.io.Reader;

import javax.swing.event.DocumentEvent;
import javax.swing.text.Document;

import org.syntax.jedit.tokenmarker.Token;

import bluej.parser.NodeTree.NodeAndPosition;

/**
 * A parsed compilation unit node.
 * 
 * @author davmac
 */
public class ParsedCUNode extends ParsedNode
{
	//private JavaTokenMarker marker = new JavaTokenMarker();
	
	
	public Token getMarkTokensFor(int pos, int length, int nodePos, Document document)
	{
//		TokenMarker tm = ((SyntaxDocument) document).getTokenMarker();
//		Segment line = new Segment();
//		
//		try {
//			document.getText(pos, length, line);
//		}
//		catch (BadLocationException ble) {
//			return null;
//		}
//		
//		int lineIndex = document.getDefaultRootElement().getElementIndex(pos);
//		return tm.markTokens(line, lineIndex);
		
		Token tok = new Token(length, Token.NULL);
		tok.next = new Token(0, Token.END);
		return tok;
	}

	public void textInserted(int nodePos, DocumentEvent event)
	{
	    NodeAndPosition child = getNodeTree().findNode(nodePos);
	    if (child != null) {
	        child.getNode().textInserted(nodePos + child.getPosition(), event);
	        // TODO grow the child node?
	    }
	    else {
	        // We must handle the insertion ourself
	        // TODO
            // for now just do a full reparse
	        doReparse(event.getDocument());
	    }
	}
	
	public void textRemoved(int nodePos, DocumentEvent event)
	{
            NodeAndPosition child = getNodeTree().findNode(nodePos);
            if (child != null) {
                child.getNode().textRemoved(nodePos + child.getPosition(), event);
                // TODO shrink the child node?
                // TODO check if an entire child/children were removed.
            }
            else {
                // We must handle the insertion ourself
                // TODO
                // for now just do a full reparse
                doReparse(event.getDocument());
            }
	}
	
	private void doReparse(Document document)
	{
	    getNodeTree().clear();
	    Reader r = new DocumentReader(document);
	    EditorParser parser = new EditorParser(r);
	    parser.parseCU(this);
	}
}
