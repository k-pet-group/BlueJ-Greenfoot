/*
 This file is part of the BlueJ program.
 Copyright (C) 2025,2026  Michael Kolling and John Rosenberg

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

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtPsiFactory;
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry;
import org.jetbrains.kotlin.psi.KtSuperTypeList;
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for PSI-based supertype clause rebuilding in BlueJ's standalone
 * KotlinCoreEnvironment. Each test parses source to PSI, modifies the
 * supertype entry list, and rebuilds the entire clause — mirroring the
 * approach used by {@code KotlinLanguageSupport}.
 */
public class KotlinPsiEditingTest
{
    private KtPsiFactory factory()
    {
        return KotlinEnvironmentManager.getPsiFactory();
    }

    private KtClassOrObject findClass(KtFile ktFile)
    {
        for (KtDeclaration decl : ktFile.getDeclarations())
        {
            if (decl instanceof KtClassOrObject co)
            {
                return co;
            }
        }
        fail("No class declaration found");
        return null;
    }

    // ===== Single-line class declarations =====

    @Test
    public void testAddSuperclass()
    {
        String result = rebuildWith("class Foo {\n}", cls -> {
            List<String> texts = collectEntryTexts(cls);
            texts.add(0, "Base()");
            return texts;
        });
        assertEquals("class Foo : Base() {\n}", result);
    }

    @Test
    public void testReplaceSuperclass()
    {
        String result = rebuildWith("class Foo : OldBase() {\n}", cls -> {
            List<String> texts = new ArrayList<>();
            for (var entry : cls.getSuperTypeListEntries())
            {
                if (entry instanceof KtSuperTypeCallEntry)
                    texts.add("NewBase()");
                else
                    texts.add(entry.getText());
            }
            return texts;
        });
        assertEquals("class Foo : NewBase() {\n}", result);
    }

    @Test
    public void testAddInterface()
    {
        String result = rebuildWith("class Foo : Base() {\n}", cls -> {
            List<String> texts = collectEntryTexts(cls);
            texts.add("Runnable");
            return texts;
        });
        assertEquals("class Foo : Base(), Runnable {\n}", result);
    }

    @Test
    public void testRemoveLastSupertype()
    {
        String result = rebuildWith("class Foo : Base() {\n}", cls -> {
            return new ArrayList<>();
        });
        assertEquals("class Foo {\n}", result);
    }

    @Test
    public void testRemoveOneInterface()
    {
        String result = rebuildWith(
            "class Foo : Base(), Runnable, Comparable<Foo> {\n}", cls -> {
                List<String> texts = new ArrayList<>();
                for (var entry : cls.getSuperTypeListEntries())
                {
                    String name = entryTypeName(entry);
                    if (!stripGenerics(name).equals("Runnable"))
                        texts.add(entry.getText());
                }
                return texts;
            });
        assertEquals("class Foo : Base(), Comparable<Foo> {\n}", result);
    }

    @Test
    public void testRemoveSuperclass()
    {
        String result = rebuildWith(
            "class Foo : Base(), Runnable, Comparable<Foo> {\n}", cls -> {
                List<String> texts = new ArrayList<>();
                for (var entry : cls.getSuperTypeListEntries())
                {
                    if (!(entry instanceof KtSuperTypeCallEntry))
                        texts.add(entry.getText());
                }
                return texts;
            });
        assertEquals("class Foo : Runnable, Comparable<Foo> {\n}", result);
    }

    // ===== Multiline class declarations =====

    @Test
    public void testMultilineAddInterface()
    {
        String source = "class SomeClass(\n    val x: Int\n): Interface {\n}";
        String result = rebuildWith(source, cls -> {
            List<String> texts = collectEntryTexts(cls);
            texts.add("Runnable");
            return texts;
        });
        assertEquals(
            "class SomeClass(\n    val x: Int\n): Interface, Runnable {\n}",
            result);
    }

    @Test
    public void testMultilineRemoveLastSupertype()
    {
        String source = "class SomeClass(\n    val x: Int\n): Interface {\n}";
        String result = rebuildWith(source, cls -> {
            return new ArrayList<>();
        });
        assertEquals("class SomeClass(\n    val x: Int\n) {\n}", result);
    }

    @Test
    public void testMultilineRemoveOneOfTwo()
    {
        String source = "class SomeClass(\n    val x: Int\n): Base(), Interface {\n}";
        String result = rebuildWith(source, cls -> {
            List<String> texts = new ArrayList<>();
            for (var entry : cls.getSuperTypeListEntries())
            {
                if (!(entry instanceof KtSuperTypeCallEntry))
                    texts.add(entry.getText());
            }
            return texts;
        });
        assertEquals(
            "class SomeClass(\n    val x: Int\n): Interface {\n}",
            result);
    }

    @Test
    public void testMultilineReplaceSuperclass()
    {
        String source = "class SomeClass(\n    val x: Int\n): OldBase(), Interface {\n}";
        String result = rebuildWith(source, cls -> {
            List<String> texts = new ArrayList<>();
            for (var entry : cls.getSuperTypeListEntries())
            {
                if (entry instanceof KtSuperTypeCallEntry)
                    texts.add("NewBase()");
                else
                    texts.add(entry.getText());
            }
            return texts;
        });
        assertEquals(
            "class SomeClass(\n    val x: Int\n): NewBase(), Interface {\n}",
            result);
    }

    @Test
    public void testMultilineAddSuperclassToBarClass()
    {
        String source = "class SomeClass(\n    val x: Int\n) {\n}";
        String result = rebuildWith(source, cls -> {
            List<String> texts = new ArrayList<>();
            texts.add("Base()");
            return texts;
        });
        assertEquals(
            "class SomeClass(\n    val x: Int\n) : Base() {\n}",
            result);
    }

    // ===== Helpers =====

    /**
     * Apply a clause-rebuild operation and return the resulting source string.
     * The modifier function receives the parsed class and returns the desired
     * list of supertype entry texts.
     */
    private String rebuildWith(String source,
        java.util.function.Function<KtClassOrObject, List<String>> modifier)
    {
        KtFile ktFile = factory().createFile("test.kt", source);
        KtClassOrObject cls = findClass(ktFile);
        List<String> entryTexts = modifier.apply(cls);
        return rebuildSupertypeClause(source, cls, entryTexts);
    }

    /**
     * Rebuild the supertype clause in the source string — mirrors the logic
     * in KotlinLanguageSupport.rebuildSupertypeClause() but operates on
     * strings instead of FlowEditor.
     */
    private String rebuildSupertypeClause(String source,
        KtClassOrObject cls, List<String> entryTexts)
    {
        KtSuperTypeList superList = cls.getSuperTypeList();
        String joined = String.join(", ", entryTexts);

        if (entryTexts.isEmpty())
        {
            if (superList == null)
                return source;
            var colon = cls.getColon();
            if (colon == null)
                return source;
            int removeStart = colon.getTextRange().getStartOffset();
            if (removeStart > 0 && source.charAt(removeStart - 1) == ' ')
                removeStart--;
            return source.substring(0, removeStart)
                + source.substring(superList.getTextRange().getEndOffset());
        }
        else if (superList != null)
        {
            return source.substring(0, superList.getTextRange().getStartOffset())
                + joined
                + source.substring(superList.getTextRange().getEndOffset());
        }
        else
        {
            int offset = findSupertypeInsertOffset(cls);
            return source.substring(0, offset)
                + " : " + joined
                + source.substring(offset);
        }
    }

    private List<String> collectEntryTexts(KtClassOrObject cls)
    {
        List<String> texts = new ArrayList<>();
        for (var entry : cls.getSuperTypeListEntries())
        {
            texts.add(entry.getText());
        }
        return texts;
    }

    private String entryTypeName(KtSuperTypeListEntry entry)
    {
        var typeRef = entry.getTypeReference();
        return typeRef != null ? typeRef.getText() : entry.getText();
    }

    private String stripGenerics(String typeName)
    {
        int idx = typeName.indexOf('<');
        return idx != -1 ? typeName.substring(0, idx).trim() : typeName.trim();
    }

    private int findSupertypeInsertOffset(KtClassOrObject cls)
    {
        var ctor = cls.getPrimaryConstructor();
        if (ctor != null)
            return ctor.getTextRange().getEndOffset();
        var tpList = cls.getTypeParameterList();
        if (tpList != null)
            return tpList.getTextRange().getEndOffset();
        var nameIdent = cls.getNameIdentifier();
        if (nameIdent != null)
            return nameIdent.getTextRange().getEndOffset();
        return cls.getTextRange().getStartOffset();
    }
}
