/*
 This file is part of the BlueJ program.
 Copyright (C) 2016 Michael Kölling and John Rosenberg

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
package bluej.stride.framedjava.ast;

import bluej.stride.framedjava.errors.SyntaxCodeError;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.stride.slots.EditableSlot;
import bluej.stride.slots.TextSlot;

import java.util.stream.Stream;

/**
 * The piece of code containing the declared package name for a particular type.
 */
public class PackageFragment extends TextSlotFragment
{
    private TextSlot<PackageFragment> slot;

    public PackageFragment(String content)
    {
        super(content);
    }

    public PackageFragment(String content, TextSlot<PackageFragment> textSlot)
    {
        super(content);
        this.slot = textSlot;
    }

    @Override
    public void registerSlot(TextSlot slot)
    {
        this.slot = slot;
    }

    @Override
    protected String getJavaCode(Destination dest, ExpressionSlot<?> completing, Parser.DummyNameGenerator dummyNameGenerator)
    {
        // Surrounding class adds the package part:
        return getContent();
    }

    @Override
    public EditableSlot getSlot()
    {
        return slot;
    }

    @Override
    public Stream<SyntaxCodeError> findEarlyErrors()
    {
        return Stream.empty();
    }
}
