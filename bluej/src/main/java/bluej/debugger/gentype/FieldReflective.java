/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010,2014  Michael Kolling and John Rosenberg 
 
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
package bluej.debugger.gentype;

/**
 * This class represents a field in a Java type represented
 * by a Reflective.
 * 
 * @author Davin McCall
 */
public class FieldReflective
{
    private String name;
    private JavaType type;
    private int modifiers;
    private Reflective declaringType;
    
    public FieldReflective(String name, JavaType type, int modifiers, Reflective declaringType)
    {
        this.name = name;
        this.type = type;
        this.modifiers = modifiers;
        this.declaringType = declaringType;
    }
    
    public String getName()
    {
        return name;
    }
    
    public JavaType getType()
    {
        return type;
    }
    
    public int getModifiers()
    {
        return modifiers;
    }

    public Reflective getDeclaringType()
    {
        return declaringType;
    }
    
    
}
