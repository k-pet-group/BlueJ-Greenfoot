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

import org.jetbrains.kotlin.com.intellij.lang.ASTNode;
import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange;
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
 * Proof-of-concept: validate PSI-based supertype editing approaches in
 * BlueJ's standalone KotlinCoreEnvironment.
 *
 * Tests three approaches:
 * 1. High-level PSI modification (addSuperTypeListEntry/removeSuperTypeListEntry)
 * 2. Low-level AST node manipulation (ASTNode.addChild/removeChild)
 * 3. Text-based rebuilding (parse → modify string → reparse)
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

    // ===== Approach 1: High-level PSI modification =====
    // Expected to FAIL in standalone environment (missing extension points)

    @Test
    public void testHighLevelAdd_expectsFailure()
    {
        KtFile ktFile = factory().createFile("test.kt", "class Foo {\n}");
        KtClassOrObject cls = findClass(ktFile);

        try
        {
            cls.addSuperTypeListEntry(factory().createSuperTypeCallEntry("Base()"));
            // If we get here, it works!
            assertTrue("High-level add succeeded", ktFile.getText().contains("Base()"));
        }
        catch (IllegalArgumentException | IllegalStateException e)
        {
            // Expected: standalone environment lacks IntelliJ platform services
            System.out.println("  High-level add failed (expected): " + e.getMessage());
            assertTrue("Should fail with missing extension or service",
                e.getMessage().contains("extension") || e.getMessage().contains("null"));
        }
    }

    @Test
    public void testHighLevelRemove_expectsFailure()
    {
        KtFile ktFile = factory().createFile("test.kt", "class Foo : Base() {\n}");
        KtClassOrObject cls = findClass(ktFile);

        try
        {
            cls.removeSuperTypeListEntry(cls.getSuperTypeListEntries().get(0));
            assertFalse("High-level remove succeeded", ktFile.getText().contains("Base"));
        }
        catch (IllegalArgumentException | IllegalStateException e)
        {
            System.out.println("  High-level remove failed (expected): " + e.getMessage());
            assertTrue("Should fail with missing service",
                e.getMessage().contains("extension") || e.getMessage().contains("null"));
        }
    }

    // ===== Approach 2: Low-level AST node manipulation =====

    @Test
    public void testAstLevelRemoveChild()
    {
        KtFile ktFile = factory().createFile("test.kt",
            "class Foo : Base(), Runnable {\n}");
        KtClassOrObject cls = findClass(ktFile);
        KtSuperTypeList superList = cls.getSuperTypeList();
        assertNotNull("Should have supertype list", superList);

        try
        {
            // Try removing a child at the AST level
            ASTNode listNode = superList.getNode();
            KtSuperTypeListEntry firstEntry = cls.getSuperTypeListEntries().get(0);
            ASTNode entryNode = firstEntry.getNode();

            listNode.removeChild(entryNode);

            String result = ktFile.getText();
            System.out.println("  AST remove result: " + result);
            assertFalse("Should not contain Base after AST removal",
                result.contains("Base"));
        }
        catch (Exception e)
        {
            System.out.println("  AST-level remove failed: " + e.getClass().getName()
                + ": " + e.getMessage());
            // Document whether this approach works
            fail("AST-level remove failed: " + e.getMessage());
        }
    }

    @Test
    public void testAstLevelAddChild()
    {
        KtFile ktFile = factory().createFile("test.kt",
            "class Foo : Base() {\n}");
        KtClassOrObject cls = findClass(ktFile);
        KtSuperTypeList superList = cls.getSuperTypeList();
        assertNotNull("Should have supertype list", superList);

        try
        {
            // Create a new entry and try adding at AST level
            KtFile tempFile = factory().createFile("temp.kt",
                "class Temp : Runnable");
            KtClassOrObject tempCls = findClass(tempFile);
            KtSuperTypeListEntry newEntry = tempCls.getSuperTypeListEntries().get(0);

            ASTNode listNode = superList.getNode();
            ASTNode newNode = newEntry.getNode().copyElement();

            listNode.addChild(newNode, null);

            String result = ktFile.getText();
            System.out.println("  AST add result: " + result);
            assertTrue("Should contain both supertypes",
                result.contains("Base") && result.contains("Runnable"));
        }
        catch (Exception e)
        {
            System.out.println("  AST-level add failed: " + e.getClass().getName()
                + ": " + e.getMessage());
            fail("AST-level add failed: " + e.getMessage());
        }
    }

    // ===== Approach 3: Text-based rebuilding =====
    // Parse to read → build new source string → reparse

    @Test
    public void testTextBasedAddSuperclass()
    {
        String source = "class Foo {\n}";
        KtFile ktFile = factory().createFile("test.kt", source);
        KtClassOrObject cls = findClass(ktFile);

        // Use PSI to find insertion point (after class name / type params / constructor)
        int insertOffset = findSupertypeInsertOffset(cls);
        String newSource = source.substring(0, insertOffset)
            + " : Base()" + source.substring(insertOffset);

        // Verify by reparsing
        KtFile newFile = factory().createFile("test.kt", newSource);
        KtClassOrObject newCls = findClass(newFile);
        assertEquals("Should have 1 supertype entry", 1,
            newCls.getSuperTypeListEntries().size());
        assertTrue("Entry should be Base()",
            newCls.getSuperTypeListEntries().get(0) instanceof KtSuperTypeCallEntry);

        System.out.println("  Text-based add: " + newSource);
    }

    @Test
    public void testTextBasedAddInterface()
    {
        String source = "class Foo : Base() {\n}";
        KtFile ktFile = factory().createFile("test.kt", source);
        KtClassOrObject cls = findClass(ktFile);

        // Find the end of the supertype list
        KtSuperTypeList superList = cls.getSuperTypeList();
        assertNotNull(superList);
        int endOffset = superList.getTextRange().getEndOffset();

        String newSource = source.substring(0, endOffset)
            + ", Runnable" + source.substring(endOffset);

        KtFile newFile = factory().createFile("test.kt", newSource);
        KtClassOrObject newCls = findClass(newFile);
        assertEquals("Should have 2 supertype entries", 2,
            newCls.getSuperTypeListEntries().size());

        System.out.println("  Text-based add interface: " + newSource);
    }

    @Test
    public void testTextBasedRemoveSuperclass()
    {
        String source = "class Foo : Base() {\n}";
        KtFile ktFile = factory().createFile("test.kt", source);
        KtClassOrObject cls = findClass(ktFile);

        // Remove " : Base()" — from before colon to end of supertype list
        var colon = cls.getColon();
        assertNotNull("Should have colon", colon);
        KtSuperTypeList superList = cls.getSuperTypeList();
        assertNotNull(superList);

        // Include whitespace before colon
        int removeStart = colon.getTextRange().getStartOffset();
        if (removeStart > 0 && source.charAt(removeStart - 1) == ' ')
        {
            removeStart--;
        }
        int removeEnd = superList.getTextRange().getEndOffset();

        String newSource = source.substring(0, removeStart)
            + source.substring(removeEnd);

        KtFile newFile = factory().createFile("test.kt", newSource);
        KtClassOrObject newCls = findClass(newFile);
        assertTrue("Should have no supertypes",
            newCls.getSuperTypeListEntries().isEmpty());

        System.out.println("  Text-based remove: '" + newSource + "'");
    }

    @Test
    public void testTextBasedRemoveOneInterface()
    {
        String source = "class Foo : Base(), Runnable, Comparable<Foo> {\n}";
        KtFile ktFile = factory().createFile("test.kt", source);
        KtClassOrObject cls = findClass(ktFile);

        // Find the "Runnable" entry and its surrounding comma
        var entries = cls.getSuperTypeListEntries();
        assertEquals(3, entries.size());

        KtSuperTypeListEntry runnable = entries.get(1);
        TextRange runnableRange = runnable.getTextRange();

        // Need to also remove the preceding ", "
        // Find the comma before this entry
        int removeStart = runnableRange.getStartOffset();
        // Walk backward past whitespace and comma
        while (removeStart > 0 && (source.charAt(removeStart - 1) == ' '
            || source.charAt(removeStart - 1) == ','))
        {
            removeStart--;
        }

        String newSource = source.substring(0, removeStart)
            + source.substring(runnableRange.getEndOffset());

        KtFile newFile = factory().createFile("test.kt", newSource);
        KtClassOrObject newCls = findClass(newFile);
        assertEquals("Should have 2 entries", 2,
            newCls.getSuperTypeListEntries().size());
        assertFalse("Should not contain Runnable",
            newFile.getText().contains("Runnable"));

        System.out.println("  Text-based remove interface: " + newSource);
    }

    @Test
    public void testTextBasedReplaceSuperclass()
    {
        String source = "class Foo : OldBase() {\n}";
        KtFile ktFile = factory().createFile("test.kt", source);
        KtClassOrObject cls = findClass(ktFile);

        KtSuperTypeListEntry entry = cls.getSuperTypeListEntries().get(0);
        TextRange range = entry.getTextRange();

        String newSource = source.substring(0, range.getStartOffset())
            + "NewBase()" + source.substring(range.getEndOffset());

        KtFile newFile = factory().createFile("test.kt", newSource);
        KtClassOrObject newCls = findClass(newFile);
        assertFalse("Should not contain OldBase", newFile.getText().contains("OldBase"));
        assertTrue("Should contain NewBase", newFile.getText().contains("NewBase()"));

        System.out.println("  Text-based replace: " + newSource);
    }

    @Test
    public void testGetColonAvailable()
    {
        KtFile ktFile = factory().createFile("test.kt", "class Foo : Base() {\n}");
        KtClassOrObject cls = findClass(ktFile);

        // Verify getColon() works (vs our manual findChildByText)
        var colon = cls.getColon();
        assertNotNull("getColon() should return the ':' token", colon);
        assertEquals(":", colon.getText());
    }

    @Test
    public void testGetSuperTypeListAvailable()
    {
        KtFile ktFile = factory().createFile("test.kt",
            "class Foo : Base(), Runnable {\n}");
        KtClassOrObject cls = findClass(ktFile);

        KtSuperTypeList list = cls.getSuperTypeList();
        assertNotNull("getSuperTypeList() should return the list node", list);
        assertEquals("Should have 2 entries", 2, list.getEntries().size());

        // The list's text range covers "Base(), Runnable"
        String listText = list.getText();
        assertTrue("List text should contain 'Base()', got: " + listText,
            listText.contains("Base()"));
        assertTrue("List text should contain 'Runnable', got: " + listText,
            listText.contains("Runnable"));
    }

    // ===== Approach 4: Clause-rebuild =====
    // Parse → collect entry texts → modify list → rebuild entire clause

    @Test
    public void testClauseRebuildAddSuperclass()
    {
        String result = rebuildWith("class Foo {\n}", cls -> {
            List<String> texts = collectEntryTexts(cls);
            texts.add(0, "Base()");
            return texts;
        });
        assertEquals("class Foo : Base() {\n}", result);
    }

    @Test
    public void testClauseRebuildReplaceSuperclass()
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
    public void testClauseRebuildAddInterface()
    {
        String result = rebuildWith("class Foo : Base() {\n}", cls -> {
            List<String> texts = collectEntryTexts(cls);
            texts.add("Runnable");
            return texts;
        });
        assertEquals("class Foo : Base(), Runnable {\n}", result);
    }

    @Test
    public void testClauseRebuildRemoveLastSupertype()
    {
        String result = rebuildWith("class Foo : Base() {\n}", cls -> {
            return new ArrayList<>(); // empty = remove clause
        });
        assertEquals("class Foo {\n}", result);
    }

    @Test
    public void testClauseRebuildRemoveOneInterface()
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
    public void testClauseRebuildRemoveFirstInterface()
    {
        String result = rebuildWith(
            "class Foo : Base(), Runnable, Comparable<Foo> {\n}", cls -> {
                List<String> texts = new ArrayList<>();
                for (var entry : cls.getSuperTypeListEntries())
                {
                    if (!(entry instanceof KtSuperTypeCallEntry))
                    {
                        // keep only interfaces, skip superclass
                    }
                    else
                    {
                        // Actually, let's remove Base() (first entry)
                    }
                }
                // Remove superclass, keep interfaces
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

    // ===== Clause-rebuild helpers =====

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

    /**
     * Find the offset where " : SuperType" would be inserted.
     * Looks for primary constructor end > type parameter list end > name end.
     */
    private int findSupertypeInsertOffset(KtClassOrObject cls)
    {
        var ctor = cls.getPrimaryConstructor();
        if (ctor != null)
        {
            return ctor.getTextRange().getEndOffset();
        }
        var tpList = cls.getTypeParameterList();
        if (tpList != null)
        {
            return tpList.getTextRange().getEndOffset();
        }
        var nameIdent = cls.getNameIdentifier();
        if (nameIdent != null)
        {
            return nameIdent.getTextRange().getEndOffset();
        }
        return cls.getTextRange().getStartOffset();
    }
}
