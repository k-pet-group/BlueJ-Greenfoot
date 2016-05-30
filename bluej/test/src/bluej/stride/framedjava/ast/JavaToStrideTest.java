package bluej.stride.framedjava.ast;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import bluej.parser.AssistContent;
import bluej.stride.framedjava.elements.CallElement;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.ConstructorElement;
import bluej.stride.framedjava.elements.IfElement;
import bluej.stride.framedjava.elements.LocatableElement;
import bluej.stride.framedjava.elements.NormalMethodElement;
import bluej.stride.framedjava.elements.ReturnElement;
import bluej.stride.framedjava.elements.VarElement;
import bluej.stride.framedjava.elements.WhileElement;
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
        assertEquals("return 0+1;", _return("0 + 1"));
        assertEquals("return 0+(1+2);", _return("0 + ( 1 + 2 )"));
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
        assertEquals("go();", _call("go ( )"));
        assertEquals("move(6 + 7);", _call("move ( 6 + 7 )"));
        assertEquals("getFoo().move(6 + 7);", _call("getFoo ( ) . move ( 6 + 7 )"));
        
        // Assignments will become call-frames initially, even though on insertion
        // as real code, CallFrame will check and self-convert to AssignFrame:
        assertEquals("x = getX();", _call("x = getX ( )"));
    }
    
    @Test
    public void testExpression()
    {
        assertExpression("0", "0");
        assertExpression("0 + 1", "0+1");
        assertExpression("0 + 1", "0 + 1");
        assertExpression("0 + 1", "0  +  1");
        assertExpression("0 >= 1", "0  >=  1");
        assertExpression("0 > = 1", "0  > =  1");
        assertExpression("new Foo ( )", "new Foo()");
        assertExpression("newFoo ( )", "newFoo()");
        assertExpression("<:", "instanceof");
        assertExpression("a <: b", "a instanceof b");
        assertExpression("a instanceofb", "a instanceofb");
        // Confusingly, if you put <: in from the Java side, you should get < : (two operators):
        assertExpression("a < : b", "a <: b");
        
        //TODO test casts (seems to be a problem)
    }
    
    @Test
    public void testInstanceof()
    {
        assertEquals("while (a instanceof b) {}", _while(new FilledExpressionSlotFragment("a <: b", "a instanceof b")));
        assertEquals("if (a instanceof b) {} else if (c instanceof d) {}", _ifElseIf(new FilledExpressionSlotFragment("a <: b", "a instanceof b"), l(), l(new FilledExpressionSlotFragment("c <: d", "c instanceof d")), l(l()), null));
        assertEquals("foo(a instanceof b);", _call(new CallExpressionSlotFragment("foo ( a <: b )", "foo ( a instanceof b )")));
        assertEquals("return (a instanceof b);", _return(new OptionalExpressionSlotFragment("( a <: b )", "( a instanceof b )")));
        assertEqualsClass("C() {super(a instanceof b);}", _constructorDelegate(null, AccessPermission.PROTECTED, l(), l(), SuperThis.SUPER, new SuperThisParamsExpressionFragment("a <: b", "a instanceof b"), l()));
    }
    
    @Test
    public void testMethod()
    {
        assertEqualsClass("public void foo() { return; }", _method(null, AccessPermission.PUBLIC, false, false, "void", "foo", Collections.emptyList(), Collections.emptyList(), Arrays.asList(_return())));
        assertEqualsClass("public final void foo(int x) { return; }", _method(null, AccessPermission.PUBLIC, false, true, "void", "foo", Arrays.asList(_param("int", "x")), Collections.emptyList(), Arrays.asList(_return())));

        assertEqualsClass("Foo(int x, String y) throws IOException { }",
            _constructor(null, AccessPermission.PROTECTED, Arrays.asList(_param("int", "x"), _param("String", "y")),
                Arrays.asList("IOException"), Arrays.asList()));
     
        assertEqualsClass("/** Comment */ private void foo() {}", _method("Comment", AccessPermission.PRIVATE, false, false, "void", "foo", Collections.emptyList(), Collections.emptyList(), Collections.emptyList()));
        assertEqualsClass("/** Multi\n * line \n * comment.\n*/ private static final java.lang.String foo() throws IOException, NullPointerException {}",
            _method("Multi line comment.", AccessPermission.PRIVATE, true, true, "java.lang.String", "foo",
                Collections.emptyList(), Arrays.asList("IOException", "NullPointerException"), Collections.emptyList()));
        assertEqualsClass("/** First\nPara.\n\nSecond\nPara.\n\n\nThird Para.*/\nprotected Foo(){}",
            _constructor("First Para.\nSecond Para.\n\nThird Para.",
                AccessPermission.PROTECTED, Collections.emptyList(), Collections.emptyList(), Collections.emptyList()));
        
        assertEqualsClass("// X\npublic Bar() { this(0); }", _constructorDelegate("X", AccessPermission.PUBLIC, Collections.emptyList(), Collections.emptyList(), SuperThis.THIS, "0", Collections.emptyList()));
        assertEqualsClass("C() {this2(0);}", _constructor(null, AccessPermission.PROTECTED, l(), l(), l(_call("this2 ( 0 )"))));
        assertEqualsClass("C() {super(2, 3);}", _constructorDelegate(null, AccessPermission.PROTECTED, l(), l(), SuperThis.SUPER, "2,3", l()));
        //TODO test: abstract methods, interface methods (incl default), generic methods (either fail or do our best)
    }
    
    @Test
    public void testField()
    {
        assertEqualsClass("int x;", _var(AccessPermission.PROTECTED, false, false, "int", "x", null));
        assertEqualsClass("public static final int CONST=7;", _var(AccessPermission.PUBLIC, true, true, "int", "CONST", "7"));
        assertEqualsClass("private final bool b = 7, c=false;",
            _var(AccessPermission.PRIVATE, false, true, "bool", "b", "7"),
            _var(AccessPermission.PRIVATE, false, true, "bool", "c", "false"));
        //TODO add a couple more tests
    }
    
    private VarElement _var(AccessPermission access, boolean _static, boolean _final, String type, String name, String init)
    {
        return new VarElement(null, new AccessPermissionFragment(access), _static, _final, new TypeSlotFragment(type, type), new NameDefSlotFragment(name), init == null ? null : filled(init), true);
    }
    
    private ParamFragment _param(String type, String name)
    {
        return new ParamFragment(new TypeSlotFragment(type, type), new NameDefSlotFragment(name));
    }

    private CodeElement _method(String comment, AccessPermission accessPermission, boolean _static, boolean _final, String returnType, String name, List<ParamFragment> params, List<String> _throws, List<CodeElement> body)
    {
        return new NormalMethodElement(null, new AccessPermissionFragment(accessPermission), _static, _final, new TypeSlotFragment(returnType, returnType), new NameDefSlotFragment(name), params, _throws.stream().map(t -> new ThrowsTypeFragment(new TypeSlotFragment(t, t))).collect(Collectors.toList()), body, new JavadocUnit(comment), true);
    }

    private CodeElement _constructor(String comment, AccessPermission accessPermission, List<ParamFragment> params, List<String> _throws, List<CodeElement> body)
    {
        return new ConstructorElement(null, new AccessPermissionFragment(accessPermission), params, _throws.stream().map(t -> new ThrowsTypeFragment(new TypeSlotFragment(t, t))).collect(Collectors.toList()), null, null, body, new JavadocUnit(comment), true);
    }

    private CodeElement _constructorDelegate(String comment, AccessPermission accessPermission, List<ParamFragment> params, List<String> _throws, SuperThis superThis, String superThisArgs, List<CodeElement> body)
    {
        return new ConstructorElement(null, new AccessPermissionFragment(accessPermission), params, _throws.stream().map(t -> new ThrowsTypeFragment(new TypeSlotFragment(t, t))).collect(Collectors.toList()), new SuperThisFragment(superThis), new SuperThisParamsExpressionFragment(superThisArgs, superThisArgs), body, new JavadocUnit(comment), true);
    }

    private CodeElement _constructorDelegate(String comment, AccessPermission accessPermission, List<ParamFragment> params, List<String> _throws, SuperThis superThis, SuperThisParamsExpressionFragment superThisArgs, List<CodeElement> body)
    {
        return new ConstructorElement(null, new AccessPermissionFragment(accessPermission), params, _throws.stream().map(t -> new ThrowsTypeFragment(new TypeSlotFragment(t, t))).collect(Collectors.toList()), new SuperThisFragment(superThis), superThisArgs, body, new JavadocUnit(comment), true);
    }

    private static void assertExpression(String expectedStride, String original)
    {
        Assert.assertEquals(expectedStride, JavaStrideParser.replaceInstanceof(original));
    }

    private CallElement _call(String call)
    {
        return _call(new CallExpressionSlotFragment(call, call));
    }

    private CallElement _call(CallExpressionSlotFragment call)
    {
        return new CallElement(null, call, true);
    }

    private ReturnElement _return()
    {
        return new ReturnElement(null, new OptionalExpressionSlotFragment("", ""), true);
    }

    private ReturnElement _return(String s)
    {
        return _return(new OptionalExpressionSlotFragment(s, s));
    }

    private ReturnElement _return(OptionalExpressionSlotFragment s)
    {
        return new ReturnElement(null, s, true);
    }

    private WhileElement _while(String expression, CodeElement... body)
    {
        return _while(filled(expression), body);
    }

    private WhileElement _while(FilledExpressionSlotFragment expression, CodeElement... body)
    {
        return new WhileElement(null, expression, Arrays.asList(body), true);
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

    private IfElement _ifElseIf(FilledExpressionSlotFragment expression, List<CodeElement> body, List<FilledExpressionSlotFragment> expressions, List<List<CodeElement>> elseIfBodies, List<CodeElement> elseBody)
    {
        return new IfElement(null, expression, body, expressions, elseIfBodies, elseBody, true);
    }
    
    private FilledExpressionSlotFragment filled(String e)
    {
        return new FilledExpressionSlotFragment(e, e);
    }
        
    // Short-hand for Arrays.asList
    private static <T> List<T> l(T... items)
    {
        return Arrays.asList(items);
    }

    private static void assertEquals(String javaSource, CodeElement... expectedStride)
    {
        List<CodeElement> result = Parser.javaToStride(javaSource, Parser.JavaContext.STATEMENT);
        List<String> resultXML = result.stream().map(CodeElement::toXML).map(LocatableElement::toXML).collect(Collectors.toList());
        List<String> expectedXML = Arrays.stream(expectedStride).map(CodeElement::toXML).map(LocatableElement::toXML).collect(Collectors.toList());
        Assert.assertEquals("Checking XML", expectedXML, resultXML);
    }

    private static void assertEqualsClass(String javaSource, CodeElement... expectedStride)
    {
        List<CodeElement> result = Parser.javaToStride(javaSource, Parser.JavaContext.CLASS_MEMBER);
        List<String> resultXML = result.stream().map(CodeElement::toXML).map(LocatableElement::toXML).collect(Collectors.toList());
        List<String> expectedXML = Arrays.stream(expectedStride).map(CodeElement::toXML).map(LocatableElement::toXML).collect(Collectors.toList());
        Assert.assertEquals("Checking XML", expectedXML, resultXML);
    }
}
