/*
 This file is part of the BlueJ program. 
 Copyright (C) 2011  Michael Kolling and John Rosenberg 
 
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

import bluej.debugger.gentype.Reflective;
import bluej.parser.nodes.JavaParentNode;

/**
 * An entity resolver which resolves from a JavaParentNode at a particular position.
 * Used to resolve correctly when the position is important, i.e. forward references are not allowed.
 * 
 * @author Davin McCall
 */
public class PositionedResolver implements EntityResolver
{
    private JavaParentNode parentNode;
    private int fromPosition;
    
    /**
     * Construct a new PositionedResolver, resolving against the given parent node at the given position.
     */
    public PositionedResolver(JavaParentNode parentNode, int fromPosition)
    {
        this.parentNode = parentNode;
        this.fromPosition = fromPosition;
    }
    
    @Override
    public PackageOrClass resolvePackageOrClass(String name,
            Reflective querySource)
    {
        return parentNode.resolvePackageOrClass(name, querySource, fromPosition);
    }

    @Override
    public TypeEntity resolveQualifiedClass(String name)
    {
        return parentNode.resolveQualifiedClass(name);
    }

    @Override
    public JavaEntity getValueEntity(String name, Reflective querySource)
    {
        return parentNode.getValueEntity(name, querySource, fromPosition);
    }
}
