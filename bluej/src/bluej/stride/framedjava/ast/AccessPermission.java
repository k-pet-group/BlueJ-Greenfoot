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

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import bluej.parser.AssistContent;
import bluej.parser.AssistContent.Access;

public enum AccessPermission
{
    PRIVATE("private"), PROTECTED("protected"), PUBLIC("public"), EMPTY("");
    // EMPTY is not a valid state in the language, but it arises in the case that the slot is empty
    
    private String displayName;
    
    private AccessPermission(String displayName)
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
        // This is ok, even for EMPTY, because missing the permission does produce
        // parseable code:
        return displayName;
    }

    public Access asAccess()
    {
        switch (this)
        {
            case PRIVATE:
                return Access.PRIVATE;
            case PROTECTED:
                return Access.PROTECTED;
            case PUBLIC:
                return Access.PUBLIC;
            default:
                return null;
        }
    }
    
    public static List<AccessPermission> all()
    {
        return Arrays.asList(PRIVATE, PROTECTED, PUBLIC);
    }

    public static AccessPermission fromString(String s)
    {
        for (AccessPermission a : all()) {
            if (a.toString().equals(s)) {
                return a;
            }
        }
        return EMPTY;
    }

    
    public static boolean isValid(AccessPermission ap)
    {
        return ap == PRIVATE || ap == PROTECTED || ap == PUBLIC;
    }

    public static AccessPermission fromAccess(AssistContent.Access access)
    {
        switch (access)
        {
            case PRIVATE:
                return PRIVATE;
            case PROTECTED:
            case PACKAGE:
                return PROTECTED;
            case PUBLIC:
                return PUBLIC;
            default:
                return EMPTY;
        }
    }
}
