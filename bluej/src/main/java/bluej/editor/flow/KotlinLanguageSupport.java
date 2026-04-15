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
 * {@link KotlinParsedCUNode} for parsing and uses PSI to read the current
 * supertype list, then rebuilds the entire supertype clause from a modified
 * list of entry texts.
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
        if (cls == null) {
            return;
        }

        List<String> texts = new ArrayList<>();
        boolean replaced = false;
        for (var entry : cls.getSuperTypeListEntries()) {
            if (entry instanceof KtSuperTypeCallEntry) {
                texts.add(className + "()");
                replaced = true;
            } else {
                texts.add(entry.getText());
            }
        }
        if (!replaced) {
            // Superclass goes first in the list
            texts.add(0, className + "()");
        }
        rebuildSupertypeClause(editor, source, cls, texts);
    }

    @Override
    public void removeExtendsClass(FlowEditor editor, ClassInfo info)
    {
        String source = getSourceText(editor);
        KtClassOrObject cls = findClassOrObject(source);
        if (cls == null) {
            return;
        }

        List<String> texts = new ArrayList<>();
        for (var entry : cls.getSuperTypeListEntries()) {
            if (!(entry instanceof KtSuperTypeCallEntry)) {
                texts.add(entry.getText());
            }
        }
        rebuildSupertypeClause(editor, source, cls, texts);
    }

    @Override
    public void addImplements(FlowEditor editor, String interfaceName, ClassInfo info)
    {
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
        if (cls == null) {
            return;
        }

        List<String> texts = new ArrayList<>();
        for (var entry : cls.getSuperTypeListEntries()) {
            if (!stripGenerics(entryTypeName(entry)).equals(interfaceName)) {
                texts.add(entry.getText());
            }
        }
        rebuildSupertypeClause(editor, source, cls, texts);
    }

    /**
     * Add a supertype (class or interface) to the Kotlin supertype list.
     * Checks for duplicates before inserting.
     */
    private void addSupertype(FlowEditor editor, String typeName)
    {
        String source = getSourceText(editor);
        KtClassOrObject cls = findClassOrObject(source);
        if (cls == null) {
            return;
        }

        List<String> texts = new ArrayList<>();
        for (var entry : cls.getSuperTypeListEntries()) {
            if (stripGenerics(entryTypeName(entry)).equals(typeName)) {
                return; // already present
            }
            texts.add(entry.getText());
        }
        texts.add(typeName);
        rebuildSupertypeClause(editor, source, cls, texts);
    }

    /**
     * Replace the entire supertype clause in the editor. Handles three cases:
     * <ul>
     *   <li>Empty list — remove the entire "{@code  : ...}" clause</li>
     *   <li>Existing clause — replace the supertype list text range</li>
     *   <li>No existing clause — insert "{@code  : }" at the appropriate offset</li>
     * </ul>
     */
    private void rebuildSupertypeClause(FlowEditor editor, String source,
        KtClassOrObject cls, List<String> entryTexts)
    {
        KtSuperTypeList superList = cls.getSuperTypeList();
        String joined = String.join(", ", entryTexts);

        if (entryTexts.isEmpty()) {
            // Remove entire clause: " : ..."
            if (superList == null) {
                return;
            }
            var colon = cls.getColon();
            if (colon == null) {
                return;
            }
            int removeStart = colon.getTextRange().getStartOffset();
            if (removeStart > 0 && source.charAt(removeStart - 1) == ' ') {
                removeStart--;
            }
            replaceRange(editor, removeStart,
                superList.getTextRange().getEndOffset(), "");
        } else if (superList != null) {
            // Replace existing supertype list portion
            replaceRange(editor,
                superList.getTextRange().getStartOffset(),
                superList.getTextRange().getEndOffset(),
                joined);
        } else {
            // Insert new clause
            int offset = findSupertypeInsertOffset(cls);
            replaceRange(editor, offset, offset, " : " + joined);
        }
    }

    /** Returns the type reference text, falling back to the full entry text. */
    private String entryTypeName(KtSuperTypeListEntry entry)
    {
        var typeRef = entry.getTypeReference();
        return typeRef != null ? typeRef.getText() : entry.getText();
    }

    /**
     * Strip generic type arguments from a type name.
     * E.g., {@code "Comparable<Foo>"} becomes {@code "Comparable"}.
     */
    private String stripGenerics(String typeName)
    {
        int idx = typeName.indexOf('<');
        return idx != -1 ? typeName.substring(0, idx).trim() : typeName.trim();
    }

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
        for (KtDeclaration decl : ktFile.getDeclarations()) {
            if (decl instanceof KtClassOrObject co) {
                return co;
            }
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
        if (ctor != null) {
            return ctor.getTextRange().getEndOffset();
        }
        var tpList = cls.getTypeParameterList();
        if (tpList != null) {
            return tpList.getTextRange().getEndOffset();
        }
        var nameIdent = cls.getNameIdentifier();
        if (nameIdent != null) {
            return nameIdent.getTextRange().getEndOffset();
        }
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
