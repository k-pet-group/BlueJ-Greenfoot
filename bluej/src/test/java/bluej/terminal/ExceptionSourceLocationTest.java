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
package bluej.terminal;

import java.util.regex.Matcher;

import org.junit.Test;

import static org.junit.Assert.*;

/** Tests the frame matcher used by the terminal, independently of window display. */
public class ExceptionSourceLocationTest
{
    /** Kotlin frames retain the source filename for classes, companions and file facades. */
    @Test
    public void testKotlinUsesSourceFilenameRatherThanJvmClassName()
    {
        for (String method : new String[] {"Crab.act", "demo.Crab$Companion.fail", "demo.UtilsKt.fail"}) {
            String file = method.contains("UtilsKt") ? "utils.kt" : "Crab.kt";
            assertFrame(method, file);
        }
    }

    /** Java source frames match, while unsupported locations and ordinary output do not. */
    @Test
    public void testJavaFramesAndNonSourceOutput()
    {
        assertFrame("demo.Crab$Helper.act", "Crab.java");
        for (String text : new String[] {"at demo.Crab.act(Unknown Source)",
            "at demo.Crab.act(Native Method)", "at demo.Crab.act(Crab.scala:12)", "ordinary output"}) {
            assertFalse(text, ExceptionSourceLocation.STACK_TRACE_PATTERN.matcher(text).find());
        }
    }

    private void assertFrame(String method, String file)
    {
        String text = "\tat " + method + "(" + file + ":12)\n";
        Matcher matcher = ExceptionSourceLocation.STACK_TRACE_PATTERN.matcher(text);
        assertTrue(text, matcher.find());
        assertEquals(method, matcher.group(1));
        assertEquals(file, matcher.group(2));
        assertEquals("12", matcher.group(3));
        assertEquals(method + "(" + file + ":12)", text.substring(matcher.start(1), matcher.end()));
        assertFalse(matcher.find());
    }
}
