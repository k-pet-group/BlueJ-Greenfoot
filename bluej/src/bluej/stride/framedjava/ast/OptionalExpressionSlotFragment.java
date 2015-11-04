/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015 Michael Kölling and John Rosenberg 
 
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

import bluej.stride.framedjava.slots.ExpressionSlot;

/**
 * An ExpressionSlotFragment that is allowed to have empty content
 */
public class OptionalExpressionSlotFragment extends ExpressionSlotFragment
{

    public OptionalExpressionSlotFragment(String content, String javaCode,
            ExpressionSlot slot)
    {
        super(content, javaCode, slot);
    }

    public OptionalExpressionSlotFragment(String content, String javaCode)
    {
        super(content, javaCode);
    }

    public OptionalExpressionSlotFragment(ExpressionSlotFragment f)
    {
        super(f);
    }

    @Override
    protected boolean isRequired()
    {
        return false;
    }

}
