package bluej.parser.ast;

import antlr.*;

public class LocatableToken extends CommonToken
{
    private int endColumn;
    private Token hiddenBefore;
    
    public LocatableToken() {
        super();
    }

    public LocatableToken(int t, String txt) {
        super(t, txt);
    }

    public LocatableToken(String s) {
        super(s);
    }
    
    public void setEndColumn(int c)
    {
        endColumn = c;
    }
    
    public int getEndColumn()
    {
        return endColumn;
    }
    
    public int getLength()
    {
        return endColumn - col;
    }
    
    public void setHiddenBefore(Token t)
    {
        hiddenBefore = t;
    }
    
    public Token getHiddenBefore()
    {
        return hiddenBefore;
    }
}
