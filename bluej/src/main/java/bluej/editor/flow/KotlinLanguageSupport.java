/*
 This file is part of the BlueJ program.
 Copyright (C) 2025  Michael Kolling and John Rosenberg

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

import bluej.parser.SourceLocation;
import bluej.parser.entity.EntityResolver;
import bluej.parser.kotlin.KotlinParsedCUNode;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.symtab.ClassInfo;
import bluej.parser.symtab.Selection;

/**
 * Kotlin implementation of {@link FlowLanguageSupport}. Creates a
 * {@link KotlinParsedCUNode} for parsing and uses Kotlin's unified supertype
 * list syntax ({@code : SuperClass(), Interface}) for class declaration editing.
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
        if (info.getSuperclass() == null)
        {
            if (info.hasInterfaceSelections())
            {
                // Has interfaces but no superclass: insert "className(), "
                // before the first interface in the supertype list
                Selection firstIface = info.getInterfaceSelections().get(1);
                editor.setSelection(
                    new SourceLocation(firstIface.getLine(), firstIface.getColumn()),
                    new SourceLocation(firstIface.getLine(), firstIface.getColumn()));
                editor.insertText(className + "(), ", false);
            }
            else
            {
                // No supertypes at all: insert " : className()"
                Selection s1 = info.getExtendsInsertSelection();
                editor.setSelection(
                    new SourceLocation(s1.getLine(), s1.getColumn()),
                    new SourceLocation(s1.getEndLine(), s1.getEndColumn()));
                editor.insertText(" : " + className + "()", false);
            }
        }
        else
        {
            // Replace existing superclass: replace "OldName()" with "NewName()"
            Selection s1 = info.getSuperReplaceSelection();
            editor.setSelection(
                new SourceLocation(s1.getLine(), s1.getColumn()),
                new SourceLocation(s1.getEndLine(), s1.getEndColumn()));
            editor.insertText(className + "()", false);
        }
    }

    @Override
    public void removeExtendsClass(FlowEditor editor, ClassInfo info)
    {
        Selection superSel = info.getSuperReplaceSelection();
        if (superSel != null)
        {
            if (info.hasInterfaceSelections())
            {
                // Has interfaces after superclass: remove superclass
                // entry and its trailing comma, keeping ": Interface..."
                // Delete from superclass start to first interface start
                Selection firstIface = info.getInterfaceSelections().get(1);
                editor.setSelection(
                    new SourceLocation(superSel.getLine(), superSel.getColumn()),
                    new SourceLocation(firstIface.getLine(), firstIface.getColumn()));
                editor.insertText("", false);
            }
            else
            {
                // Superclass is the only supertype: remove entire
                // " : SuperClass()" span
                Selection extReplace = info.getExtendsReplaceSelection();
                if (extReplace != null)
                {
                    extReplace.combineWith(superSel);
                    editor.setSelection(
                        new SourceLocation(extReplace.getLine(), extReplace.getColumn()),
                        new SourceLocation(extReplace.getEndLine(), extReplace.getEndColumn()));
                }
                else
                {
                    editor.setSelection(
                        new SourceLocation(superSel.getLine(), superSel.getColumn()),
                        new SourceLocation(superSel.getEndLine(), superSel.getEndColumn()));
                }
                editor.insertText("", false);
            }
        }
    }

    @Override
    public void addImplements(FlowEditor editor, String interfaceName, ClassInfo info)
    {
        // Kotlin uses a unified supertype list for both class and interface inheritance
        addSupertype(editor, interfaceName, info);
    }

    @Override
    public void addExtendsInterface(FlowEditor editor, String interfaceName, ClassInfo info)
    {
        // Kotlin uses a unified supertype list — same logic as addImplements
        addSupertype(editor, interfaceName, info);
    }

    /**
     * Shared implementation for adding a supertype (class or interface) to the
     * Kotlin supertype list. Kotlin uses a single colon-separated list for both
     * class and interface inheritance, so the logic is identical.
     */
    private void addSupertype(FlowEditor editor, String typeName, ClassInfo info)
    {
        Selection s1 = info.getImplementsInsertSelection();
        editor.setSelection(
            new SourceLocation(s1.getLine(), s1.getColumn()),
            new SourceLocation(s1.getEndLine(), s1.getEndColumn()));

        if (info.hasInterfaceSelections() || info.getSuperclass() != null)
        {
            // Already has supertypes: append ", typeName"
            if (info.hasInterfaceSelections())
            {
                List<String> exists = editor.getInterfaceTexts(info.getInterfaceSelections());
                if (!exists.contains(typeName))
                    editor.insertText(", " + typeName, false);
            }
            else
            {
                editor.insertText(", " + typeName, false);
            }
        }
        else
        {
            // No supertypes at all: insert " : typeName"
            editor.insertText(" : " + typeName, false);
        }
    }
}
