/*
 This file is part of the Greenfoot program.
 Copyright (C) 2026  Poul Henriksen and Michael Kolling

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
package greenfoot.record;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import bluej.debugger.gentype.JavaPrimitiveType;
import bluej.debugger.gentype.JavaType;
import greenfoot.Actor;
import greenfoot.World;
import greenfoot.record.RecordedAction.Arguments;

import org.junit.Test;

import static org.junit.Assert.*;

import threadchecker.OnThread;
import threadchecker.Tag;

/** Focused checks of recorded statement rendering and argument snapshots, without an editor or VM. */
@OnThread(Tag.FXPlatform)
public class RecordedActionTest
{
    /**
     * Legacy Java/Stride formatting retains trimming, array initializers and varargs element types.
     *
     * @throws NoSuchMethodException if a fixture or Greenfoot method cannot be found
     */
    @Test
    public void testLegacyJavaOutput() throws NoSuchMethodException
    {
        JavaType ints = JavaPrimitiveType.getInt().getArray();
        assertEquals("Crab crab = new Crab(new int[] {1, 2}, 3);\n", javaSource(new RecordedAction.Create(
            "Crab", "crab", Arguments.capture(new String[] {"  {1, 2}  ", " 3 "},
                new JavaType[] {ints, JavaPrimitiveType.getInt()}, false))));
        Method method = getClass().getMethod("when", int[][].class);
        assertEquals("Commands.when(new int[] {1}, new int[] {2, 3});\n", javaSource(new RecordedAction.MethodCall(
            "Commands", true, method, Arguments.capture(new String[] {" {1} ", " {2, 3} "},
                new JavaType[] {ints.getArray()}, method.isVarArgs()))));
        assertEquals("act();\n", javaSource(new RecordedAction.MethodCall(null, false,
            World.class.getMethod("act"), Arguments.capture(null, null, false))));
        assertEquals("addObject(crab,10,20);\n", javaSource(new RecordedAction.Add("crab", 10, 20)));
        assertEquals("crab.setLocation(30,40);\n", javaSource(new RecordedAction.Move("crab", 30, 40)));
        assertEquals("removeObject(crab);\n", javaSource(new RecordedAction.Remove("crab")));
    }

    /**
     * Kotlin escapes generated names but leaves even Java-only argument text and whitespace untouched.
     *
     * @throws NoSuchMethodException if a fixture or Greenfoot method cannot be found
     */
    @Test
    public void testKotlinStatementsAndUnchangedArguments() throws NoSuchMethodException
    {
        Arguments raw = Arguments.capture(new String[] {"  {1, 2}  ", " new Crab(\"$price\") "}, null, false);
        assertEquals("val `when` = example.`object`.Crab(  {1, 2}  ,  new Crab(\"$price\") )",
            new RecordedAction.Create("example.object.Crab", "when", raw).toKotlin());
        assertEquals("example.`object`.Commands.`when`(  {1, 2}  ,  new Crab(\"$price\") )",
            new RecordedAction.MethodCall("example.object.Commands", true,
                getClass().getMethod("when", int[][].class), raw).toKotlin());
        assertEquals("val value = Box<example.`when`>()", new RecordedAction.Create(
            "Box<example.when>", "value", Arguments.capture(null, null, false)).toKotlin());
        assertEquals("`when`.setLocation( 10 , 20)", new RecordedAction.MethodCall("when", false,
            Actor.class.getMethod("setLocation", int.class, int.class),
            Arguments.capture(new String[] {" 10 ", "20"}, null, false)).toKotlin());
        assertEquals("act()", new RecordedAction.MethodCall(null, false,
            World.class.getMethod("act"), Arguments.capture(null, null, false)).toKotlin());
        assertEquals("addObject(`when`, 10, 20)", new RecordedAction.Add("when", 10, 20).toKotlin());
        assertEquals("`when`.setLocation(30, 40)", new RecordedAction.Move("when", 30, 40).toKotlin());
        assertEquals("removeObject(`when`)", new RecordedAction.Remove("when").toKotlin());
    }

    /** Captured arrays and directly supplied lists cannot change the stored argument snapshot. */
    @Test
    public void testArgumentSnapshot()
    {
        JavaType integer = JavaPrimitiveType.getInt();
        String[] values = {" 1 "};
        JavaType[] types = {integer};
        Arguments captured = Arguments.capture(values, types, true);
        values[0] = "changed";
        types[0] = JavaPrimitiveType.getBoolean();
        assertEquals(List.of(" 1 "), captured.values());
        assertEquals(List.of(integer), captured.parameterTypes());
        assertTrue(captured.varArgs());

        List<String> valueList = new ArrayList<>(captured.values());
        List<JavaType> typeList = new ArrayList<>(captured.parameterTypes());
        Arguments copied = new Arguments(valueList, typeList, true);
        valueList.clear();
        typeList.clear();
        assertEquals(captured, copied);
        for (List<?> list : List.of(captured.values(), captured.parameterTypes(), copied.values(), copied.parameterTypes())) {
            try {
                list.clear();
                fail("Argument snapshots must be unmodifiable");
            }
            catch (UnsupportedOperationException expected) {
                // No writable collection view is exposed.
            }
        }
        assertEquals(new Arguments(List.of(), List.of(), false), Arguments.capture(null, null, false));
    }

    private static String javaSource(RecordedAction action)
    {
        return action.toJava().toJavaSource().toTemporaryJavaCodeString();
    }

    /**
     * Reflection-only fixture with a Kotlin-keyword name and array-valued varargs.
     * @param arrays unused arguments
     */
    public static void when(int[]... arrays)
    {
    }
}
