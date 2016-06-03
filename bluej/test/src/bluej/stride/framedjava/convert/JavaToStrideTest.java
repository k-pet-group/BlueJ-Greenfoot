package bluej.stride.framedjava.convert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.stride.framedjava.ast.AccessPermission;
import bluej.stride.framedjava.ast.AccessPermissionFragment;
import bluej.stride.framedjava.ast.CallExpressionSlotFragment;
import bluej.stride.framedjava.ast.ExpressionSlotFragment;
import bluej.stride.framedjava.ast.FilledExpressionSlotFragment;
import bluej.stride.framedjava.ast.JavadocUnit;
import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.ast.OptionalExpressionSlotFragment;
import bluej.stride.framedjava.ast.PackageFragment;
import bluej.stride.framedjava.ast.ParamFragment;
import bluej.stride.framedjava.ast.Parser;
import bluej.stride.framedjava.ast.SuperThis;
import bluej.stride.framedjava.ast.SuperThisFragment;
import bluej.stride.framedjava.ast.SuperThisParamsExpressionFragment;
import bluej.stride.framedjava.ast.ThrowsTypeFragment;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.elements.AssignElement;
import bluej.stride.framedjava.elements.BreakElement;
import bluej.stride.framedjava.elements.CallElement;
import bluej.stride.framedjava.elements.CaseElement;
import bluej.stride.framedjava.elements.ClassElement;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.CommentElement;
import bluej.stride.framedjava.elements.ConstructorElement;
import bluej.stride.framedjava.elements.ForeachElement;
import bluej.stride.framedjava.elements.IfElement;
import bluej.stride.framedjava.elements.ImportElement;
import bluej.stride.framedjava.elements.InterfaceElement;
import bluej.stride.framedjava.elements.LocatableElement;
import bluej.stride.framedjava.elements.MethodProtoElement;
import bluej.stride.framedjava.elements.NormalMethodElement;
import bluej.stride.framedjava.elements.ReturnElement;
import bluej.stride.framedjava.elements.SwitchElement;
import bluej.stride.framedjava.elements.ThrowElement;
import bluej.stride.framedjava.elements.TryElement;
import bluej.stride.framedjava.elements.VarElement;
import bluej.stride.framedjava.elements.WhileElement;
import bluej.utility.Utility;
import nu.xom.Element;
import org.junit.Assert;
import org.junit.Test;
import static bluej.stride.framedjava.convert.Expression.uniformSpacing;

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

        assertEquals("break;", new BreakElement(null, true));

        assertEquals("throw e;", new ThrowElement(null, filled("e"), true));
        assertEquals("throw new IOException();", new ThrowElement(null, filled("new IOException ( )"), true));

        assertEquals("try { return 0; } catch (Exception e) { return 1; }", _try(l(_return("0")), l(type("Exception")), l(name("e")), l(l(_return("1"))), null));
        assertEquals("try { return 0; } finally { return 1;}", _try(l(_return("0")), l(), l(), l(), l(_return("1"))));
        assertEquals("try { return 0; } catch (E1 e1) { return 1; } catch (E2 e2) { return 2; } finally { return -1;}", _try(l(_return("0")), l(type("E1"), type("E2")), l(name("e1"), name("e2")), l(l(_return("1")), l(_return("2"))), l(_return("- 1"))));
        assertEquals("try { return 0; } catch (E1A|E1B e1) { return 1; } catch (E2A|E2B|E2C e2) { return 2; } finally { return -1;}",
                _try(l(_return("0")),
                        l(type("E1A"), type("E1B"), type("E2A"), type("E2B"), type("E2C")),
                        l(name("e1"), name("e1"), name("e2"), name("e2"), name("e2")),
                        l(l(_return("1")), l(_return("1")), l(_return("2")), l(_return("2")), l(_return("2"))), l(_return("- 1"))));

        // Failing random test:
        assertEquals("try { } catch (java.lang.String x) { while (0) { } } catch (int x) { }",
                _try(l(), l(type("java.lang.String"), type("int")), l(name("x"), name("x")), l(l(_while("0")), l()), null));

        assertEquals("switch (0) { case 1: break; }", _switch("0", l(_case("1", new BreakElement(null, true))), null));
        assertEquals("switch (0) { default: break; }", _switch("0", l(), l(new BreakElement(null, true))));

        assertEquals("for (int x : xs) return;", _forEach("int", "x", "xs", _return()));
        assertEquals("for (int x : (int[])xs) return;", _forEach("int", "x", "( int [ ] ) xs", _return()));

        // i = i + 1 gets turned into a call because we don't parse the expression
        assertEquals("for (int i = 0; i < 10; i = i + 1) return;",
                _var(null, false, false, "int", "i", filled("0")),
                _while("i < 10", _return(), _assign("i", "i + 1"))
        );
        // However, i++ becomes an assignment because we do transform it:
        assertEquals("for (int i = 0; i < 10; i++) return;",
            _var(null, false, false, "int", "i", filled("0")),
            _while("i < 10", _return(), _assign("i", "i + 1"))
        );
        assertEquals("for (int i = 0, j, k = (double)7, l; i < 10; i = i + 1) {return 0; return 1;}",
                _var(null, false, false, "int", "i", filled("0")),
                _var(null, false, false, "int", "j", null),
                _var(null, false, false, "int", "k", filled("( double ) 7")),
                _var(null, false, false, "int", "l", null),
                _while("i < 10", _return("0"), _return("1"), _assign("i", "i + 1"))
        );
    }
    
    @Test
    public void testIncDec()
    {
        assertEquals("++i;", _assign("i", "i + 1"));
        assertEquals("i++;", _assign("i", "i + 1"));
        assertEquals("++i[j.k];", _assign("i [ j . k ]", "i [ j . k ] + 1"));
        assertEquals("i[j.k]++;", _assign("i [ j . k ]", "i [ j . k ] + 1"));
        assertEquals("--i;", _assign("i", "i - 1"));
        assertEquals("i--;", _assign("i", "i - 1"));
        assertEquals("--i[j.k];", _assign("i [ j . k ]", "i [ j . k ] - 1"));
        assertEquals("i[j.k]--;", _assign("i [ j . k ]", "i [ j . k ] - 1"));
        //TODO assert one that shouldn't be supported.
    }
    
    @Test
    public void testAssign()
    {
        assertEquals("i += 1;", _assign("i", "i + 1"));
    }

    private CodeElement _assign(String lhs, String rhs)
    {
        return new AssignElement(null, filled(lhs), filled(rhs), true);
    }

    private CodeElement _forEach(String type, String var, String of, CodeElement... body)
    {
        return new ForeachElement(null, type(type), name(var), filled(of), l(body), true);
    }

    private static TypeSlotFragment type(String t)
    {
        return new TypeSlotFragment(t, t);
    }

    private static NameDefSlotFragment name(String n)
    {
        return new NameDefSlotFragment(n);
    }

    private SwitchElement _switch(String exp, List<CaseElement> cases, List<CodeElement> defaultBody)
    {
        return new SwitchElement(null, filled(exp), cases, defaultBody, true);
    }

    private CaseElement _case(String exp, CodeElement... body)
    {
        return new CaseElement(null, filled(exp), l(body), true);
    }

    private TryElement _try(List<CodeElement> tryContents, List<TypeSlotFragment> catchTypes, List<NameDefSlotFragment> catchNames, List<List<CodeElement>> catchContents, List<CodeElement> finallyContents)
    {
        return new TryElement(null, tryContents, catchTypes, catchNames, catchContents, finallyContents, true);
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
        
        assertEquals("x = getX();", _assign("x", "getX ( )"));
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

        // TODO test += etc, and ++
    }
    
    @Test
    public void testInstanceof()
    {
        assertEquals("while (a instanceof b) {}", _while(new FilledExpressionSlotFragment("a <: b", "a instanceof b")));
        assertEquals("if (a instanceof b) {} else if (c instanceof d) {}", _ifElseIf(new FilledExpressionSlotFragment("a <: b", "a instanceof b"), l(), l(new FilledExpressionSlotFragment("c <: d", "c instanceof d")), l(l()), null));
        assertEquals("foo(a instanceof b);", _call(new CallExpressionSlotFragment("foo ( a <: b )", "foo ( a instanceof b )")));
        assertEquals("return (a instanceof b);", _return(new OptionalExpressionSlotFragment("( a <: b )", "( a instanceof b )")));
        assertEqualsMember("C() {super(a instanceof b);}", _constructorDelegate(null, AccessPermission.PROTECTED, l(), l(), SuperThis.SUPER, new SuperThisParamsExpressionFragment("a <: b", "a instanceof b"), l()));
    }
    
    @Test
    public void testMethod()
    {
        assertEqualsMember("public void foo() { return; }", _method(null, AccessPermission.PUBLIC, false, false, "void", "foo", Collections.emptyList(), Collections.emptyList(), Arrays.asList(_return())));
        assertEqualsMember("public final void foo(int x) { return; }", _method(null, AccessPermission.PUBLIC, false, true, "void", "foo", Arrays.asList(_param("int", "x")), Collections.emptyList(), Arrays.asList(_return())));

        assertEqualsMember("public final void foo(int[] x, double y[][], String[] s[]) { return; }",
                _method(null, AccessPermission.PUBLIC, false, true, "void", "foo",
                        Arrays.asList(_param("int[]", "x"), _param("double[][]", "y"), _param("String[][]", "s")),
                        Collections.emptyList(), Arrays.asList(_return())));

        assertEqualsMember("Foo(int x, String y) throws IOException { }",
            _constructor(null, AccessPermission.PROTECTED, Arrays.asList(_param("int", "x"), _param("String", "y")),
                Arrays.asList("IOException"), Arrays.asList()));
     
        assertEqualsMember("/** Comment */ private void foo() {}", _method("Comment", AccessPermission.PRIVATE, false, false, "void", "foo", Collections.emptyList(), Collections.emptyList(), Collections.emptyList()));
        assertEqualsMember("// Comment\nprivate void foo() {}", _comment("Comment"), _method(null, AccessPermission.PRIVATE, false, false, "void", "foo", Collections.emptyList(), Collections.emptyList(), Collections.emptyList()));
        assertEqualsMember("/** Multi\n * line \n * comment.\n*/ private static final java.lang.String foo() throws IOException, NullPointerException {}",
            _method("Multi line comment.", AccessPermission.PRIVATE, true, true, "java.lang.String", "foo",
                Collections.emptyList(), Arrays.asList("IOException", "NullPointerException"), Collections.emptyList()));
        assertEqualsMember("/** First\nPara.\n\nSecond\nPara.\n\n\nThird Para.*/\nprotected Foo(){}",
            _constructor("First Para.\nSecond Para.\n\nThird Para.",
                AccessPermission.PROTECTED, Collections.emptyList(), Collections.emptyList(), Collections.emptyList()));
        
        assertEqualsMember("/** X*/\npublic Bar() { this(0); }", _constructorDelegate("X", AccessPermission.PUBLIC, Collections.emptyList(), Collections.emptyList(), SuperThis.THIS, "0", Collections.emptyList()));
        assertEqualsMember("C() {this2(0);}", _constructor(null, AccessPermission.PROTECTED, l(), l(), l(_call("this2 ( 0 )"))));
        assertEqualsMember("C() {super(2, 3);}", _constructorDelegate(null, AccessPermission.PROTECTED, l(), l(), SuperThis.SUPER, "2 , 3", l()));
        //TODO test: abstract methods, interface methods (incl default -- should fail), generic methods (either fail or do our best)
        
    }
    
    @Test
    public void testFieldAndVar()
    {
        assertEqualsMember("int x;", _var(AccessPermission.PROTECTED, false, false, "int", "x", null));
        assertEqualsMember("public static final int CONST=7;", _var(AccessPermission.PUBLIC, true, true, "int", "CONST", filled("7")));
        assertEqualsMember("private final bool b = 7, c=false;",
            _var(AccessPermission.PRIVATE, false, true, "bool", "b", filled("7")),
            _var(AccessPermission.PRIVATE, false, true, "bool", "c", filled("false")));
        assertEqualsMember("public static String a = null, b, c=(String)false, d;",
                _var(AccessPermission.PUBLIC, true, false, "String", "a", filled("null")),
                _var(AccessPermission.PUBLIC, true, false, "String", "b", null),
                _var(AccessPermission.PUBLIC, true, false, "String", "c", filled("( String ) false")),
                _var(AccessPermission.PUBLIC, true, false, "String", "d", null));

        assertEquals("int x;", _var(null, false, false, "int", "x", null));
        assertEquals("final int CONST=7;", _var(null, false, true, "int", "CONST", filled("7")));
        assertEquals("bool b = 7, c=false;",
                _var(null, false, false, "bool", "b", filled("7")),
                _var(null, false, false, "bool", "c", filled("false")));
        assertEquals("final String a = null, b, c=(String)false, d;",
                _var(null, false, true, "String", "a", filled("null")),
                _var(null, false, true, "String", "b", null),
                _var(null, false, true, "String", "c", filled("( String ) false")),
                _var(null, false, true, "String", "d", null));

        assertEquals("int[] x;", _var(null, false, false, "int[]", "x", null));
        assertEquals("int x[];", _var(null, false, false, "int[]", "x", null));
        assertEquals("int[] x[];", _var(null, false, false, "int[][]", "x", null));
    }

    @Test
    public void testComments()
    {
        assertEquals("//ABC", _comment("ABC"));
        assertEquals("//Declares x\nint x;", _comment("Declares x"), _var(null, false, false, "int", "x", null));
        assertEquals("//Declares x\nint x /* empty */;", _comment("Declares x empty"), _var(null, false, false, "int", "x", null));
        assertEquals("//Declares x\nint x /* empty */;int y;", _comment("Declares x empty"), _var(null, false, false, "int", "x", null), _var(null, false, false, "int", "y", null));
        assertEquals("//Declares x\nint x;int y/* empty */;", _comment("Declares x"), _var(null, false, false, "int", "x", null), _comment("empty"), _var(null, false, false, "int", "y", null));
        
        assertEquals("break; // Post-comment", new BreakElement(null, true), _comment("Post-comment"));
        assertEquals("while(true) {/*Just-comment*/}", _while("true", _comment("Just-comment")));
        assertEquals("while(true) {/*Pre-comment*/ break;}", _while("true", _comment("Pre-comment"), new BreakElement(null, true)));
        assertEquals("while(true) /*Pre-comment*/ break;", _while("true", _comment("Pre-comment"), new BreakElement(null, true)));
        assertEquals("while(true) {break; /*End-comment*/}", _while("true", new BreakElement(null, true), _comment("End-comment")));
        assertEquals("while(true) {break; }/*After-comment*/", _while("true", new BreakElement(null, true)), _comment("After-comment"));
        assertEquals("while(true) break;/*After-comment*/", _while("true", new BreakElement(null, true)), _comment("After-comment"));
        assertEquals("return 0; /*XXX*/ while(true) {}", _return("0"), _comment("XXX"), _while("true"));
    }
    
    @Test
    public void testWhole()
    {
        assertEqualsFile("class Foo {}", _class(null, l(), null, false, "Foo", null, l(), l(), l(), l()));
        assertEqualsFile("abstract class A extends B { int x; }",
            _class(null, l(), null, true, "A", "B", l(), l(_var(AccessPermission.PROTECTED, false, false, "int", "x", null)), l(), l()));

        assertEqualsFile("package p; import java.util.*; /**Mixed order*/class Jumble implements A,B,C { public double method() {return 0.0;}; private int member; protected Jumble() {return;} }",
            _class("p", l("java.util.*"), "Mixed order", false, "Jumble", null, l("A", "B", "C"),
                l(_var(AccessPermission.PRIVATE, false, false, "int", "member", null)),
                l(_constructor(null, AccessPermission.PROTECTED, l(), l(), l(_return()))),
                l(_method(null, AccessPermission.PUBLIC, false, false, "double", "method", l(), l(), l(_return("0.0"))))));
        
        // TODO test interfaces
        //TODO test non-Javadoc mid-class comments
    }
    
    @Test
    public void testRandomStatement()
    {
        for (int i = 0; i < 1000; i++)
        {
            roundTrip(many(() -> genStatement(4)), Parser.JavaContext.STATEMENT);
        }
    }

    @Test
    public void testRandomTopLevel()
    {
        for (int i = 0; i < 1000; i++)
        {
            roundTrip(l(genTopLevel()), Parser.JavaContext.TOP_LEVEL);
        }
    }
    
    private static CodeElement genTopLevel()
    {
        return genOneOf(
        () -> new ClassElement(null, null, rand(), genName(), rand() ? genType() : null, some(() -> genType()),
            somePrecede(() -> genComment(), () -> new VarElement(null, genAccess(), rand(), rand(), genType(), genName(), rand() ? genExpression() : null, true)),
            somePrecede(() -> genComment(), () -> {
                boolean hasSuperThis = rand();
                boolean isSuper = rand();
                return new ConstructorElement(null, genAccess(), some(() -> genParam()), some(() -> genThrowsType()), hasSuperThis ? new SuperThisFragment(isSuper ? SuperThis.SUPER : SuperThis.THIS) : null, hasSuperThis ? genSuperThisParams() : null, some(() -> genStatement(3)), rand() ? null : genJavadoc(), true);
            }),
            somePrecede(() -> genComment(), () -> genOneOf(
                () -> new NormalMethodElement(null, genAccess(), rand(), rand(), genType(), genName(), some(() -> genParam()), some(() -> genThrowsType()), some(() -> genStatement(3)), rand() ? null : genJavadoc(), true),
                () -> new MethodProtoElement(null, genType(), genName(), some(() -> genParam()), some(() -> genThrowsType()), rand() ? null : genJavadoc(), true)
            )),
            genJavadoc(), rand() ? null : genPackage(), some(() -> genImport()), true),
        () -> new InterfaceElement(null, null, genName(), some(() -> genType()),
            somePrecede(() -> genComment(), () -> new VarElement(null, genAccess(), rand(), rand(), genType(), genName(), rand() ? genExpression() : null, true)),
            somePrecede(() -> genComment(), 
                () -> new MethodProtoElement(null, genType(), genName(), some(() -> genParam()), some(() -> genThrowsType()), rand() ? null : genJavadoc(), true)
            ),
            genJavadoc(), rand() ? null : genPackage(), some(() -> genImport()), true)
        );
    }

    private static CommentElement genComment()
    {
        return new CommentElement(oneOf("Hello", "Bye", "Whatever", "///"));
    }

    private static ImportElement genImport()
    {
        return new ImportElement(oneOf("p.*", "hi.xx", "java.lang.String", "java.util.*"), null, true);
    }

    private static PackageFragment genPackage()
    {
        return new PackageFragment(oneOf("p", "hi.xx", "a0.b1.c2"));
    }

    private static SuperThisParamsExpressionFragment genSuperThisParams()
    {
        return some(() -> (ExpressionSlotFragment)genExpression()).stream().map(e -> new SuperThisParamsExpressionFragment(e.getContent(), e.getJavaCode())).reduce((a, b) -> new SuperThisParamsExpressionFragment(a.getContent() + " , " + b.getContent(), a.getJavaCode() + " , " + b.getJavaCode())).orElse(new SuperThisParamsExpressionFragment("", ""));
    }

    private static ThrowsTypeFragment genThrowsType()
    {
        return new ThrowsTypeFragment(genType());
    }

    private static ParamFragment genParam()
    {
        return new ParamFragment(genType(), genName());
    }

    private static AccessPermissionFragment genAccess()
    {
        return new AccessPermissionFragment(oneOf(AccessPermission.PRIVATE, AccessPermission.PROTECTED, AccessPermission.PUBLIC));
    }

    private static JavadocUnit genJavadoc()
    {
        return new JavadocUnit(oneOf("Hi", "One Two", "A B C."));
    }

    private static CodeElement genStatement(int maxDepth)
    {
        List<Supplier<CodeElement>> terminals = Arrays.asList(
            () -> new CommentElement("c" + rand(0, 9)),
            () -> new BreakElement(null, true),
            () -> new ReturnElement(null, genOptExpression(), true),
            () -> new ThrowElement(null, genExpression(), true),
            () -> new VarElement(null, null, false, rand(), genType(), genName(), rand() ? genExpression() : null, true),
                //Calls and assignments are always turned back into calls, so for round-tripping we start as calls:
            () -> genCallOrAssign()
        );
        List<Supplier<CodeElement>> all = new ArrayList<>(Arrays.asList(
            () -> new WhileElement(null, genExpression(), some(() -> genStatement(maxDepth - 1)), true),
            () -> {
                int elseIfs = rand(0, 2);
                boolean _else = rand(0, 1) == 1;
                return new IfElement(null, genExpression(), some(() -> genStatement(maxDepth - 1)),
                        replicate(elseIfs, () -> genExpression()),
                        replicate(elseIfs, () -> some(() -> genStatement(maxDepth - 1))),
                        _else ? null : some(() -> genStatement(maxDepth - 1)), true);
            },
            () -> {
                int catches = rand(0, 2);
                boolean _finally = rand(0, 1) == 1;
                return new TryElement(null, some(() -> genStatement(maxDepth - 1)),
                        replicate(catches, () -> genType()),
                        replicate(catches, () -> genName()),
                        replicate(catches, () -> some(() -> genStatement(maxDepth - 1))),
                        _finally ? null : some(() -> genStatement(maxDepth - 1)), true);
            },
            () -> new ForeachElement(null, genType(), genName(), genExpression(), some(() -> genStatement(maxDepth - 1)), true),
            () -> new SwitchElement(null, genExpression(), some(() -> genCase()), rand() ? null : some(() -> genStatement(maxDepth - 1)), true)
        ));
        all.addAll(terminals);
        if (maxDepth <= 1)
            return genOneOf(terminals.toArray(new Supplier[0]));
        else
            return genOneOf(all.toArray(new Supplier[0]));
    }

    private static CaseElement genCase()
    {
        return new CaseElement(null, genExpression(), some(() -> genStatement(2)), true);
    }

    private static boolean rand()
    {
        return rand(0, 1) == 0;
    }

    private static CodeElement genCallOrAssign()
    {
        return genOneOf(
                () -> {
                    return new AssignElement(null, filled(genName().getContent()), genExpression(), true);
                },
                () -> {
                    FilledExpressionSlotFragment call = genCall();
                    return new CallElement(null, new CallExpressionSlotFragment(call.getContent(), call.getJavaCode()), true);
                }
        );
    }

    private static <T> List<T> replicate(int count, Supplier<T> supplier)
    {
        ArrayList<T> r = new ArrayList<>();
        for (int i = 0; i < count; i++)
            r.add(supplier.get());
        return r;
    }

    private static OptionalExpressionSlotFragment genOptExpression()
    {
        return genOneOf(() -> null, () -> new OptionalExpressionSlotFragment(genExpression()));
    }

    private static FilledExpressionSlotFragment genExpression()
    {
        return genExpression(2);
    }
    
    private static FilledExpressionSlotFragment concat(FilledExpressionSlotFragment a, FilledExpressionSlotFragment b)
    {
        return new FilledExpressionSlotFragment(a.getContent() + " " + b.getContent(), a.getJavaCode() + " " + b.getJavaCode());
    }

    private static FilledExpressionSlotFragment genExpression(int maxDepth)
    {
        //Terminals:
        List<Supplier<FilledExpressionSlotFragment>> all = Arrays.asList(
            () -> filled("0"),
            () -> filled("\"hello\\n\""),
            () -> filled("\"c\" + 0"),
            () -> filled("\"\" . length ( )"),
            () -> filled("new Foo ( )"),
            () -> new FilledExpressionSlotFragment("a <: b", "a instanceof b"),
            () -> genCall()
        );
        if (maxDepth > 1)
        {
            // Non-terminals:
            return genOneOf(
                // Unary operator:
                () -> concat(filled(oneOf("!")), genExpression(maxDepth - 1)),
                // Cast:
                () -> {
                    TypeSlotFragment t = genType();
                    return concat(new FilledExpressionSlotFragment("( " + uniformSpacing(t.getContent(), false) + " )", "( " + uniformSpacing(t.getJavaCode(), false) + " )"), genExpression(maxDepth - 1));
                },
                // Binary operator:
                () -> concat(genExpression(maxDepth - 1), genOneOf(
                    () -> concat(filled("+"), genExpression(maxDepth - 1)),
                    () -> concat(filled("-"), genExpression(maxDepth - 1)),
                    () -> concat(filled("<<"), genExpression(maxDepth - 1)),
                    () -> {
                        TypeSlotFragment t = genType();
                        return new FilledExpressionSlotFragment("<: " + uniformSpacing(t.getContent(), false), "instanceof " + uniformSpacing(t.getJavaCode(), false));
                    })
                )
            );
        }
        return genOneOf(all.toArray(new Supplier[0]));
    }

    private static FilledExpressionSlotFragment genCall()
    {
        return filled(oneOf("foo . getX ( )", "x ( )", "foo ( ) [ getN ( 6 + 7 ) ]", "x ( )", "y ( )", "a ( ) . b ( )"));
    }

    private static TypeSlotFragment genType()
    {
        return type(oneOf("T", "int", "char", "double", "float", "int_", "double", "Double", "java.lang.String", "a.b", "a.b.c.d", "List<String>", "A.B<C.D>[]", "int[]", "double[][]", "List<String>[][]"));
    }

    private static NameDefSlotFragment genName()
    {
        return name(oneOf("x","y","i","z081","foo","bar","name_thing", "Class", "_package", "ABCD"));
    }

    private static <T> T oneOf(T... items)
    {
        return items[rand(0, items.length - 1)];
    }

    private static <T> T genOneOf(Supplier<T>... items)
    {
        return items[rand(0, items.length - 1)].get();
    }
    
    private static <T> List<T> collapseComments(List<T> items)
    {
        ArrayList<T> r = new ArrayList<>(items);
        int i = 1;
        while (i < r.size())
        {
            // We don't try two comment elements next to each other:
            if (r.get(i - 1) instanceof CommentElement && r.get(i) instanceof CommentElement)
                r.remove(i);
            else
                i += 1;
        }
        return r;
    }
    
    private static <T> List<T> some(Supplier<T> item)
    {
        return collapseComments(Stream.generate(item).limit(rand(0, 4)).collect(Collectors.toList()));
    }

    private static <T> List<T> somePrecede(Supplier<T> before, Supplier<T> item)
    {
        return collapseComments(Stream.generate(item).limit(rand(0, 4)).flatMap(x -> rand() ? Stream.of(x) : Stream.of(before.get(), x)).collect(Collectors.toList()));
    }

    private static <T> List<T> many(Supplier<T> item)
    {
        return collapseComments(Stream.generate(item).limit(rand(1, 5)).collect(Collectors.toList()));
    }
    
    private static int rand(int min, int max)
    {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
    
    private static void roundTrip(List<CodeElement> original, Parser.JavaContext context)
    {
        String java = collapseComments(original).stream().map(el -> el.toJavaSource().toTemporaryJavaCodeString()).collect(Collectors.joining("\n"));
        try
        {
            test(java, original.toArray(new CodeElement[0]), context);
        }
        catch (Exception e)
        {
            System.err.println("Error while round-tripping: " + original.stream().map(CodeElement::toXML).map(LocatableElement::toXML).collect(Collectors.joining()) + "\n\n" + java);
            throw e;
        }
    }

    private ClassElement _class(String pkg, List<String> imports, String javadoc, boolean _abstract, String name, String _extends, List<String> _implements, List<VarElement> fields, List<ConstructorElement> constructors, List<NormalMethodElement> methods)
    {
        return new ClassElement(null, null, _abstract, name(name), _extends == null ? null : type(_extends),
            _implements.stream().map(t -> type(t)).collect(Collectors.toList()),
            fields, constructors, methods, new JavadocUnit(javadoc), pkg == null ? null : new PackageFragment(pkg), imports.stream().map(i -> new ImportElement(i, null, true)).collect(Collectors.toList()), true);
    }

    private CommentElement _comment(String s)
    {
        return new CommentElement(s);
    }

    private VarElement _var(AccessPermission access, boolean _static, boolean _final, String type, String name, FilledExpressionSlotFragment init)
    {
        return new VarElement(null, access == null ? null : new AccessPermissionFragment(access), _static, _final, type(type), name(name), init, true);
    }
    
    private ParamFragment _param(String type, String name)
    {
        return new ParamFragment(type(type), name(name));
    }

    private NormalMethodElement _method(String comment, AccessPermission accessPermission, boolean _static, boolean _final, String returnType, String name, List<ParamFragment> params, List<String> _throws, List<CodeElement> body)
    {
        return new NormalMethodElement(null, new AccessPermissionFragment(accessPermission), _static, _final, type(returnType), name(name), params, _throws.stream().map(t -> new ThrowsTypeFragment(type(t))).collect(Collectors.toList()), body, new JavadocUnit(comment), true);
    }

    private ConstructorElement _constructor(String comment, AccessPermission accessPermission, List<ParamFragment> params, List<String> _throws, List<CodeElement> body)
    {
        return new ConstructorElement(null, new AccessPermissionFragment(accessPermission), params, _throws.stream().map(t -> new ThrowsTypeFragment(type(t))).collect(Collectors.toList()), null, null, body, new JavadocUnit(comment), true);
    }

    private CodeElement _constructorDelegate(String comment, AccessPermission accessPermission, List<ParamFragment> params, List<String> _throws, SuperThis superThis, String superThisArgs, List<CodeElement> body)
    {
        return new ConstructorElement(null, new AccessPermissionFragment(accessPermission), params, _throws.stream().map(t -> new ThrowsTypeFragment(type(t))).collect(Collectors.toList()), new SuperThisFragment(superThis), new SuperThisParamsExpressionFragment(superThisArgs, superThisArgs), body, new JavadocUnit(comment), true);
    }

    private CodeElement _constructorDelegate(String comment, AccessPermission accessPermission, List<ParamFragment> params, List<String> _throws, SuperThis superThis, SuperThisParamsExpressionFragment superThisArgs, List<CodeElement> body)
    {
        return new ConstructorElement(null, new AccessPermissionFragment(accessPermission), params, _throws.stream().map(t -> new ThrowsTypeFragment(type(t))).collect(Collectors.toList()), new SuperThisFragment(superThis), superThisArgs, body, new JavadocUnit(comment), true);
    }

    private static void assertExpression(String expectedStride, String original)
    {
        Assert.assertEquals(expectedStride, new Expression(original).toFilled().getContent());
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
        return new ReturnElement(null, null, true);
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
        return new IfElement(null, filled(expression), body, expressions.stream().map(JavaToStrideTest::filled).collect(Collectors.toList()), elseIfBodies, elseBody, true);
    }

    private IfElement _ifElseIf(FilledExpressionSlotFragment expression, List<CodeElement> body, List<FilledExpressionSlotFragment> expressions, List<List<CodeElement>> elseIfBodies, List<CodeElement> elseBody)
    {
        return new IfElement(null, expression, body, expressions, elseIfBodies, elseBody, true);
    }
    
    private static FilledExpressionSlotFragment filled(String e)
    {
        return new FilledExpressionSlotFragment(e, e);
    }
        
    // Short-hand for Arrays.asList
    private static <T> List<T> l(T... items)
    {
        return Arrays.asList(items);
    }

    private static void assertEqualsNoRound(String javaSource, CodeElement... expectedStride)
    {
        test(javaSource, expectedStride, Parser.JavaContext.STATEMENT);
    }

    private static void assertEquals(String javaSource, CodeElement... expectedStride)
    {
        test(javaSource, expectedStride, Parser.JavaContext.STATEMENT);
        roundTrip(Arrays.asList(expectedStride), Parser.JavaContext.STATEMENT);
    }

    private static void assertEqualsMember(String javaSource, CodeElement... expectedStride)
    {
        test(javaSource, expectedStride, Parser.JavaContext.CLASS_MEMBER);
        // Round tripping is awkward, e.g. constructors by themselves can't generate
        // Java because they need their name from their parent class
    }

    private static void assertEqualsFile(String javaSource, CodeElement... expectedStride)
    {
        test(javaSource, expectedStride, Parser.JavaContext.TOP_LEVEL);
    }
    
    private static String serialise(Element el)
    {
        try
        {
            return Utility.serialiseCodeToString(el);
        }
        catch (IOException e)
        {
            return null;
        }
    }

    private static void test(String javaSource, CodeElement[] expectedStride, Parser.JavaContext classMember)
    {
        List<CodeElement> result = Parser.javaToStride(javaSource, classMember);
        List<String> resultXML = result.stream().map(CodeElement::toXML).map(JavaToStrideTest::serialise).collect(Collectors.toList());
        List<String> expectedXML = Arrays.stream(expectedStride).map(CodeElement::toXML).map(JavaToStrideTest::serialise).collect(Collectors.toList());
        if (expectedXML.size() == 1 && resultXML.size() == 1)
            // Get better output if we compare strings:
            Assert.assertEquals("Checking XML", expectedXML.get(0), resultXML.get(0));
        else
            Assert.assertEquals("Checking XML", expectedXML, resultXML);
    }
}
