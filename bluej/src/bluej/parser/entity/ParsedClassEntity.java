/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.parser.entity;

import bluej.debugger.gentype.GenTypeClass;
import bluej.parser.SemanticException;
import bluej.parser.nodes.ParsedTypeNode;

/**
 * A class entity representing a parsed (not necessarily compiled) class.
 * 
 * @author Davin McCall
 */
public class ParsedClassEntity extends ClassEntity
{
    private ParsedTypeNode pnode;
    
    public ParsedClassEntity(ParsedTypeNode pnode)
    {
        this.pnode = pnode;
    }
    
    @Override
    public GenTypeClass getClassType()
    {
        return new GenTypeClass(new ParsedReflective(pnode));
    }

    @Override
    public PackageOrClass getPackageOrClassMember(String name)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName()
    {
        return pnode.getName(); // Hmmm. should probably be fully qualified.
    }

    @Override
    public JavaEntity getSubentity(String name) throws SemanticException
    {
        // TODO Auto-generated method stub
        return null;
    }
}
