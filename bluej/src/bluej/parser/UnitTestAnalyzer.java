package bluej.parser;

// make sure we don't use the JavaLexer in bluej.parser
import bluej.parser.ast.gen.JavaLexer;
import bluej.parser.ast.*;
import bluej.parser.ast.gen.*;

import java.io.Reader;
import java.util.*;

import antlr.*;
import antlr.collections.AST;

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
        parsedOk = false;
        
        // create a scanner that reads from the input stream passed to us
        JavaLexer lexer = new JavaLexer(r);
        lexer.setTokenObjectClass("bluej.parser.ast.LocatableToken");

        // with a tab size of one, the rows and column numbers that
        // locatable token returns are model coordinates in the editor
        // (not view coordinates)
        // ie a keyword may appear to start at column 14 because of tabs
        // but in the actual document model its really at column 4
        // so we set our tabsize to 1 so that it maps directly to the
        // document model
        lexer.setTabSize(1);

        // create a filter to handle our comments
        TokenStreamHiddenTokenFilter filter;
        filter = new TokenStreamHiddenTokenFilter(lexer);
        filter.hide(JavaRecognizer.SL_COMMENT);
        filter.hide(JavaRecognizer.ML_COMMENT);

        // Create a parser that reads from the scanner
        JavaRecognizer parser = new JavaRecognizer(filter);
        parser.setASTNodeClass("bluej.parser.ast.LocatableAST");

        // start parsing at the compilationUnit rule
        try {
            parser.compilationUnit();
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
            tparse.compilationUnit(parser.getAST());
        }
        catch (RecognitionException e) {
            e.printStackTrace();
        }

        rootAST.setFirstChild(tparse.getAST());

        parsedOk = true;            
    }

    /**
     * Find the AST of the only class declared in this file that is
     * public. This class will be the unit test class.
     */
    public AST getUnitTestClass()
    {
        if (unitTestAST != null)
            return unitTestAST;

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
                            unitTestAST = firstClass;
                            return unitTestAST;
                        }
                    }
                }
            }

            firstClass = (LocatableAST) firstClass.getNextSibling();
        }

        return null;
    }

    /**
     * Given an OBJBLOCK (generally from a class definition), we extract
     * a list of fields declared in the OBJBLOCK
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
     * The list will be ordered in the order that the variables appear in the src.
     */
    public static List getVariableSourceSpans(AST objBlock)
    {
        if (!(objBlock instanceof LocatableAST))
            throw new IllegalArgumentException("using unit test parser with wrong AST type");

        // we are creating a list of AST nodes
        LinkedList l = new LinkedList();

        // the first AST in this OBJBLOCK
        LocatableAST childAST = (LocatableAST) ((BaseAST)objBlock).getFirstChild();

        // the children in an object block are a list of variable definitions
        // and method definitions
        while(childAST != null) {
            // we are only interested in variable definitions
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
     * Given an OBJBLOCK (generally from a class definition), we extract
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
    public static SourceSpan getMethodBlockSourceSpan(AST objBlock, String methodName)
    {
        if (!(objBlock instanceof LocatableAST))
            throw new IllegalArgumentException("using unit test parser with wrong AST type");

        // the children in an object block are a list of variable defs and method defs
        for(LocatableAST childAST = (LocatableAST) ((BaseAST)objBlock).getFirstChild();
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


    public static LocatableAST getOpeningBracketSelection(AST classBlock)
    {
        if (!(classBlock instanceof LocatableAST))
            throw new IllegalArgumentException("wrong AST type");

        return (LocatableAST) classBlock.getNextSibling();
    }

    public static LocatableAST getMethodInsertSelection(AST classBlock)
    {
        if (!(classBlock instanceof LocatableAST))
            throw new IllegalArgumentException("wrong AST type");

        return (LocatableAST) classBlock.getNextSibling();
    }

    {
/*            java.util.List variables = null;
            SourceSpan setupSpan = null;
            LocatableAST openingBracket = null;
            LocatableAST methodInsert = null;

            openingBracket = (LocatableAST) firstClass.getFirstChild();
            methodInsert = (LocatableAST) firstClass.getFirstChild().getNextSibling();
            
            BaseAST childAST = (BaseAST) methodInsert.getNextSibling();

            while(childAST != null) {
                if(childAST.getType() == UnitTestParserTokenTypes.OBJBLOCK) {
                    
                    variables = UnitTestParser.getVariableSourceSpans(childAST);
                    setupSpan = UnitTestParser.getMethodBlockSourceSpan(childAST, "setUp");
                    break;
                }               
                childAST = (BaseAST) childAST.getNextSibling();            
            }   */

    }
}
