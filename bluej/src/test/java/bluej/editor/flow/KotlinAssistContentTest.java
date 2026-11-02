/*
 This file is part of the BlueJ program.
 Copyright (C) 2026  Michael Kolling and John Rosenberg

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
package bluej.editor.flow;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import bluej.compiler.KotlinTestClasspath;
import bluej.debugger.gentype.MethodReflective;
import bluej.debugger.gentype.Reflective;
import bluej.editor.fixes.SuggestionList.SuggestionDetails;
import bluej.parser.AssistContent.Access;
import bluej.parser.AssistContent.CompletionKind;
import bluej.parser.AssistContent.ParamInfo;
import bluej.parser.ConstructorOrMethodReflective;
import bluej.parser.InitConfig;
import bluej.parser.kotlin.analysis.KotlinAnalysisRequest;
import bluej.parser.kotlin.analysis.KotlinAnalysisService;
import bluej.parser.kotlin.analysis.KotlinCompletionCandidate;
import bluej.parser.kotlin.analysis.KotlinCompletionCandidate.JavaMember;
import bluej.parser.kotlin.analysis.KotlinCompletionCandidate.Parameter;
import bluej.parser.kotlin.analysis.KotlinCompletionEngine;
import bluej.parser.kotlin.analysis.KotlinCompletionResult;
import bluej.pkgmgr.JavadocResolver;
import javafx.stage.Stage;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link KotlinAssistContent}: Javadoc lookup, parameters, documentation and insertion.
 */
public class KotlinAssistContentTest extends FXTest
{
    @Override
    public void start(Stage stage) throws Exception
    {
        super.start(stage);
        InitConfig.init();
    }

    @Test
    public void testJavaParameterNamesAndDocumentationFromJavadoc()
    {
        StubJavadocResolver resolver = new StubJavadocResolver(Map.of("indexOf",
            new Doc("Finds a substring.\n@param str the substring\n@param fromIndex where to start", List.of("str", "fromIndex"))));
        KotlinAssistContent item = single(javaMethod("indexOf", "java.lang.String", List.of("java.lang.String", "int"),
            List.of(param("p0", "String"), param("p1", "Int"))), resolver);

        List<ParamInfo> params = fx(item::getParams);
        assertEquals("str", params.get(0).getFormalName());
        assertEquals("_fromIndex_", params.get(1).getDummyName());
        assertEquals("the substring", params.get(0).getJavadocDescription());
        assertTrue(fx(item::getJavadoc).startsWith("Finds a substring."));
    }

    @Test
    public void testJavadocLookupMatchesArrayAndGenericParameters()
    {
        StubJavadocResolver resolver = new StubJavadocResolver(Map.of(
            "valueOf", new Doc("", List.of("data")),
            "add", new Doc("", List.of("e"))));
        List<KotlinAssistContent> items = fx(() -> KotlinAssistContent.create(List.of(
            javaMethod("valueOf", "java.lang.String", List.of("char[]"), List.of(param("p0", "CharArray"))),
            javaMethod("add", "java.util.List", List.of("java.lang.Object"), List.of(param("p0", "E")))),
            resolver, KotlinAssistContentTest::loadClass));
        assertEquals("data", fx(() -> items.getFirst().getParams().getFirst().getFormalName()));
        assertEquals("e", fx(() -> items.get(1).getParams().getFirst().getFormalName()));
    }

    @Test
    public void testEngineSignaturesMatchJavaReflection() throws Exception
    {
        // JDK classes, like the Greenfoot API, are compiled without parameter names: the engine sees p0, p1, ...
        Path projectDir = Files.createTempDirectory("kotlin-assist-test");
        KotlinAnalysisService service = new KotlinAnalysisService();
        try {
            String text = "fun f(c: Class<*>) { c. }";
            KotlinAnalysisRequest request = new KotlinAnalysisRequest(projectDir.toFile(),
                KotlinTestClasspath.getKotlinStdlib(), StandardCharsets.UTF_8, projectDir.resolve("F.kt").toFile(),
                text, Map.of());
            KotlinCompletionResult result = KotlinCompletionEngine.complete(service, request, text.indexOf("c. ") + 2)
                .get(60, TimeUnit.SECONDS);
            List<KotlinCompletionCandidate> methods = result.candidates().stream()
                .filter(c -> c.name().equals("isAssignableFrom") || c.name().equals("getMethod")).toList();
            assertEquals(2, methods.size());

            StubJavadocResolver resolver = new StubJavadocResolver(Map.of(
                "isAssignableFrom", new Doc("", List.of("cls")),
                "getMethod", new Doc("", List.of("name", "parameterTypes"))));
            List<KotlinAssistContent> items = fx(() -> KotlinAssistContent.create(methods, resolver,
                KotlinAssistContentTest::loadClass));
            for (KotlinAssistContent item : items) {
                assertTrue("Javadoc names should replace p0 for " + item.getName() + " (generic and vararg parameters)",
                    fx(item::getParams).stream().allMatch(p -> p.getFormalName() != null));
            }
        }
        finally {
            service.dispose();
            Files.deleteIfExists(projectDir);
        }
    }

    @Test
    public void testWithoutJavadocSyntheticNamesFallBackToTypePlaceholders()
    {
        KotlinAssistContent item = single(javaMethod("indexOf", "java.lang.String", List.of("java.lang.String", "int"),
            List.of(param("p0", "String"), param("p1", "Int"))), new StubJavadocResolver(Map.of()));
        List<ParamInfo> params = fx(item::getParams);
        assertNull("p0 is not a real name", params.get(0).getFormalName());
        assertEquals("_Int_", params.get(1).getDummyName());
        assertEquals(CompletionInsertion.call("indexOf", List.of("_String_", "_Int_")), fx(item::getInsertion));
    }

    @Test
    public void testKotlinDeclarationsKeepTheirNamesAndUseKDoc()
    {
        KotlinCompletionCandidate kotlin = new KotlinCompletionCandidate("", "shift", CompletionKind.METHOD, "Unit",
            List.of(param("p0", "Int")), "Rock", Access.PUBLIC, "/** Moves by `p0`. */", null);
        KotlinAssistContent item = single(kotlin, new StubJavadocResolver(Map.of()));
        assertEquals("Kotlin parameters named p0 are real names", "p0", fx(item::getParams).getFirst().getFormalName());
        assertEquals("Moves by <code>p0</code>.", fx(item::getJavadoc).trim());
    }

    @Test
    public void testInsertionUsesRequiredArgumentsAndTrailingLambda()
    {
        assertEquals("Properties insert their name", CompletionInsertion.text("this.speed"),
            insertion(new KotlinCompletionCandidate("this.", "speed", CompletionKind.FIELD, "Int", null, "Wombat",
                Access.PUBLIC, null, null)));
        assertEquals("Optional parameters are left out; a required lambda last becomes a trailing lambda",
            CompletionInsertion.trailingLambdaCall("roll", List.of("_fast_")), insertion(roll()));
        assertEquals("Only a lambda: name { }", CompletionInsertion.trailingLambdaCall("forEach", List.of()),
            insertion(kotlinFunction("forEach", new Parameter("action", "(T) -> Unit", false, false, true))));
        assertEquals("An optional lambda is left out, not made trailing", CompletionInsertion.call("tick", List.of("_x_")),
            insertion(kotlinFunction("tick", param("x", "Int"), new Parameter("block", "() -> Unit", true, false, true))));
        assertEquals("Varargs are optional", CompletionInsertion.call("listOf", List.of()),
            insertion(kotlinFunction("listOf", new Parameter("elements", "T", true, true, false))));
        assertEquals("Prefixes are inserted", CompletionInsertion.call("Greenfoot.getRandomNumber", List.of("_limit_")),
            insertion(new KotlinCompletionCandidate("Greenfoot.", "getRandomNumber", CompletionKind.METHOD, "Int",
                List.of(param("limit", "Int")), "greenfoot.Greenfoot", Access.PUBLIC, null, null)));
    }

    @Test
    public void testPopupRowAndDocumentationHeader()
    {
        KotlinAssistContent item = single(roll(), new StubJavadocResolver(Map.of()));
        SuggestionDetails details = fx(item::getSuggestionDetails);
        assertEquals("roll", details.choice);
        assertEquals("Kotlin syntax after the name; defaults are only marked in the documentation",
            "(steps: Int, fast: Boolean, action: () -> Unit): Int", details.suffix);
        assertNull("No Java-style type column", details.type);
        String html = fx(item::getDocHTML);
        assertTrue(html, html.contains("<b>roll</b>(steps: Int = ..., fast: Boolean, action: () -&gt; Unit): Int"));
        assertTrue(html, html.contains("Rolls the rock"));
    }

    @Test
    public void testPopupRowForPropertiesVarargsAndUnnamedJavaParameters()
    {
        KotlinCompletionCandidate world = new KotlinCompletionCandidate("", "world", CompletionKind.FIELD, "World", null,
            "greenfoot.Actor", Access.PUBLIC, null, null);
        assertEquals(": World", fx(single(world, new StubJavadocResolver(Map.of()))::getSuggestionDetails).suffix);

        KotlinCompletionCandidate listOf = kotlinFunction("listOf", new Parameter("elements", "T", true, true, false));
        assertEquals("(vararg elements: T): Unit", fx(single(listOf, new StubJavadocResolver(Map.of()))::getSuggestionDetails).suffix);

        KotlinAssistContent indexOf = single(javaMethod("indexOf", "java.lang.String", List.of("java.lang.String", "int"),
            List.of(param("p0", "String"), param("p1", "Int"))), new StubJavadocResolver(Map.of()));
        assertEquals("Without Javadoc names, just the types", "(String, Int): Int", fx(indexOf::getSuggestionDetails).suffix);
    }

    private static KotlinCompletionCandidate roll()
    {
        return new KotlinCompletionCandidate("", "roll", CompletionKind.METHOD, "Int",
            List.of(new Parameter("steps", "Int", true, false, false), param("fast", "Boolean"),
                new Parameter("action", "() -> Unit", false, false, true)),
            "Rock", Access.PUBLIC, "/** Rolls the rock. */", null);
    }

    private static KotlinCompletionCandidate kotlinFunction(String name, Parameter... parameters)
    {
        return new KotlinCompletionCandidate("", name, CompletionKind.METHOD, "Unit", List.of(parameters), null,
            Access.PUBLIC, null, null);
    }

    private static KotlinCompletionCandidate javaMethod(String name, String className, List<String> jvmTypes,
                                                        List<Parameter> parameters)
    {
        return new KotlinCompletionCandidate("", name, CompletionKind.METHOD, "Int", parameters, className,
            Access.PUBLIC, null, new JavaMember(className, name, jvmTypes));
    }

    private static Parameter param(String name, String type)
    {
        return new Parameter(name, type, false, false, false);
    }

    private CompletionInsertion insertion(KotlinCompletionCandidate candidate)
    {
        return fx(single(candidate, new StubJavadocResolver(Map.of()))::getInsertion);
    }

    private KotlinAssistContent single(KotlinCompletionCandidate candidate, JavadocResolver resolver)
    {
        return fx(() -> KotlinAssistContent.create(List.of(candidate), resolver, KotlinAssistContentTest::loadClass).getFirst());
    }

    private static Class<?> loadClass(String name)
    {
        try {
            return Class.forName(name);
        }
        catch (ClassNotFoundException e) {
            return null;
        }
    }

    // Javadoc and parameter names for a method, as a real resolver would find them.
    private record Doc(String javadoc, List<String> paramNames)
    {
    }

    // Supplies Javadoc by method name, so tests need no documentation sources.
    private record StubJavadocResolver(Map<String, Doc> docs) implements JavadocResolver
    {
        @Override
        public void getJavadoc(Reflective declType, Collection<? extends ConstructorOrMethodReflective> methods)
        {
            for (ConstructorOrMethodReflective method : methods) {
                Doc doc = docs.get(((MethodReflective) method).getName());
                if (doc != null) {
                    method.setJavaDoc(doc.javadoc());
                    method.setParamNames(new ArrayList<>(doc.paramNames()));
                }
            }
        }

        @Override
        public String getJavadoc(String moduleName, String typeName)
        {
            return null;
        }

        @Override
        public boolean getJavadocAsync(ConstructorOrMethodReflective method, AsyncCallback callback,
                                       Executor executor)
        {
            return true;
        }
    }
}
