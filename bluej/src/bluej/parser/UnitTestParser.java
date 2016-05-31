/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2016  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.parser;

import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;

/**
 * A parser which extracts certain information needed for BlueJ's unit test
 * (junit) functionality.
 * 
 * @author Davin McCall
 */
public class UnitTestParser extends JavaParser
{
    private int classLevel = 0; // level of class nesting
    private boolean inMethod = false; // are we in an interesting method
    private String methodName;
    private LocatableToken methodBegin;
    private boolean isPublic = false;
    private boolean haveClassInfo = false;
    
    private List<SourceSpan> fieldSpans = new LinkedList<SourceSpan>();
    private SourceLocation methodInsertLocation;
    private SourceLocation fixtureInsertLocation;
    private Map<String,SourceSpan> methodSpans = new HashMap<String,SourceSpan>();
    
    private Stack<SourceLocation> fieldStarts = new Stack<SourceLocation>();
    
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
    
    @Override
    protected void error(String msg, int beginLine, int beginColumn, int endLine, int endColumn)
    {
        throw new RuntimeException("Parse error: " + msg);
    }
        
    @Override
    protected void gotModifier(LocatableToken token)
    {
        if (token.getType() == JavaTokenTypes.LITERAL_public) {
            isPublic = true;
        }
    }
    
    @Override
    protected void modifiersConsumed()
    {
        isPublic = false;
    }
    
    @Override
    protected void gotField(LocatableToken first, LocatableToken idToken, boolean initExpressionFollows)
    {
        if (classLevel == 1 && !haveClassInfo) {
            fieldStarts.push(new SourceLocation(first.getLine(), first.getColumn()));
        }
    }
    
    @Override
    protected void endFieldDeclarations(LocatableToken token, boolean included)
    {
        if (classLevel == 1 && !haveClassInfo) {
            SourceLocation start = fieldStarts.pop();
            SourceLocation end = new SourceLocation(token.getEndLine(), token.getEndColumn());
            SourceSpan ss = new SourceSpan(start, end);
            fieldSpans.add(ss);
        }
    }
    
    @Override
    protected void gotTypeDef(LocatableToken firstToken, int tdType)
    {
        classLevel++;
        if (haveClassInfo && isPublic) {
            // A public class overrides a non-public class
            haveClassInfo = false;
            fieldSpans = new LinkedList<SourceSpan>();
            methodSpans = new HashMap<String,SourceSpan>();
        }
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
    protected void gotTypeDefEnd(LocatableToken token, boolean included)
    {
        classLevel--;
        endElement(token, included);
        if (classLevel == 0) {
            haveClassInfo = true;
            methodInsertLocation = new SourceLocation(token.getLine(), token.getColumn());
        }
    }
    
    @Override
    protected void gotMethodDeclaration(LocatableToken token,
                                        LocatableToken hiddenToken)
    {
        if (classLevel == 1 && ! haveClassInfo) {
            inMethod = true;
            methodName = token.getText();
        }
    }
    
    @Override
    protected void gotMethodParameter(LocatableToken token, LocatableToken ellipsisToken)
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
        if (classLevel == 1 && !haveClassInfo && methodBegin != null) {
            SourceLocation start = new SourceLocation(methodBegin.getLine(), methodBegin.getColumn());
            SourceLocation end = new SourceLocation(token.getEndLine(), token.getEndColumn());
            SourceSpan ss = new SourceSpan(start, end);
            methodSpans.put(methodName, ss);
        }
    }
}
