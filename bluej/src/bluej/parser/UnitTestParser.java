package bluej.parser;

import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import bluej.parser.ast.LocatableToken;

public class UnitTestParser extends NewParser
{
    private LocatableToken beginningElement;
    private int classLevel = 0; // level of class nesting
    private int elementLevel = 0;
    private boolean inField = false;
    private boolean inMethod = false; // are we in an interesting method
    private String methodName;
    private LocatableToken methodBegin;
    
    private List<SourceSpan> fieldSpans = new LinkedList<SourceSpan>();
    private SourceLocation methodInsertLocation;
    private SourceLocation fixtureInsertLocation;
    private Map<String,SourceSpan> methodSpans = new HashMap<String,SourceSpan>();
    
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
    
    public SourceLocation getNewMethodInsertLocation()
    {
        return methodInsertLocation;
    }
    
    public SourceLocation getFixtureInsertLocation()
    {
        return fixtureInsertLocation;
    }
    
    public SourceSpan getMethodBlockSpan(String name)
    {
        return methodSpans.get(name);
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
    
    @Override
    protected void gotTypeDef(int tdType)
    {
        classLevel++;
    }
    
    @Override
    protected void beginTypeBody(LocatableToken leftCurlyToken)
    {
        if (classLevel == 1) {
            fixtureInsertLocation = new SourceLocation(leftCurlyToken.getLine(),
                    leftCurlyToken.getColumn());
        }
    }
    
    @Override
    protected void endTypeBody(LocatableToken endCurlyToken, boolean included)
    {
    }
    
    @Override
    protected void gotTypeDefEnd(LocatableToken token, boolean included)
    {
        classLevel--;
        endElement(token, included);
        if (classLevel == 0) {
            methodInsertLocation = new SourceLocation(token.getLine(), token.getColumn());
        }
    }
    
    @Override
    protected void gotMethodDeclaration(LocatableToken token,
            LocatableToken hiddenToken)
    {
        if (elementLevel == 2) {
            inMethod = true;
            methodName = token.getText();
        }
    }
    
    @Override
    protected void gotMethodParameter(LocatableToken token)
    {
        inMethod = false; // we're not interested in methods with parameters
    }
    
    @Override
    protected void beginMethodBody(LocatableToken token)
    {
        if (inMethod) {
            methodBegin = token;
        }
    }
    
    @Override
    protected void endMethodBody(LocatableToken token, boolean included)
    {
        if (elementLevel == 2 && methodBegin != null) {
            SourceLocation start = new SourceLocation(methodBegin.getLine(), methodBegin.getColumn());
            SourceLocation end = new SourceLocation(token.getLine(), token.getColumn());
            SourceSpan ss = new SourceSpan(start, end);
            methodSpans.put(methodName, ss);
        }
    }
}
