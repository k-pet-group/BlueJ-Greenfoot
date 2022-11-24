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

import java.util.Arrays;
import java.util.List;

public enum SuperThis
{
    SUPER("super"), THIS("this"), EMPTY("");
    // EMPTY is not a valid state in the language, but it arises in the case that the slot is empty
    
    private String displayName;
    
    private SuperThis(String displayName)
    {
        this.displayName = displayName;
    }
    
    @Override
    public String toString()
    {
        return displayName;
    }
    
    public String getJavaCode()
    {
        if (this == EMPTY)
            return "empty_super_this";
        else
            return displayName;
    }
    
    public static List<SuperThis> all()
    {
        return Arrays.asList(SUPER, THIS);
    }

    public static SuperThis fromString(String s)
    {
        for (SuperThis a : all())
        {
            if (a.toString().equals(s)) {
                return a;
            }
        }
        return null;
    }
    
    public static boolean isValid(SuperThis st)
    {
        return st == SUPER || st == THIS;
    }
}
