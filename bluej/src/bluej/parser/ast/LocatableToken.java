package bluej.parser.ast;

import antlr.*;

public class LocatableToken extends CommonHiddenStreamToken
{
    public LocatableToken() {
        super();
    }

    public LocatableToken(int t, String txt) {
        super(t, txt);
    }

    public LocatableToken(String s) {
        super(s);
    }
}
