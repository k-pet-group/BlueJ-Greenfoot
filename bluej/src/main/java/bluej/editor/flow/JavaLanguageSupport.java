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

import bluej.parser.SourceLocation;
import bluej.parser.entity.EntityResolver;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.symtab.ClassInfo;
import bluej.parser.symtab.Selection;

/**
 * Java implementation of {@link FlowLanguageSupport}. Creates a standard
 * {@link ParsedCUNode} for parsing and uses Java syntax ({@code extends},
 * {@code implements}) for class declaration editing.
 */
public class JavaLanguageSupport implements FlowLanguageSupport
{
    @Override
    public ParsedCUNode createRootNode(EntityResolver resolver)
    {
        return new ParsedCUNode(resolver);
    }

    @Override
    public void setExtendsClass(FlowEditor editor, String className, ClassInfo info)
    {
        if (info.getSuperclass() == null) {
            Selection s1 = info.getExtendsInsertSelection();

            editor.setSelection(
                new SourceLocation(s1.getLine(), s1.getColumn()),
                new SourceLocation(s1.getEndLine(), s1.getEndColumn()));
            editor.insertText(" extends " + className, false);
        } else {
            Selection s1 = info.getSuperReplaceSelection();

            editor.setSelection(
                new SourceLocation(s1.getLine(), s1.getColumn()),
                new SourceLocation(s1.getEndLine(), s1.getEndColumn()));
            editor.insertText(className, false);
        }
    }

    @Override
    public void removeExtendsClass(FlowEditor editor, ClassInfo info)
    {
        Selection s1 = info.getExtendsReplaceSelection();
        s1.combineWith(info.getSuperReplaceSelection());

        if (s1 != null) {
            editor.setSelection(
                new SourceLocation(s1.getLine(), s1.getColumn()),
                new SourceLocation(s1.getEndLine(), s1.getEndColumn()));
            editor.insertText("", false);
        }
    }

    @Override
    public void addImplements(FlowEditor editor, String interfaceName, ClassInfo info)
    {
        Selection s1 = info.getImplementsInsertSelection();
        editor.setSelection(
            new SourceLocation(s1.getLine(), s1.getColumn()),
            new SourceLocation(s1.getEndLine(), s1.getEndColumn()));

        if (info.hasInterfaceSelections()) {
            List<String> exists = editor.getInterfaceTexts(info.getInterfaceSelections());

            if (!exists.contains(interfaceName)) {
                editor.insertText(", " + interfaceName, false);
            }
        } else {
            editor.insertText(" implements " + interfaceName, false);
        }
    }

    @Override
    public void addExtendsInterface(FlowEditor editor, String interfaceName, ClassInfo info)
    {
        Selection s1 = info.getExtendsInsertSelection();
        editor.setSelection(
            new SourceLocation(s1.getLine(), s1.getColumn()),
            new SourceLocation(s1.getEndLine(), s1.getEndColumn()));

        if (info.hasInterfaceSelections()) {
            List<String> exists = editor.getInterfaceTexts(info.getInterfaceSelections());

            if (!exists.contains(interfaceName)) {
                editor.insertText(", " + interfaceName, false);
            }
        } else {
            editor.insertText(" extends " + interfaceName, false);
        }
    }

    @Override
    public void removeInterface(FlowEditor editor, String interfaceName, ClassInfo info)
    {
        List<Selection> vsels = info.getInterfaceSelections();
        List<String> vtexts = editor.getInterfaceTexts(vsels);
        int where = vtexts.indexOf(interfaceName);

        // Special case: deleting the first interface when others remain —
        // delete the following comma instead of the preceding one
        if (where == 1 && vsels.size() > 2) {
            where = 2;
        }

        if (where > 0) {
            Selection s1 = vsels.get(where - 1);
            s1.combineWith(vsels.get(where));

            editor.setSelection(
                new SourceLocation(s1.getLine(), s1.getColumn()),
                new SourceLocation(s1.getEndLine(), s1.getEndColumn()));
            editor.insertText("", false);
        }
    }
}
