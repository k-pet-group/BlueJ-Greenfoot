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
            // if we already have an implements clause then we need to put a
            // comma and the interface name but not before checking that we
            // don't already have it

            List<String> exists = editor.getInterfaceTexts(info.getInterfaceSelections());

            // XXX make this equality check against full package name
            if (!exists.contains(interfaceName)) {
                editor.insertText(", " + interfaceName, false);
            }
        } else {
            // otherwise we need to put the actual "implements" word
            // and the interface name
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
            // if we already have an extends clause then we need to put a
            // comma and the interface name but not before checking that we
            // don't already have it

            List<String> exists = editor.getInterfaceTexts(info.getInterfaceSelections());

            // XXX make this equality check against full package name
            if (!exists.contains(interfaceName)) {
                editor.insertText(", " + interfaceName, false);
            }
        } else {
            // otherwise we need to put the actual "extends" word
            // and the interface name
            editor.insertText(" extends " + interfaceName, false);
        }
    }

    @Override
    public void removeInterface(FlowEditor editor, String interfaceName, ClassInfo info)
    {
        Selection s1 = null;

        List<Selection> vsels;
        List<String> vtexts;

        vsels = info.getInterfaceSelections();
        vtexts = editor.getInterfaceTexts(vsels);
        int where = vtexts.indexOf(interfaceName);

        // we have a special case if we deleted the first bit of an
        // "implements" clause, yet there are still clauses left.. we have
        // to delete the following "," instead of the preceding one.
        if (where == 1 && vsels.size() > 2) {
            where = 2;
        }

        if (where > 0) { // should always be true
            s1 = vsels.get(where - 1);
            s1.combineWith(vsels.get(where));
        }

        // delete the text from the end backwards so that our
        if (s1 != null) {
            editor.setSelection(
                    new SourceLocation(s1.getLine(), s1.getColumn()),
                    new SourceLocation(s1.getEndLine(), s1.getEndColumn()));
            editor.insertText("", false);
        }
    }
}
