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

    /**
     * Asserts that the given names are/aren't available for autocomplete
     * at position "A" in the source, i.e. where
     * / * A * / (without spaces) occurs in the source.
     * @param namesShouldBeAvailable A list of names which should be available.
     * @param namesShouldNotBeAvailable A list of names which should not be available
     * @param javaSrc The Java source code                        
     */
    private void assertNamesAtA(List<String> namesShouldBeAvailable, List<String> namesShouldNotBeAvailable, String javaSrc)
    {
        Parsed p = parse(javaSrc, resolver);
        resolver.addCompilationUnit("", p.node());

        int pos = p.positionStart("A");
        AssistContent[] results = ParseUtils.getPossibleCompletions(p.node().getExpressionType(pos, p.doc()), dummyJavadocResolver, null, p.node().getContainingMethodOrClassNode(pos));
        List<String> names = Arrays.stream(results).map(AssistContent::getName).collect(Collectors.toList());

        MatcherAssert.assertThat(names, Matchers.hasItems(namesShouldBeAvailable.toArray(String[]::new)));
        MatcherAssert.assertThat(names, Matchers.not(Matchers.hasItems(namesShouldNotBeAvailable.toArray(String[]::new))));
    }

    @Test
    public void testFieldsInInitialiser()
    {
        assertNamesAtA(List.of("field1", "field2"), List.of("field3"), """
                class Foo
                {
                    int field1, field2;
                    {
                        /*A*/
                    }
                }
                """);
    }
}
