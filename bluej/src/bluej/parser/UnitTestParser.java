package bluej.parser;

import java.io.Reader;
import java.util.LinkedList;
import java.util.List;

import bluej.parser.ast.LocatableToken;

public class UnitTestParser extends NewParser
{
    private LocatableToken beginningElement;
    private int classLevel = 0; // level of class nesting
    private int elementLevel = 0;
    private boolean inField = false;
    
    private List<SourceSpan> fieldSpans = new LinkedList<SourceSpan>();
    
    public UnitTestParser(Reader r)
    {
        super(r);
        try {
            parseCU();
        }
        catch (Exception e) {
            
        }
    }
    
    public List<SourceSpan> getFieldSpans()
    {
        return fieldSpans;
    }
    
    protected void error(String msg)
    {
        throw new RuntimeException("Parse error: " + msg);
    }
    
    protected void beginElement(LocatableToken token)
    {
        elementLevel++;
        if (classLevel == 1 && elementLevel == 2) {
            beginningElement = token;
        }
    }
    
    protected void gotField(LocatableToken idToken)
    {
        if (elementLevel == 2) {
            inField = true;
        }
    }
    
    protected void endElement(LocatableToken token, boolean included)
    {
        if (elementLevel == 2 && inField) {
            inField = false;
            SourceLocation start = new SourceLocation(beginningElement.getLine(), beginningElement.getColumn());
            SourceLocation end = new SourceLocation(token.getEndLine(), token.getEndColumn());
            SourceSpan ss = new SourceSpan(start, end);
            fieldSpans.add(ss);
        }
        elementLevel--;
    }
    
    protected void gotTypeDef(int tdType)
    {
        classLevel++;
    }
    
    protected void gotTypeDefEnd(LocatableToken token, boolean included)
    {
        classLevel--;
        endElement(token, included);
    }
    
    
}
