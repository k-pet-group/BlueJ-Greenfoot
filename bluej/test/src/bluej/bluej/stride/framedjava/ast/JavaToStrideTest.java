package bluej.bluej.stride.framedjava.ast;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import bluej.stride.framedjava.ast.CallExpressionSlotFragment;
import bluej.stride.framedjava.ast.FilledExpressionSlotFragment;
import bluej.stride.framedjava.ast.OptionalExpressionSlotFragment;
import bluej.stride.framedjava.ast.Parser;
import bluej.stride.framedjava.elements.CallElement;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.IfElement;
import bluej.stride.framedjava.elements.LocatableElement;
import bluej.stride.framedjava.elements.ReturnElement;
import bluej.stride.framedjava.elements.WhileElement;
import nu.xom.Element;
import org.junit.Assert;
import org.junit.Test;

/**
 * Note: some of the test cases are not semantically valid, but as long as they are syntactically valid
 * (in Java and Stride) that is fine for parser tests.
 */
public class JavaToStrideTest
{
    @Test
    public void testStatements()
    {
        assertEquals("while (true);", _while("true"));
        assertEquals("while (true) while (false);", _while("true", _while("false")));

        assertEquals("while (true) {}", _while("true"));
        assertEquals("while (true) {while (false); while (0);}", _while("true", _while("false"), _while("0")));

        assertEquals("while (0) {while (1) { while (2) {} } while (3) while (4); while(5); }",
                _while("0", _while("1", _while("2")), _while("3", _while("4")), _while("5")));

        assertEquals("return 0;", _return("0"));
        assertEquals("return 0+1;", _return("0+1"));
        assertEquals("return 0+(1+2);", _return("0+(1+2)"));
        assertEquals("return;", _return());
    }
    
    @Test
    public void testIf()
    {
        assertEquals("if (0);", _if("0"));
        assertEquals("if (0) return 1;", _if("0", _return("1")));
        assertEquals("if (0) return 1; else return 2;", _ifElse("0", Arrays.asList(_return("1")), Arrays.asList(_return("2"))));
        assertEquals("if (0) return 1; else if (2) return 3; else if (4) return 5; else return 6;",
            _ifElseIf("0", Arrays.asList(_return("1")),
                      Arrays.asList("2", "4"), Arrays.asList(Arrays.asList(_return("3")), Arrays.asList(_return("5"))),
                      Arrays.asList(_return("6"))));
        assertEquals("if (0) return 1; else if (2) return 3; else if (4) return 5;",
            _ifElseIf("0", Arrays.asList(_return("1")),
                Arrays.asList("2", "4"), Arrays.asList(Arrays.asList(_return("3")), Arrays.asList(_return("5"))),
                null));
    }
    
    @Test
    public void testCall()
    {
        assertEquals("go();", _call("go()"));
        assertEquals("move(6 + 7);", _call("move(6 + 7)"));
        assertEquals("getFoo().move(6 + 7);", _call("getFoo().move(6 + 7)"));
        
        // Assignments will become call-frames initially, even though on insertion
        // as real code, CallFrame will check and self-convert to AssignFrame:
        assertEquals("x = getX();", _call("x = getX()"));
    }
    
    private CallElement _call(String call)
    {
        return new CallElement(null, new CallExpressionSlotFragment(call, call), true);
    }

    private ReturnElement _return()
    {
        return new ReturnElement(null, new OptionalExpressionSlotFragment("", ""), true);
    }

    private ReturnElement _return(String s)
    {
        return new ReturnElement(null, new OptionalExpressionSlotFragment(s, s), true);
    }

    private WhileElement _while(String expression, CodeElement... body)
    {
        return new WhileElement(null, new FilledExpressionSlotFragment(expression, expression), Arrays.asList(body), true);
    }

    // If without any elses
    private IfElement _if(String expression, CodeElement... body)
    {
        return _ifElse(expression, Arrays.asList(body), null);
    }

    // If with an else
    private IfElement _ifElse(String expression, List<CodeElement> body, List<CodeElement> elseBody)
    {
        return _ifElseIf(expression, body, Collections.emptyList(), Collections.emptyList(), elseBody);
    }

    private IfElement _ifElseIf(String expression, List<CodeElement> body, List<String> expressions, List<List<CodeElement>> elseIfBodies, List<CodeElement> elseBody)
    {
        return new IfElement(null, filled(expression), body, expressions.stream().map(this::filled).collect(Collectors.toList()), elseIfBodies, elseBody, true);
    }
    
    private FilledExpressionSlotFragment filled(String e)
    {
        return new FilledExpressionSlotFragment(e, e);
    }
        

    private static void assertEquals(String javaSource, CodeElement... expectedStride)
    {
        List<CodeElement> result = Parser.javaToStride(javaSource);
        List<String> resultXML = result.stream().map(CodeElement::toXML).map(LocatableElement::toXML).collect(Collectors.toList());
        List<String> expectedXML = Arrays.stream(expectedStride).map(CodeElement::toXML).map(LocatableElement::toXML).collect(Collectors.toList());
        Assert.assertEquals("Checking XML", expectedXML, resultXML);
    }
}
