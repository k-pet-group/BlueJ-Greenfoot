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

import java.util.Arrays;

import bluej.parser.InitConfig;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Tests for the token-driven Kotlin auto-indenter.
 */
public class KotlinIndentTest
{
    @BeforeClass
    public static void init()
    {
        InitConfig.init();
    }

    /**
     * Apply the Kotlin indenter to {@code src} and assert the result equals
     * {@code expected}. Also asserts the {@code isPerfect} flag is true iff
     * the input was already correct (idempotency contract).
     */
    private void runTest(String expected, String src)
    {
        HoleDocument doc = new HoleDocument();
        doc.replaceText(0, 0, src);
        FlowIndent.AutoIndentInformation info = KotlinIndent.calculateIndentsAndApply(
                null, doc, null, 0, doc.getLength(), 0);
        String actual = doc.getContent(0, doc.getLength()).toString();
        assertEquals(expected, actual);
        assertEquals(expected.equals(src), info.isPerfect());
    }

    @Test
    public void testComputeIndentsSimpleClass()
    {
        String src = ""
                + "class Foo {\n"
                + "    fun bar() {\n"
                + "        return\n"
                + "    }\n"
                + "}\n";
        int[] got = KotlinIndent.computeIndents(src);
        // Six lines after splitting on '\n' (the trailing newline yields an empty line).
        // We assert on the first five — the leading body lines.
        int[] expectedHead = {0, 4, 8, 4, 0};
        assertArrayEquals(expectedHead, Arrays.copyOf(got, expectedHead.length));
    }

    @Test
    public void testApplyCardClass()
    {
        String correct = ""
                + "class Card(\n"
                + "    val suit: Suit,\n"
                + "    val rank: Rank\n"
                + ") {\n"
                + "    fun describe(): String {\n"
                + "        return \"$rank of $suit\"\n"
                + "    }\n"
                + "}\n";
        String mangled = ""
                + "class Card(\n"
                + "val suit: Suit,\n"
                + "        val rank: Rank\n"
                + "    ) {\n"
                + "fun describe(): String {\n"
                + "    return \"$rank of $suit\"\n"
                + "        }\n"
                + "        }\n";
        runTest(correct, mangled);
        // Idempotency: already-perfect input untouched and perfect == true.
        runTest(correct, correct);
    }

    /**
     * Property accessors {@code get()} / {@code set()}: the accessor
     * declaration line sits one indent step in from the property (Kotlin
     * continuation-indent convention), and the accessor body indents one
     * further step from there. The token-driven indenter detects this case
     * by recognising the soft keywords {@code get} / {@code set} as the
     * first token on a line inside a class body.
     */
    @Test
    public void testPropertyAccessors()
    {
        String correct = ""
                + "class Box {\n"
                + "    var size: Int = 0\n"
                + "        get() {\n"
                + "            return field\n"
                + "        }\n"
                + "        set(value) {\n"
                + "            field = value\n"
                + "        }\n"
                + "}\n";
        runTest(correct, correct);
    }

    /**
     * Real-world case: a property with a {@code get()} accessor whose body
     * contains a multi-arm {@code when} expression. Accessor line at +1
     * continuation, body at +2, {@code when} entries at +3 of the property.
     */
    @Test
    public void testPropertyAccessorWithWhenExpression()
    {
        String correct = ""
                + "class Card(val rang: String) {\n"
                + "    val wert: Int\n"
                + "        get() {\n"
                + "            return when (rang) {\n"
                + "                \"B\" -> 11\n"
                + "                \"D\" -> 12\n"
                + "                \"K\" -> 13\n"
                + "                \"A\" -> 14\n"
                + "                else -> rang.toInt()\n"
                + "            }\n"
                + "        }\n"
                + "}\n";
        runTest(correct, correct);
    }

    @Test
    public void testWhenExpression()
    {
        String correct = ""
                + "fun describe(x: Int): String {\n"
                + "    return when (x) {\n"
                + "        1 -> \"one\"\n"
                + "        2 -> \"two\"\n"
                + "        else -> {\n"
                + "            val s = x.toString()\n"
                + "            s\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
        String mangled = ""
                + "fun describe(x: Int): String {\n"
                + "return when (x) {\n"
                + "1 -> \"one\"\n"
                + "        2 -> \"two\"\n"
                + "    else -> {\n"
                + "val s = x.toString()\n"
                + "                s\n"
                + "}\n"
                + "}\n"
                + "}\n";
        runTest(correct, mangled);
        runTest(correct, correct);
    }

    @Test
    public void testTryCatchFinally()
    {
        String correct = ""
                + "fun safe() {\n"
                + "    try {\n"
                + "        risky()\n"
                + "    }\n"
                + "    catch (e: Exception) {\n"
                + "        handle(e)\n"
                + "    }\n"
                + "    finally {\n"
                + "        cleanup()\n"
                + "    }\n"
                + "}\n";
        runTest(correct, correct);
    }

    @Test
    public void testLambdaBody()
    {
        String correct = ""
                + "fun greet(names: List<String>) {\n"
                + "    names.map {\n"
                + "        it.uppercase()\n"
                + "    }\n"
                + "}\n";
        String mangled = ""
                + "fun greet(names: List<String>) {\n"
                + "names.map {\n"
                + "        it.uppercase()\n"
                + "}\n"
                + "}\n";
        runTest(correct, mangled);
        runTest(correct, correct);
    }

    @Test
    public void testAnonymousObject()
    {
        String correct = ""
                + "fun listener(): Runnable {\n"
                + "    return object : Runnable {\n"
                + "        override fun run() {\n"
                + "            println(\"go\")\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
        runTest(correct, correct);
    }

    @Test
    public void testSecondaryConstructor()
    {
        String correct = ""
                + "class Point(val x: Int, val y: Int) {\n"
                + "    constructor(x: Int) : this(x, 0) {\n"
                + "        println(\"y defaulted to 0\")\n"
                + "    }\n"
                + "}\n";
        runTest(correct, correct);
    }

    @Test
    public void testMultiLineFunctionSignature()
    {
        String correct = ""
                + "fun build(\n"
                + "    name: String,\n"
                + "    age: Int\n"
                + "): Person {\n"
                + "    return Person(name, age)\n"
                + "}\n";
        String mangled = ""
                + "fun build(\n"
                + "name: String,\n"
                + "        age: Int\n"
                + "    ): Person {\n"
                + "return Person(name, age)\n"
                + "}\n";
        runTest(correct, mangled);
        runTest(correct, correct);
    }

    /**
     * Triple-quoted string body lines must be left untouched even if their
     * existing indent looks "wrong" — re-indenting would change the string's
     * value.
     */
    @Test
    public void testTripleQuotedStringLeftUntouched()
    {
        // The interior of the """ ... """ has its own deliberate layout that
        // must survive auto-indent. Surrounding code can still be tidied.
        String correct = ""
                + "fun query(): String {\n"
                + "    return \"\"\"\n"
                + "SELECT *\n"
                + "  FROM tbl\n"
                + "\"\"\".trimIndent()\n"
                + "}\n";
        // Same string interior, but the surrounding `return` line is mis-indented.
        String mangled = ""
                + "fun query(): String {\n"
                + "return \"\"\"\n"
                + "SELECT *\n"
                + "  FROM tbl\n"
                + "\"\"\".trimIndent()\n"
                + "}\n";
        runTest(correct, mangled);
        runTest(correct, correct);
    }
}
