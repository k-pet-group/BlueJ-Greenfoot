/*
 This file is part of the BlueJ program. 
 Copyright (C) 2015,2016 Michael Kölling and John Rosenberg 
 
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

import java.util.stream.Stream;

import bluej.stride.framedjava.errors.EmptyError;
import bluej.stride.framedjava.errors.SyntaxCodeError;
import bluej.stride.framedjava.slots.ExpressionSlot;

/**
 * Created by neil on 04/12/2015.
 */
public class CallExpressionSlotFragment extends FilledExpressionSlotFragment
{
    public CallExpressionSlotFragment(String content, String javaCode)
    {
        super(content, javaCode);
    }

    public CallExpressionSlotFragment(String content, String javaCode, ExpressionSlot slot)
    {
        super(content, javaCode, slot);
    }

    @Override
    public Stream<SyntaxCodeError> findEarlyErrors()
    {
        // TODO Also check the call is actually a method call, and not an assignment or other expression
        Stream<SyntaxCodeError> superErrors = super.findEarlyErrors();
        // Look for a blank frame and give an error:
        if (content.equals("()"))
        {
            return Stream.concat(Stream.of(new EmptyError(this, "Method name cannot be blank")), superErrors);
        }
        return superErrors;
    }
}
