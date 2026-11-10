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

import java.util.ArrayList;
import java.util.List;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * The text a chosen completion inserts in place of the typed prefix.
 *
 * <p>Segments are inserted one after another as separate document edits, so each segment
 * is its own undo step (Java method completion has always inserted {@code name(}, the
 * parameters and {@code )} separately). Empty segments are dropped.</p>
 *
 * @param segments       the text to insert, in order
 * @param selectionStart offset (relative to the start of the insertion) of the range to
 *                       select afterwards, or -1 to leave the caret after the inserted text
 * @param selectionEnd   end offset (relative, exclusive) of the range to select; ignored if
 *                       {@code selectionStart} is -1
 */
@OnThread(Tag.Any)
public record CompletionInsertion(List<String> segments, int selectionStart, int selectionEnd)
{
    /**
     * Create an insertion; empty segments are dropped.
     *
     * @throws IllegalArgumentException if the selection lies outside the inserted text
     */
    public CompletionInsertion
    {
        segments = segments.stream().filter(s -> !s.isEmpty()).toList();
        int length = segments.stream().mapToInt(String::length).sum();
        if (selectionStart != -1 && (selectionStart < 0 || selectionEnd < selectionStart || selectionEnd > length)) {
            throw new IllegalArgumentException("Selection " + selectionStart + "-" + selectionEnd
                + " outside insertion of length " + length);
        }
    }

    /**
     * An insertion of plain text, leaving the caret after it.
     *
     * @param text the text to insert
     * @return the insertion
     */
    public static CompletionInsertion text(String text)
    {
        return new CompletionInsertion(List.of(text), -1, -1);
    }

    /**
     * A call insertion: {@code name(arg1, arg2)}, with the first argument selected so the
     * user can overtype it. With no arguments the caret is left after the closing parenthesis.
     *
     * @param name      the callable's name
     * @param arguments placeholder text for each argument, in order
     * @return the insertion
     */
    public static CompletionInsertion call(String name, List<String> arguments)
    {
        List<String> segments = new ArrayList<>();
        segments.add(name + "(");
        // Put all available params in, separated by ", "
        segments.add(String.join(", ", arguments));
        segments.add(")");
        if (arguments.isEmpty()) {
            return new CompletionInsertion(segments, -1, -1);
        }
        // Record position before we add first parameter, so that we can come back and select it:
        int firstArgStart = name.length() + 1;
        return new CompletionInsertion(segments, firstArgStart, firstArgStart + arguments.getFirst().length());
    }

    /**
     * A call whose last argument is a lambda written after the parentheses (Kotlin's trailing lambda
     * syntax): {@code name(arg1) { }}, or {@code name { }} without other arguments. The first argument is
     * selected; without one, the caret is left inside the braces.
     *
     * @param name      the callable's name
     * @param arguments placeholder text for the arguments before the lambda, in order
     * @return the insertion
     */
    public static CompletionInsertion trailingLambdaCall(String name, List<String> arguments)
    {
        if (arguments.isEmpty()) {
            String opening = name + " { ";
            return new CompletionInsertion(List.of(opening, "}"), opening.length(), opening.length());
        }
        int firstArgStart = name.length() + 1;
        return new CompletionInsertion(List.of(name + "(", String.join(", ", arguments), ") { ", "}"),
            firstArgStart, firstArgStart + arguments.getFirst().length());
    }

    /**
     * Get the complete inserted text.
     *
     * @return all segments joined together
     */
    public String fullText()
    {
        return String.join("", segments);
    }
}
