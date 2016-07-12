/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016 Michael Kölling and John Rosenberg 
 
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
package bluej.extensions;

// Note: this is not part of the extensions API as such, but because they need to be able
// to see it, it lives in the extensions package.

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * The type of source that is available.
 */
@OnThread(Tag.Any)
public enum SourceType
{
    NONE, Java, Stride;

    public static SourceType getEnum(String s)
    {
        if (s == null || s.equals("null")) {
            return NONE;
        }
        String lowerCase = s.toLowerCase();
        if(lowerCase.equals("stride")){
            return Stride;
        }
        if(lowerCase.equals("java")){
            return Java;
        }
        throw new IllegalArgumentException("No Enum specified for this string");
    }
    
    public String getExtension()
    {
        switch (this)
        {
            case Java: return "java";
            case Stride: return "stride";
            default: return "";
        }
    }
}