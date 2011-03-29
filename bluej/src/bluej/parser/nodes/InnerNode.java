/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010,2011  Michael Kolling and John Rosenberg 
 
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
package bluej.parser.nodes;

import bluej.debugger.gentype.Reflective;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.PackageOrClass;
import bluej.parser.entity.ParsedReflective;
import bluej.parser.entity.TypeEntity;
import bluej.parser.entity.ValueEntity;

/**
 * A generic "inner" representation (for eg. loop bodies)
 * 
 * @author Davin McCall
 */
public class InnerNode extends JavaParentNode
{
    public InnerNode(JavaParentNode parent)
    {
        super(parent);
    }
    
    @Override
    protected boolean marksOwnEnd()
    {
        return false;
    }
    
    @Override
    public JavaEntity getValueEntity(String name, Reflective querySource, int fromPosition)
    {
        FieldNode var = variables.get(name);
        if (var != null && var.getOffsetFromParent() <= fromPosition) {
            JavaEntity fieldType = var.getFieldType().resolveAsType();
            if (fieldType != null) {
                return new ValueEntity(fieldType.getType());
            }
        }
        
        JavaEntity rval = null;
        if (parentNode != null) {
            rval = parentNode.getValueEntity(name, querySource, getOffsetFromParent());
        }
        
        if (rval == null) {
            rval = resolvePackageOrClass(name, querySource, fromPosition);
        }
        
        return rval;
    }
    
    @Override
    public PackageOrClass resolvePackageOrClass(String name,
            Reflective querySource, int fromPosition)
    {
        ParsedNode cnode = classNodes.get(name);
        if (cnode != null && cnode.getOffsetFromParent() <= fromPosition) {
            return new TypeEntity(new ParsedReflective((ParsedTypeNode) cnode));
        }
        
        PackageOrClass rval = null;
        if (parentNode != null) {
            rval = parentNode.resolvePackageOrClass(name, querySource);
        }
        return rval;
    }
}
