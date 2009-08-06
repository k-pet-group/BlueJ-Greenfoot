package bluej.parser;

import javax.swing.text.Document;

import org.syntax.jedit.tokenmarker.Token;

public class ColourNode extends ParsedNode
{
    byte colour;
    
    public ColourNode(ParsedCUNode parentNode, byte colour)
    {
        super(parentNode);
        this.colour = colour;
    }
    
    public Token getMarkTokensFor(int pos, int length, int nodePos,
            Document document)
    {
        Token tok = new Token(length, colour);
        tok.next = new Token(0, Token.END);
        return tok;
    }

    public void textInserted(Document document, int nodePos, int insPos, int length)
    {
        getParentNode().reparseNode(document, nodePos, 0);
    }

    public void textRemoved(Document document, int nodePos, int delPos, int length)
    {
        getParentNode().reparseNode(document, nodePos, 0);
    }

}
