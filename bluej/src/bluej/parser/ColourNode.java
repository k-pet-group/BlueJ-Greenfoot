package bluej.parser;

import javax.swing.event.DocumentEvent;
import javax.swing.text.Document;

import org.syntax.jedit.tokenmarker.Token;

public class ColourNode extends ParsedNode
{
    public ColourNode(ParsedCUNode parentNode)
    {
        super(parentNode);
    }
    
    
    public Token getMarkTokensFor(int pos, int length, int nodePos,
            Document document)
    {
        Token tok = new Token(length, Token.KEYWORD1);
        tok.next = new Token(0, Token.END);
        return tok;
    }

    public void textInserted(int nodePos, DocumentEvent event)
    {
        getParentNode().reparseNode(event.getDocument(), nodePos);
    }

    public void textRemoved(int nodePos, DocumentEvent event)
    {
        getParentNode().reparseNode(event.getDocument(), nodePos);
    }

}
