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
package bluej.editor.flow;

import java.util.List;

import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry;
import org.jetbrains.kotlin.psi.KtSuperTypeList;
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry;

import bluej.parser.SourceLocation;
import bluej.parser.entity.EntityResolver;
import bluej.parser.kotlin.KotlinEnvironmentManager;
import bluej.parser.kotlin.KotlinParsedCUNode;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.symtab.ClassInfo;

/**
 * Kotlin implementation of {@link FlowLanguageSupport}. Creates a
 * {@link KotlinParsedCUNode} for parsing and uses PSI {@code TextRange}
 * offsets with string splicing for supertype list editing. Each editing
 * method re-parses the editor text to a PSI tree, locates the relevant
 * supertype entries, and replaces the corresponding text range.
 *
 * <p>Kotlin uses a unified supertype list ({@code : SuperClass(), Interface})
 * for both class and interface inheritance. A {@link KtSuperTypeCallEntry}
 * (with constructor call parentheses) indicates a superclass; a plain
 * {@link KtSuperTypeListEntry} indicates an interface.</p>
 */
public class KotlinLanguageSupport implements FlowLanguageSupport
{
    @Override
    public ParsedCUNode createRootNode(EntityResolver resolver)
    {
        return new KotlinParsedCUNode();
    }

    @Override
    public void setExtendsClass(FlowEditor editor, String className, ClassInfo info)
    {
        String source = getSourceText(editor);
        KtClassOrObject cls = findClassOrObject(source);
        if (cls == null)
            return;

        KtSuperTypeCallEntry existing = findSuperclassEntry(cls);
        KtSuperTypeList superList = cls.getSuperTypeList();

        if (existing != null)
        {
            // Replace existing superclass entry: "OldName()" -> "NewName()"
            replaceRange(editor,
                existing.getTextRange().getStartOffset(),
                existing.getTextRange().getEndOffset(),
                className + "()");
        }
        else if (superList != null)
        {
            // Has interfaces but no superclass: insert "className(), " at
            // the start of the supertype list
            int insertOffset = superList.getTextRange().getStartOffset();
            replaceRange(editor, insertOffset, insertOffset, className + "(), ");
        }
        else
        {
            // No supertypes at all: insert " : className()"
            int insertOffset = findSupertypeInsertOffset(cls);
            replaceRange(editor, insertOffset, insertOffset, " : " + className + "()");
        }
    }

    @Override
    public void removeExtendsClass(FlowEditor editor, ClassInfo info)
    {
        String source = getSourceText(editor);
        KtClassOrObject cls = findClassOrObject(source);
        if (cls == null)
            return;

        KtSuperTypeCallEntry superEntry = findSuperclassEntry(cls);
        if (superEntry == null)
            return;

        removeSuperTypeEntry(editor, source, cls, superEntry);
    }

    @Override
    public void addImplements(FlowEditor editor, String interfaceName, ClassInfo info)
    {
        // Kotlin uses a unified supertype list for both class and interface
        // inheritance, so the logic is identical for addImplements and
        // addExtendsInterface
        addSupertype(editor, interfaceName);
    }

    @Override
    public void addExtendsInterface(FlowEditor editor, String interfaceName, ClassInfo info)
    {
        addSupertype(editor, interfaceName);
    }

    @Override
    public void removeInterface(FlowEditor editor, String interfaceName, ClassInfo info)
    {
        String source = getSourceText(editor);
        KtClassOrObject cls = findClassOrObject(source);
        if (cls == null)
            return;

        KtSuperTypeListEntry target = findEntryByName(cls, interfaceName);
        if (target == null)
            return;

        removeSuperTypeEntry(editor, source, cls, target);
    }

    // ----- Private helpers -----

    /**
     * Add a supertype (class or interface) to the Kotlin supertype list.
     * Checks for duplicates before inserting.
     */
    private void addSupertype(FlowEditor editor, String typeName)
    {
        String source = getSourceText(editor);
        KtClassOrObject cls = findClassOrObject(source);
        if (cls == null)
            return;

        KtSuperTypeList superList = cls.getSuperTypeList();

        if (superList != null)
        {
            // Already has supertypes: check for duplicates, then append
            if (findEntryByName(cls, typeName) != null)
                return;
            int endOffset = superList.getTextRange().getEndOffset();
            replaceRange(editor, endOffset, endOffset, ", " + typeName);
        }
        else
        {
            // No supertypes: insert " : typeName"
            int insertOffset = findSupertypeInsertOffset(cls);
            replaceRange(editor, insertOffset, insertOffset, " : " + typeName);
        }
    }

    /**
     * Remove a supertype entry from the list, handling comma separators
     * and removing the entire supertype clause when the last entry is removed.
     */
    private void removeSuperTypeEntry(FlowEditor editor, String source,
        KtClassOrObject cls, KtSuperTypeListEntry target)
    {
        List<KtSuperTypeListEntry> entries = cls.getSuperTypeListEntries();
        KtSuperTypeList superList = cls.getSuperTypeList();

        if (entries.size() == 1)
        {
            // Last supertype: remove entire " : Entry" span including colon
            var colon = cls.getColon();
            if (colon == null)
                return;
            int removeStart = colon.getTextRange().getStartOffset();
            // Include whitespace before colon
            if (removeStart > 0 && source.charAt(removeStart - 1) == ' ')
                removeStart--;
            replaceRange(editor, removeStart,
                superList.getTextRange().getEndOffset(), "");
        }
        else
        {
            // Multiple supertypes: remove this entry + its comma separator
            int entryStart = target.getTextRange().getStartOffset();
            int entryEnd = target.getTextRange().getEndOffset();
            boolean isFirst = entries.get(0).getTextRange().getStartOffset()
                == entryStart;

            if (isFirst)
            {
                // First entry: remove entry + trailing ", "
                int removeEnd = entryEnd;
                while (removeEnd < source.length()
                    && (source.charAt(removeEnd) == ','
                        || source.charAt(removeEnd) == ' '))
                {
                    removeEnd++;
                }
                replaceRange(editor, entryStart, removeEnd, "");
            }
            else
            {
                // Non-first entry: remove preceding ", " + entry
                int removeStart = entryStart;
                while (removeStart > 0
                    && (source.charAt(removeStart - 1) == ','
                        || source.charAt(removeStart - 1) == ' '))
                {
                    removeStart--;
                }
                replaceRange(editor, removeStart, entryEnd, "");
            }
        }
    }

    /**
     * Get the full source text from the editor.
     */
    private String getSourceText(FlowEditor editor)
    {
        return editor.getText(
            new SourceLocation(1, 1),
            editor.getLineColumnFromOffset(editor.getTextLength()));
    }

    /**
     * Parse source text to PSI and return the first class/object declaration,
     * or {@code null} if none found.
     */
    private KtClassOrObject findClassOrObject(String source)
    {
        KtFile ktFile = KotlinEnvironmentManager.getPsiFactory().createFile(source);
        for (KtDeclaration decl : ktFile.getDeclarations())
        {
            if (decl instanceof KtClassOrObject co)
                return co;
        }
        return null;
    }

    /**
     * Find the superclass entry in the supertype list. In Kotlin PSI, a
     * {@link KtSuperTypeCallEntry} (with constructor call parentheses)
     * indicates a superclass, while a plain entry indicates an interface.
     *
     * @return the superclass entry, or {@code null} if none
     */
    private KtSuperTypeCallEntry findSuperclassEntry(KtClassOrObject cls)
    {
        for (var entry : cls.getSuperTypeListEntries())
        {
            if (entry instanceof KtSuperTypeCallEntry callEntry)
                return callEntry;
        }
        return null;
    }

    /**
     * Find a supertype entry by its type name. Strips generic type arguments
     * for comparison (e.g., {@code Comparable<Foo>} matches {@code "Comparable"}).
     *
     * @return the matching entry, or {@code null} if not found
     */
    private KtSuperTypeListEntry findEntryByName(KtClassOrObject cls, String name)
    {
        for (var entry : cls.getSuperTypeListEntries())
        {
            var typeRef = entry.getTypeReference();
            String entryName = typeRef != null ? typeRef.getText() : entry.getText();
            // Strip generic type arguments: "Comparable<Foo>" -> "Comparable"
            int taIndex = entryName.indexOf('<');
            if (taIndex != -1)
                entryName = entryName.substring(0, taIndex);
            entryName = entryName.trim();
            if (name.equals(entryName))
                return entry;
        }
        return null;
    }

    /**
     * Find the offset where {@code " : SuperType"} should be inserted.
     * Checks: primary constructor end > type parameter list end > name end.
     */
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

    /**
     * Replace a character range in the editor. PSI offsets are 0-based
     * character offsets matching the document's internal positions.
     */
    private void replaceRange(FlowEditor editor,
        int startOffset, int endOffset, String newText)
    {
        editor.setSelection(
            editor.getLineColumnFromOffset(startOffset),
            editor.getLineColumnFromOffset(endOffset));
        editor.insertText(newText, false);
    }
}
