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
    public void testJavaInsertionUnchanged()
    {
        // A field inserts its name; a method inserts name(, the dummy parameters and ) as the three separate
        // undo steps Java completion has always had, with the first dummy parameter selected.
        JavaCompletionProvider java = new JavaCompletionProvider();
        assertEquals(CompletionInsertion.text("out"), java.getInsertion(new StubContent("out", null)));
        assertEquals(List.of("act(", ")"), java.getInsertion(new StubContent("act", List.of())).segments());

        List<AssistContent.ParamInfo> params = List.of(
            new AssistContent.ParamInfo("int", "distance", "distance", () -> ""),
            new AssistContent.ParamInfo("int", null, "int1", () -> ""));
        CompletionInsertion insertion = java.getInsertion(new StubContent("move", params));
        assertEquals(List.of("move(", "distance, int1", ")"), insertion.segments());
        assertEquals("move(distance, int1)", insertion.fullText());
        assertEquals("First dummy parameter should be selected", "move(".length(), insertion.selectionStart());
        assertEquals("First dummy parameter should be selected", "move(distance".length(), insertion.selectionEnd());
    }

    @Test
    public void testTrailingLambdaCall()
    {
        CompletionInsertion withArgs = CompletionInsertion.trailingLambdaCall("repeat", List.of("_times_"));
        assertEquals(List.of("repeat(", "_times_", ") { ", "}"), withArgs.segments());
        assertEquals("First argument should be selected", "repeat(".length(), withArgs.selectionStart());
        assertEquals("First argument should be selected", "repeat(_times_".length(), withArgs.selectionEnd());

        CompletionInsertion lambdaOnly = CompletionInsertion.trailingLambdaCall("forEach", List.of());
        assertEquals("forEach { }", lambdaOnly.fullText());
        assertEquals("Caret should be inside the braces", "forEach { ".length(), lambdaOnly.selectionStart());
        assertEquals("Nothing should be selected", lambdaOnly.selectionStart(), lambdaOnly.selectionEnd());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSelectionOutsideInsertionIsRejected()
    {
        new CompletionInsertion(List.of("ab"), 1, 3);
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
