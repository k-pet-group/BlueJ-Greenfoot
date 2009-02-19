/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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
import java.util.*;

import antlr.*;
import antlr.collections.AST;
import bluej.parser.ast.*;
import bluej.parser.ast.gen.*;

/**
 * @author Andrew
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class UnitTestAnalyzer
{
    private boolean parsedOk;
    private AST rootAST;
    private LocatableAST unitTestAST;
    
    /**
     * Analyse unit test source code.
     */
    public UnitTestAnalyzer(Reader r)
    {
        JavaAnalyzer ja = null;
        
        parsedOk = false;
        
        try {
            ja = new JavaAnalyzer(r);
        }
        catch (RecognitionException re) {
            re.printStackTrace();
        }
        catch (TokenStreamException tse) {
            tse.printStackTrace();
        }
            
        rootAST = new ASTFactory().create(0, "AST ROOT");

        UnitTestParser tparse = new UnitTestParser();
        tparse.setASTNodeClass("bluej.parser.ast.LocatableAST");
        try {
            tparse.compilationUnit(ja.getAST());
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        rootAST.setFirstChild(tparse.getAST());

        unitTestAST = findUnitTestClass();

        if (unitTestAST != null)
            parsedOk = true;            
    }

    /**
     * Find the AST of the only class declared in this file that is
     * public. This class will be the unit test class.
     */
    private LocatableAST findUnitTestClass()
    {
        // loop through the classes defined in the src file
        LocatableAST firstClass = (LocatableAST) rootAST.getFirstChild();

        while (firstClass != null) {
            LocatableAST leftCurly = (LocatableAST) firstClass.getFirstChild();
            if(leftCurly != null) {
                LocatableAST rightCurly = (LocatableAST) leftCurly.getNextSibling();
                if (rightCurly != null) {
                    LocatableAST modifiers = (LocatableAST) rightCurly.getNextSibling();

                    LocatableAST modifierTokens = (LocatableAST) modifiers.getFirstChild();
                    while(modifierTokens != null) {
                        if (modifierTokens.getText().equals("public")) {
                            return firstClass;
                        }
                    }
                }
            }

            firstClass = (LocatableAST) firstClass.getNextSibling();
        }
        return null;
    }

    private LocatableAST findUnitTestOpeningBracket()
    {
        return (LocatableAST) unitTestAST.getFirstChild();
    }
    
    private LocatableAST findUnitTestClosingBracket()
    {
        return (LocatableAST) unitTestAST.getFirstChild().getNextSibling();
        
    }

    private LocatableAST findUnitTestObjectBlock()
    {
        LocatableAST childAST = (LocatableAST) unitTestAST.getFirstChild().getNextSibling().getNextSibling();

        while(childAST != null) {
            if(childAST.getType() == UnitTestParserTokenTypes.OBJBLOCK) {
                return childAST;
            }
            childAST = (LocatableAST) childAST.getNextSibling();            
        }
        return null;
    }

    /**
     * Extract from the unit testing source the list of source spans
     * for the fields declared in the unit test class.
     * 
     * ie
     *
     * class FooBar {
     *    private int a = 10;
     *    java.util.HashMap h,i,j = null;
     *    public String aString;
     * }
     * gives us a list with SourceSpan objects encompassing
     *   p in private to ;
     *   j in java to ;
     *   p in public to ;
     *
     * The list will be ordered in the order that the fields appear in the src.
     */
    public List getFieldSpans()
    {
        // we are creating a list of AST nodes
        LinkedList l = new LinkedList();

        // the first AST in this OBJBLOCK
        LocatableAST childAST = (LocatableAST) (findUnitTestObjectBlock()).getFirstChild();

        // the children in an object block are a list of variable definitions
        // and method definitions
        while(childAST != null) {
            // we are only interested in variable definitions (fields)
            if(childAST.getType() == UnitTestParserTokenTypes.VARIABLE_DEF) {
                // potentially VARIABLE_DEF could look like this
                // we need to find the first type token (in this case
                // "java") and the semicolon
                
                // ( VARIABLE_DEF ( . ( . java lang ) String ) ; )

                // find the complete span of nodes for this variable definition
                LocatableAST startSibling = null, endSibling = null;
                
                startSibling = (LocatableAST) childAST.getFirstChild();
                if(startSibling != null) {
                    // the semicolon is always the sibling of the first child found
                    endSibling = (LocatableAST) startSibling.getNextSibling();
                    
                    // however, we need to keep going down with startSibling to find
                    // the left most token
                    while (startSibling.getFirstChild() != null)
                        startSibling = (LocatableAST) startSibling.getFirstChild();
                }
                                    
                if (startSibling != null && endSibling != null) {                    
                    l.add(new SourceSpan(new SourceLocation(startSibling.getLine(),
                                                            startSibling.getColumn()),
                                         new SourceLocation(endSibling.getLine(),
                                                            endSibling.getColumn())));
                }
            }               
            childAST = (LocatableAST) childAST.getNextSibling();            
        }            
        return l;
    }

    /**
     * Extract from the unit testing source
     * the opening and closing bracket locations for the method 'methodName'.
     * We select only methods that do not have any parameters (all unit test
     * methods take no arguements).
     * ie
     *
     * class FooBar {
     *    public void setUp() {
     *       // do something
     *       i++;
     *    }
     * }
     * gives us a SourceSpan object from the second "{" to the first "}"
     */
    public SourceSpan getMethodBlockSpan(String methodName)
    {
        // the children in an object block are a list of variable defs and method defs
        for(LocatableAST childAST = (LocatableAST) (findUnitTestObjectBlock()).getFirstChild();
            childAST != null;
            childAST = (LocatableAST) childAST.getNextSibling()){
            
            // we are only interested in method definitions
            if(childAST.getType() != UnitTestParserTokenTypes.METHOD_DEF)
                continue;

            // a METHOD_DEF with no actual parameters looks like this
            // ( METHOD_DEF ( methodName PARAMETERS ) { } COMMENT_DEF )
            LocatableAST nameAST = null;

            // first sibling is the method name
            // we make sure that it is the method we are looking for
            nameAST = (LocatableAST) childAST.getFirstChild();
            if(nameAST == null || !nameAST.getText().equals(methodName))
                continue;

            // the first child of the name AST is the parameter block
            LocatableAST parameterAST = (LocatableAST) nameAST.getFirstChild();

            // make sure our paramter list is empty
            if (parameterAST.getFirstChild() != null)
                continue;

            LocatableAST openBracketAST = (LocatableAST) nameAST.getNextSibling();
            if (openBracketAST == null)
                continue;
                
            LocatableAST closeBracketAST = (LocatableAST) openBracketAST.getNextSibling();
            if(closeBracketAST == null)
                continue;

            return new SourceSpan(new SourceLocation(openBracketAST.getLine(),
                                                     openBracketAST.getColumn()),
                                new SourceLocation(closeBracketAST.getLine(),
                                                   closeBracketAST.getColumn()));
        }            
        return null;
    }

    /**
     * Extract from the unit test source a source location where
     * we should insert declarations of fields (that will become
     * the classes fixtures).
     * 
     * @return
     */
    public SourceLocation getFixtureInsertLocation()
    {
        LocatableAST a = findUnitTestOpeningBracket();
        return new SourceLocation(a.getLine(),a.getColumn());
    }

    /**
     * Extract from the unit test source a source location where
     * we can insert new methods.
     * 
     * @return
     */
    public SourceLocation getNewMethodInsertLocation()
    {
        LocatableAST a = findUnitTestClosingBracket();
        return new SourceLocation(a.getLine(),a.getColumn());
        
    }
  
}
