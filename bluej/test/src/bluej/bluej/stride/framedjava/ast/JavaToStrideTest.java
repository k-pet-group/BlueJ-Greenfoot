package bluej.bluej.stride.framedjava.ast;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import bluej.stride.framedjava.ast.FilledExpressionSlotFragment;
import bluej.stride.framedjava.ast.OptionalExpressionSlotFragment;
import bluej.stride.framedjava.ast.Parser;
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

        //assertEquals("if (0);", _if("0"));

        assertEquals("return 0;", _return("0"));
        assertEquals("return 0+1;", _return("0+1"));
        assertEquals("return 0+(1+2);", _return("0+(1+2)"));
        assertEquals("return;", _return());
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

    private static void assertEquals(String javaSource, CodeElement... expectedStride)
    {
        List<CodeElement> result = Parser.javaToStride(javaSource);
        List<String> resultXML = result.stream().map(CodeElement::toXML).map(LocatableElement::toXML).collect(Collectors.toList());
        List<String> expectedXML = Arrays.stream(expectedStride).map(CodeElement::toXML).map(LocatableElement::toXML).collect(Collectors.toList());
        Assert.assertEquals("Checking XML", expectedXML, resultXML);
    }
}
