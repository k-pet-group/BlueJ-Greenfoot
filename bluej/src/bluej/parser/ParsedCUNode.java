package bluej.parser;

import javax.swing.event.DocumentEvent;
import javax.swing.text.Document;

import org.syntax.jedit.tokenmarker.Token;

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

	public void textInserted(int nodePos, DocumentEvent event) {
	// TODO Auto-generated method stub
	
	}
	
	public void textRemoved(int nodePos, DocumentEvent event) {
	// TODO Auto-generated method stub
	
	}
}
