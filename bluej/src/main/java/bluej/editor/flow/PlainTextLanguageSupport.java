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

import bluej.extensions2.SourceType;
import bluej.parser.entity.EntityResolver;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.symtab.ClassInfo;

/**
 * No-op implementation of {@link FlowLanguageSupport} for non-code editors
 * (README files, plain text files, CSS files, etc.). All methods throw
 * {@link UnsupportedOperationException}.
 */
public class PlainTextLanguageSupport implements FlowLanguageSupport
{
    @Override
    public ParsedCUNode createRootNode(EntityResolver resolver)
    {
        throw new UnsupportedOperationException(
            "PlainTextLanguageSupport does not support parsing — "
            + "this editor should have sourceIsCode = false");
    }

    @Override
    public void setExtendsClass(FlowEditor editor, String className, ClassInfo info)
    {
        throw new UnsupportedOperationException(
            "PlainTextLanguageSupport does not support class declaration editing");
    }

    @Override
    public void removeExtendsClass(FlowEditor editor, ClassInfo info)
    {
        throw new UnsupportedOperationException(
            "PlainTextLanguageSupport does not support class declaration editing");
    }

    @Override
    public void addImplements(FlowEditor editor, String interfaceName, ClassInfo info)
    {
        throw new UnsupportedOperationException(
            "PlainTextLanguageSupport does not support class declaration editing");
    }

    @Override
    public void addExtendsInterface(FlowEditor editor, String interfaceName, ClassInfo info)
    {
        throw new UnsupportedOperationException(
            "PlainTextLanguageSupport does not support class declaration editing");
    }

    @Override
    public void removeInterface(FlowEditor editor, String interfaceName, ClassInfo info)
    {
        throw new UnsupportedOperationException(
            "PlainTextLanguageSupport does not support class declaration editing");
    }

    @Override
    public SourceType getSourceType()
    {
        return null;
    }
}
