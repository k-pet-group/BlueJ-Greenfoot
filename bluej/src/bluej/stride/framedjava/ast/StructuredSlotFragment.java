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

import java.util.Map;

import bluej.stride.framedjava.elements.CodeElement;

/**
 * Created by neil on 22/05/2016.
 */
public abstract class StructuredSlotFragment extends StringSlotFragment
{
    private String javaCode;

    public StructuredSlotFragment(String content, String javaCode)
    {
        super(content);
        // If we are using types from an old type text-slot, javaCode will be null.
        // In this case, we just use the content as the Java code:
        this.javaCode = javaCode == null ? content : javaCode;
    }

    // Used by XML serialisation:
    public String getJavaCode()
    {
        return javaCode;
    }

    public abstract Map<String, CodeElement> getVars();
}
