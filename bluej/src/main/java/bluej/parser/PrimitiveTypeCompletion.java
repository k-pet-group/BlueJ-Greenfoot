/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2017,2019,2020 Michael Kölling and John Rosenberg
 
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
package bluej.parser;

import java.util.Arrays;
import java.util.List;

import bluej.pkgmgr.target.role.Kind;
import threadchecker.OnThread;
import threadchecker.Tag;

public class PrimitiveTypeCompletion extends AssistContent
{
    private final String type;
    private final String description;
    
    private PrimitiveTypeCompletion(String type, String description)
    {
        this.type = type;
        this.description = description;
    }

    @Override
    @OnThread(Tag.Any)
    public String getName()
    {
        return type;
    }

    @Override
    public List<ParamInfo> getParams()
    {
        return null;
    }

    @Override
    public String getType()
    {
        return null;
    }

    @Override
    public String getDeclaringClass()
    {
        return null;
    }

    @Override
    public CompletionKind getKind() {
        return CompletionKind.TYPE;
    }
    
    

    @Override
    public Kind getTypeKind()
    {
        return Kind.PRIMITIVE;
    }

    @Override
    public String getJavadoc()
    {
        return description;
    }
    
    public static PrimitiveTypeCompletion primByte = new PrimitiveTypeCompletion("byte",
            "The primitive byte type.  If you are unsure whether you want byte or Byte, then choose byte (this type).");
    public static PrimitiveTypeCompletion primShort = new PrimitiveTypeCompletion("short",
            "The primitive short integer type.  If you are unsure whether you want short or Short, then choose short (this type).");
    public static PrimitiveTypeCompletion primInt = new PrimitiveTypeCompletion("int",
            "The primitive integer (whole number) type.  If you are unsure whether you want int or Integer, then choose int (this type).");
    public static PrimitiveTypeCompletion primLong = new PrimitiveTypeCompletion("long",
            "The primitive long integer type.  If you are unsure whether you want long or Long, then choose long (this type).");
    public static PrimitiveTypeCompletion primFloat = new PrimitiveTypeCompletion("float",
            "The primitive single floating point type.  If you are unsure whether you want float or Float, then choose float (this type).");
    public static PrimitiveTypeCompletion primDouble = new PrimitiveTypeCompletion("double",
            "The primitive double floating point type.  If you are unsure whether you want double or Double, then choose double (this type).");
    public static PrimitiveTypeCompletion primBoolean = new PrimitiveTypeCompletion("boolean",
            "The primitive boolean (true or false) type.  If you are unsure whether you want boolean or Boolean, then choose boolean (this type).");
    public static PrimitiveTypeCompletion primChar = new PrimitiveTypeCompletion("char",
            "The primitive character type.  If you are unsure whether you want char or Character, then choose char (this type).");
    public static PrimitiveTypeCompletion primVoid = new PrimitiveTypeCompletion("void",
            "The empty type.  If you are unsure whether you want void or Void, then choose void (this type).");
    
    public static List<PrimitiveTypeCompletion> allPrimitiveTypes()
    {
        return Arrays.asList(primByte, primShort, primInt, primLong, primFloat, primDouble, primBoolean, primChar, primVoid);
    }

    @Override
    public Access getAccessPermission()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
