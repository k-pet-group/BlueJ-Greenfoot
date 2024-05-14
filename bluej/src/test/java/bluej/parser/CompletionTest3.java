/*
 This file is part of the BlueJ program. 
 Copyright (C) 2024  Michael Kolling and John Rosenberg
 
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

import bluej.JavaFXThreadingRule;
import bluej.debugger.gentype.Reflective;
import bluej.parser.ParseUtility.StartEnd;
import bluej.parser.entity.ClassLoaderResolver;
import bluej.parser.nodes.MethodNode;
import bluej.pkgmgr.JavadocResolver;
import bluej.utility.Debug;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static bluej.parser.ParseUtility.Parsed;
import static bluej.parser.ParseUtility.parse;
import static org.junit.Assert.*;

/**
 * Test for code completion, especially around local variables.
 */
public class CompletionTest3
{
    @Rule
    public JavaFXThreadingRule javafxRule = new JavaFXThreadingRule();

    @BeforeClass
    public static void initConfig()
    {
        InitConfig.init();
    }

    private JavadocResolver dummyJavadocResolver = new JavadocResolver()
    {
        @Override
        public void getJavadoc(Reflective declType, Collection<? extends ConstructorOrMethodReflective> methods)
        {
        }

        @Override
        public String getJavadoc(String moduleName, String typeName)
        {
            return "";
        }

        @Override
        public boolean getJavadocAsync(ConstructorOrMethodReflective method, AsyncCallback callback, Executor executor)
        {
            return false;
        }
    };

    private TestEntityResolver resolver;

    @Before
    public void setUp() throws Exception
    {
        Debug.setDebugStream(new OutputStreamWriter(System.out));
        resolver = new TestEntityResolver(new ClassLoaderResolver(this.getClass().getClassLoader()));
    }
    
    record AC(String type, String name)
    {
        // Allow constructing from a space-separated String, or constructing from an AssistContent
        public AC(String typeSpaceName)
        {
            this(typeSpaceName.split(" ")[0], typeSpaceName.split(" ")[1]);
        }
        public AC(AssistContent ac)
        {
            this(ac.getType(), ac.getName());
        }
    }

    /**
     * Asserts that the given names are/aren't available for autocomplete
     * at position "A" in the source, i.e. where
     * / * A * / (without spaces) occurs in the source.
     * @param typesAndNamesShouldBeAvailable A list of space-separated types and names (e.g. "int x") which should be available.
     * @param namesShouldNotBeAvailable A list of names which should not be available
     * @param javaSrc The Java source code                        
     */
    private void assertNamesAtA(List<String> typesAndNamesShouldBeAvailable, List<String> namesShouldNotBeAvailable, String javaSrc)
    {
        AssistContent[] results = getNamesAtA(javaSrc);
        List<AC> acs = Arrays.stream(results).map(AC::new).collect(Collectors.toList());
        List<String> namesOnly = Arrays.stream(results).map(AssistContent::getName).collect(Collectors.toList());

        MatcherAssert.assertThat(acs, Matchers.hasItems(typesAndNamesShouldBeAvailable.stream().map(AC::new).toArray(AC[]::new)));
        if (!namesShouldNotBeAvailable.isEmpty())
        {
            MatcherAssert.assertThat(namesOnly, Matchers.not(Matchers.hasItems(namesShouldNotBeAvailable.toArray(String[]::new))));
        }
    }

    private AssistContent[] getNamesAtA(String javaSrc)
    {
        Parsed p = parse(javaSrc, resolver);
        resolver.addCompilationUnit("", p.node());

        int pos = p.positionStart("A");
        AssistContent[] results = ParseUtils.getPossibleCompletions(p.node().getExpressionType(pos, p.doc()), dummyJavadocResolver, null, p.node().getContainingMethodOrClassNode(pos) instanceof MethodNode m ? m : null, pos);
        return results;
    }

    @Test
    public void testFieldsInInitialiser()
    {
        assertNamesAtA(List.of("int field1", "int field2"), List.of("field3"), """
                class Foo
                {
                    int field1, field2;
                    {
                        /*A*/
                    }
                }
                """);
    }

    @Test
    public void testLocalVarsInConstructor()
    {
        // Note: List<String> is not imported, but should still pass through to code completion:
        assertNamesAtA(List.of("int var1", "int var2", "String var3", "List var4"), List.of(), """
                class Foo
                {
                    Foo()
                    {
                        int var1, var2;
                        String var3;
                        List<String> var4;
                        /*A*/
                    }
                }
                """);
    }

    @Test
    public void testParamsInConstructor()
    {
        assertNamesAtA(List.of("int var1", "int var2", "String var3", "List var4", "List param1", "T param2", "int param3"), List.of(), """
                class Foo
                {
                    <T> Foo(List<T> param1, T param2, int param3)
                    {
                        int var1, var2;
                        String var3;
                        List<String> var4;
                        /*A*/
                    }
                }
                """);
    }

    @Test
    public void testLocalVarsInMethod()
    {
        assertNamesAtA(List.of("int var1", "int var2", "String var3", "List var4", "var var5"), List.of(), """
                class Foo
                {
                    void foo()
                    {
                        int var1, var2;
                        String var3;
                        List<String> var4;
                        var var5;
                        /*A*/
                    }
                }
                """);
    }

    @Test
    public void testParamsInMethod()
    {
        assertNamesAtA(List.of("int var1", "int var2", "String var3", "List var4", "List param1", "T param2", "int param3"), List.of(), """
                class Foo
                {
                    <T> void foo(List<T> param1, T param2, int param3)
                    {
                        int var1, var2;
                        String var3;
                        List<String> var4;
                        /*A*/
                    }
                }
                """);
    }

    @Test
    public void testArrayTypes()
    {
        assertNamesAtA(List.of("int[] arr1", "int[][] arr2", "List param1"), List.of(), """
                class Foo
                {
                    <T> void foo(List<T> param1)
                    {
                        int[] arr1;
                        int[][] arr2;
                        /*A*/
                    }
                }
                """);
    }

    @Test
    public void testLoopVars()
    {
        assertNamesAtA(List.of("int i", "String s"), List.of(), """
                class Foo
                {
                    void foo()
                    {
                        for (int i = 0;i < 10; i++)
                        {
                            for (String s : myList)
                            {
                                /*A*/
                            }
                        }
                    }
                }
                """);
    }

    @Test
    public void testInstanceofVar()
    {
        assertNamesAtA(List.of("String s"), List.of(), """
                class Foo
                {
                    void foo()
                    {
                        if ("hi" instanceof String s)
                        {
                            /*A*/
                        }
                    }
                }
                """);
    }

    @Test
    public void testSwitchInstanceofVar()
    {
        assertNamesAtA(List.of("String s"), List.of(), """
                class Foo
                {
                    void foo()
                    {
                        switch ("hi") {
                            case String s ->
                            {
                                /*A*/
                            }
                        }
                    }
                }
                """);
    }

    @Test
    public void testSwitchInstanceofVar2()
    {
        assertNamesAtA(List.of(), List.of("s"), """
                class Foo
                {
                    void foo()
                    {
                        switch ("hi") {
                            case String s ->
                            {
                            }
                            default -> { /*A*/ }
                        }
                    }
                }
                """);
    }

    @Test
    public void testSwitchInstanceofVar3()
    {
        assertNamesAtA(List.of(), List.of("s"), """
                class Foo
                {
                    void foo()
                    {
                        switch ("hi") {
                            case String s ->
                            {
                            }
                        }
                        /*A*/
                    }
                }
                """);
    }

    @Test
    public void testSwitchInstanceofVar5()
    {
        assertNamesAtA(List.of("Integer i"), List.of("s"), """
                class Foo
                {
                    void foo()
                    {
                        switch ("hi") {
                            case String s ->
                            {
                            }
                            case Integer i -> { /*A*/ }
                            default -> {}
                        }
                    }
                }
                """);
    }

    @Test
    public void testSwitchInstanceofVar4()
    {
        assertNamesAtA(List.of("String s"), List.of(), """
                class Foo
                {
                    void foo()
                    {
                        switch ("hi") {
                            case String s when /*A*/ ->
                            {
                            }
                        }
                    }
                }
                """);
    }

    @Test
    public void testNoLocalsOnThisDot()
    {
        assertNamesAtA(List.of("int field1", "int field2", "Object field3"), List.of("param1", "var1", "var2", "var3"), """
                class Foo
                {
                    int field1, field2;
                    Object field3;
                    
                    void foo(int param1)
                    {
                        int var1, var2;
                        String var3;
                        this./*A*/
                    }
                }
                """);
    }

    @Test
    public void testVarRelativePosition()
    {
        assertNamesAtA(List.of("int var1", "int var2", "String var3"), List.of("var4"), """
                class Foo
                {
                    void foo()
                    {
                        int var1, var2;
                        String var3;
                        /*A*/
                        List<String> var4;
                    }
                }
                """);
    }

    @Test
    public void testVarRelativePositionAndNested()
    {
        assertNamesAtA(List.of("int var1", "int var2", "String var3"), List.of("var2b", "var4"), """
                class Foo
                {
                    void foo()
                    {
                        int var1, var2;
                        if (true)
                        {
                            int var2b;
                        }
                        if (true)
                        {
                            String var3;
                            /*A*/
                            List<String> var4;
                        }
                    }
                }
                """);
    }

    @Test
    public void testOverlappingFieldsAndLocals()
    {
        AssistContent[] results = getNamesAtA("""
            class Foo
            {
                int x;
                String y;
                Object a;
                
                public Foo(double x, int y, int z)
                {
                    this.x = (int)x;
                    this.y = Integer.toString(y);
                    /*A*/
                }
            """);
        List<AC> acs = Arrays.stream(results).map(AC::new).collect(Collectors.toList());
        // We check what is there.  It should show two lots of x and y,
        // but the fields should be shown as "this.x/y" since that's the
        // only way to access them here:

        // Hamcrest doesn't have a good way for checking exactly one, so we
        // do that first, then check the types for everything after:
        MatcherAssert.assertThat(acs.stream().filter(ac ->ac.name.equals("x")).collect(Collectors.toList()), Matchers.hasSize(1));
        MatcherAssert.assertThat(acs.stream().filter(ac ->ac.name.equals("y")).collect(Collectors.toList()), Matchers.hasSize(1));
        MatcherAssert.assertThat(acs.stream().filter(ac ->ac.name.equals("z")).collect(Collectors.toList()), Matchers.hasSize(1));
        MatcherAssert.assertThat(acs.stream().filter(ac ->ac.name.equals("a")).collect(Collectors.toList()), Matchers.hasSize(1));
        MatcherAssert.assertThat(acs.stream().filter(ac ->ac.name.equals("this.x")).collect(Collectors.toList()), Matchers.hasSize(1));
        MatcherAssert.assertThat(acs.stream().filter(ac ->ac.name.equals("this.y")).collect(Collectors.toList()), Matchers.hasSize(1));
        MatcherAssert.assertThat(acs, Matchers.hasItems(
            new AC("int this.x"),
            new AC("String this.y"),
            new AC("Object a"),
            new AC("double x"),
            new AC("int y"),
            new AC("int z")
        ));
    }

    @Test
    public void testNoIncorrectVars()
    {
        // Saw a behaviour where with a partial expression, we were considering this a variable declaration in progress
        // and showing a variable that did not really exist (in this case, "myObj" with type "x"):
        assertNamesAtA(List.of("int var1", "int var2", "String var3"), List.of( "myObj"), """
                class Foo
                {
                    <T> void foo(List<T> param1, T param2, int param3)
                    {
                        int var1, var2;
                        String var3;
                        x/*A*/
                        myObj.toString();
                    }
                }
                """);
    }
}
