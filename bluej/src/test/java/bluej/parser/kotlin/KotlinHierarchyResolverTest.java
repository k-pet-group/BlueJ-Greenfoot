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
package bluej.parser.kotlin;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

import bluej.parser.kotlin.KotlinHierarchyResolver.TypeKind;
import bluej.parser.symtab.ClassInfo;

import org.junit.Test;

import static org.junit.Assert.*;

/** Tests exact hierarchy identities independently of project/UI construction. */
public class KotlinHierarchyResolverTest
{
    private ClassInfo parse(String source, Map<String, TypeKind> types)
    {
        return KotlinInfoParser.parse(new StringReader(source), null, null, types::get);
    }

    /** Qualified generic names must keep every identifier segment. */
    @Test
    public void testQualifiedGenericSupertype()
    {
        assertEquals("library.Outer.Base", parse("class Child : library.Outer<String>.Base<Int>()",
            Map.of()).getSuperclass());
    }

    /** Aliases name the imported declaration, not the local spelling. */
    @Test
    public void testAliasedImport()
    {
        ClassInfo info = parse("import library.Base as Scene\nimport library.Action as Task\n"
            + "class Child : Scene(), Task", Map.of("library.Action", TypeKind.INTERFACE));
        assertEquals("library.Base", info.getSuperclass());
        assertEquals(List.of("library.Action"), info.getImplements());
    }

    /** A wildcard is usable only when the exact candidate exists. */
    @Test
    public void testVerifiedStarImport()
    {
        assertEquals("library.Base", parse("import missing.*\nimport library.*\nclass Child : Base()",
            Map.of("library.Base", TypeKind.CLASS)).getSuperclass());
        assertNull(parse("import missing.*\nclass Child : Base()", Map.of()).getSuperclass());
    }

    /** Two verified wildcard candidates are ambiguous; declaration order is irrelevant. */
    @Test
    public void testAmbiguousStarImports()
    {
        Map<String, TypeKind> types = Map.of("first.Base", TypeKind.CLASS, "second.Base", TypeKind.CLASS);
        assertNull(parse("import first.*\nimport second.*\nclass Child : Base()", types).getSuperclass());
        assertNull(parse("import second.*\nimport first.*\nclass Child : Base()", types).getSuperclass());
    }

    /** Ambiguous imported outer types must not be mistaken for a package qualifier. */
    @Test
    public void testAmbiguousImportedOuterType()
    {
        assertNull(parse("import first.*\nimport second.*\nclass Child : Outer.Base()",
            Map.of("first.Outer", TypeKind.CLASS, "second.Outer", TypeKind.CLASS)).getSuperclass());
    }

    /** Same-package source identities work without any bytecode and precede wildcard imports. */
    @Test
    public void testSamePackageClassAndInterface()
    {
        ClassInfo info = parse("package demo\nimport library.*\nclass Child : Base(), Action",
            Map.of("demo.Base", TypeKind.CLASS, "demo.Action", TypeKind.INTERFACE,
                "library.Base", TypeKind.CLASS, "library.Action", TypeKind.CLASS));
        assertEquals("demo.Base", info.getSuperclass());
        assertEquals(List.of("demo.Action"), info.getImplements());
    }

    /** Secondary constructors do not turn the superclass into an implemented interface. */
    @Test
    public void testSecondaryConstructor()
    {
        ClassInfo info = parse("import library.World\nimport library.Action\n"
            + "class Child : World, Action { constructor() : super(10, 10) }",
            Map.of("library.World", TypeKind.CLASS, "library.Action", TypeKind.INTERFACE));
        assertEquals("library.World", info.getSuperclass());
        assertEquals(List.of("library.Action"), info.getImplements());
    }

    /** Values and functions do not occupy Kotlin's classifier namespace for supertype lookup. */
    @Test
    public void testMemberValuesDoNotShadowSupertypes()
    {
        Map<String, TypeKind> types = Map.of("library.Base", TypeKind.CLASS);
        assertEquals("library.Base", parse("class Child : library.Base() { val library = 1 }", types)
            .getSuperclass());
        assertEquals("library.Base", parse("import library.Base\nclass Child : Base() { fun Base() {} }", types)
            .getSuperclass());
        assertEquals("library.Base", parse("import library.Base as Parent\nclass Child : Parent() { val Parent = 1 }",
            types).getSuperclass());
    }
}
