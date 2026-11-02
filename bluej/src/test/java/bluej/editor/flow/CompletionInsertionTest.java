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

import java.util.List;

import bluej.parser.AssistContent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link CompletionInsertion} and the Java insertion rules in {@link JavaCompletionProvider}.
 */
public class CompletionInsertionTest
{
    @Test
    public void testPlainTextLeavesCaretAfterText()
    {
        CompletionInsertion insertion = CompletionInsertion.text("count");
        assertEquals(List.of("count"), insertion.segments());
        assertEquals("Plain text should not select anything", -1, insertion.selectionStart());
        assertEquals("count", insertion.fullText());
    }

    @Test
    public void testCallSelectsFirstArgument()
    {
        CompletionInsertion insertion = CompletionInsertion.call("setLocation", List.of("x", "y"));
        assertEquals(List.of("setLocation(", "x, y", ")"), insertion.segments());
        assertEquals("setLocation(x, y)", insertion.fullText());
        assertEquals("First argument should be selected", "setLocation(".length(), insertion.selectionStart());
        assertEquals("First argument should be selected", "setLocation(x".length(), insertion.selectionEnd());
    }

    @Test
    public void testCallWithoutArgumentsDropsEmptySegmentAndLeavesCaretAfterParen()
    {
        CompletionInsertion insertion = CompletionInsertion.call("act", List.of());
        assertEquals("Empty parameter segment should be dropped", List.of("act(", ")"), insertion.segments());
        assertEquals("Caret should stay after the closing parenthesis", -1, insertion.selectionStart());
    }

    @Test
    public void testTrailingLambdaCallSelectsFirstArgument()
    {
        CompletionInsertion insertion = CompletionInsertion.trailingLambdaCall("repeat", List.of("_times_"));
        assertEquals(List.of("repeat(", "_times_", ") { ", "}"), insertion.segments());
        assertEquals("First argument should be selected", "repeat(".length(), insertion.selectionStart());
        assertEquals("First argument should be selected", "repeat(_times_".length(), insertion.selectionEnd());
    }

    @Test
    public void testTrailingLambdaOnlyPutsCaretInsideBraces()
    {
        CompletionInsertion insertion = CompletionInsertion.trailingLambdaCall("forEach", List.of());
        assertEquals("forEach { }", insertion.fullText());
        assertEquals("Caret should be inside the braces", "forEach { ".length(), insertion.selectionStart());
        assertEquals("Nothing should be selected", insertion.selectionStart(), insertion.selectionEnd());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSelectionOutsideInsertionIsRejected()
    {
        new CompletionInsertion(List.of("ab"), 1, 3);
    }

    @Test
    public void testJavaFieldInsertsName()
    {
        CompletionInsertion insertion = new JavaCompletionProvider().getInsertion(new StubContent("out", null));
        assertEquals(CompletionInsertion.text("out"), insertion);
    }

    @Test
    public void testJavaMethodInsertsDummyParameterNames()
    {
        List<AssistContent.ParamInfo> params = List.of(
            new AssistContent.ParamInfo("int", "distance", "distance", () -> ""),
            new AssistContent.ParamInfo("int", null, "int1", () -> ""));
        CompletionInsertion insertion = new JavaCompletionProvider().getInsertion(new StubContent("move", params));
        assertEquals("Java inserts name(, parameters and ) as separate undo steps",
            List.of("move(", "distance, int1", ")"), insertion.segments());
        assertEquals("First dummy parameter should be selected", "move(".length(), insertion.selectionStart());
        assertEquals("First dummy parameter should be selected", "move(distance".length(), insertion.selectionEnd());
    }

    /** Minimal completion candidate: only the name and parameters matter for insertion. */
    private static class StubContent extends AssistContent
    {
        private final String name;
        private final List<ParamInfo> params;

        StubContent(String name, List<ParamInfo> params)
        {
            this.name = name;
            this.params = params;
        }

        @Override
        public String getName()
        {
            return name;
        }

        @Override
        public List<ParamInfo> getParams()
        {
            return params;
        }

        @Override
        public String getType()
        {
            return null;
        }

        @Override
        public Access getAccessPermission()
        {
            return Access.PUBLIC;
        }

        @Override
        public String getDeclaringClass()
        {
            return null;
        }

        @Override
        public CompletionKind getKind()
        {
            return params == null ? CompletionKind.FIELD : CompletionKind.METHOD;
        }

        @Override
        public String getJavadoc()
        {
            return null;
        }
    }
}
